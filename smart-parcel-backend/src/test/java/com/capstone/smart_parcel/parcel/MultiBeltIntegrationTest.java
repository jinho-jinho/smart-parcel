package com.capstone.smart_parcel.parcel;

import com.capstone.smart_parcel.PostgresIntegrationBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import static com.capstone.smart_parcel.parcel.ParcelDtos.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MultiBeltIntegrationTest extends PostgresIntegrationBase {
    @Autowired JdbcTemplate jdbc;
    @Autowired BeltConfigurationService configs;
    @Autowired DeviceEventIngestService events;
    @Autowired DeviceAuthentication authentication;
    @Autowired ObjectMapper mapper;
    @Autowired com.capstone.smart_parcel.config.jwt.JwtTokenProvider jwt;
    @Autowired MockMvc mvc;
    @Autowired PlatformTransactionManager transactionManager;
    @LocalServerPort int port;
    @TempDir Path temporary;
    record Fixture(String email,long org,long belt,long chute,long version,long rule,DeviceCredential credential) {
        DeviceIdentity identity() { return new DeviceIdentity(credential.deviceId(),org,belt,true); }
    }

    Fixture fixture() {
        String suffix=UUID.randomUUID().toString();
        long org=jdbc.queryForObject("INSERT INTO parcel.organizations(code,name) VALUES (?,?) RETURNING id",Long.class,suffix,"Test organization");
        String email=suffix+"@example.test";
        jdbc.update("INSERT INTO public.users(email,name,password,role,organization_id) VALUES (?,?,?,'MANAGER',?)",email,"Manager","unused",org);
        long belt=configs.createBelt(email,new BeltInput("A","Belt A"));
        long chute=configs.createChute(email,belt,new ChuteInput("C1","Destination"));
        long version=configs.createVersion(email,belt,input(chute,"Item"));
        var config=configs.publish(email,belt,version);
        configs.activate(email,belt,version,null);
        long rule=((Number)((Map<?,?>)((List<?>)config.get("rules")).get(0)).get("id")).longValue();
        var credential=configs.registerDevice(email,belt,new DeviceInput("device"));
        return new Fixture(email,org,belt,chute,version,rule,credential);
    }
    VersionInput input(long chute,String item) {
        return new VersionInput("Group",List.of(new ChuteSetting(chute,45)),
                List.of(new RuleInput("Label",1,InputType.TEXT,"K1S",item,chute)));
    }
    MockMultipartFile image() throws Exception {
        var out=new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",out);
        return new MockMultipartFile("image","test.png","image/png",out.toByteArray());
    }
    EventInput decision(Fixture f) {
        var at=OffsetDateTime.parse("2026-09-24T10:00:00Z");
        return new EventInput(UUID.randomUUID(),UUID.randomUUID(),EventType.DECISION,at,f.version,at,at,
                Decision.MATCHED,InputType.TEXT,"K1S",f.rule,f.chute,null,null);
    }
    EventInput discharge(EventInput decision,boolean failed,int seconds) {
        return new EventInput(UUID.randomUUID(),decision.attemptId(),failed?EventType.DISCHARGE_FAILED:EventType.DISCHARGE_CONFIRMED,
                decision.occurredAt().plusSeconds(seconds),null,null,null,null,null,null,null,null,null,failed?"SERVO_ERROR":null);
    }
    @Test void beltAChangesDoNotAffectBAndOldVersionStillExplainsResults() throws Exception {
        var a=fixture();
        long b=configs.createBelt(a.email,new BeltInput("B","Belt B"));
        long chuteB=configs.createChute(a.email,b,new ChuteInput("C1","B destination"));
        long vB=configs.createVersion(a.email,b,input(chuteB,"B item")); configs.publish(a.email,b,vB); configs.activate(a.email,b,vB,null);
        long next=configs.createVersion(a.email,a.belt,input(a.chute,"Changed item")); configs.publish(a.email,a.belt,next);
        configs.activate(a.email,a.belt,next,a.version);
        assertEquals(vB,jdbc.queryForObject("SELECT desired_rule_version_id FROM parcel.conveyor_belts WHERE id=?",Long.class,b));
        var old=decision(a); events.ingest(a.identity(),old,image());
        assertEquals("Item",jdbc.queryForObject("SELECT item_name_snapshot FROM parcel.sorting_attempts WHERE attempt_id=?",String.class,old.attemptId()));
        assertThrows(Exception.class,()->configs.activate(a.email,a.belt,a.version,a.version));
    }
    @Test void authenticatedApisRejectOtherOrganizationsAndLegacyManagerId() throws Exception {
        var a=fixture(); var b=fixture();
        mvc.perform(get("/api/v2/device/setup")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v2/device/setup").header("X-Device-Id",a.credential.deviceId()).header("X-Device-Key","wrong"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v2/device/setup").header("X-Device-Id",a.credential.deviceId()).header("X-Device-Key",a.credential.deviceKey()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(a.version));
        mvc.perform(get("/api/devices/setup").param("managerId",Long.toString(a.org))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v2/belts/"+b.belt+"/devices").with(user(a.email)).contentType("application/json")
                .content("{\"code\":\"intruder\"}")).andExpect(status().isNotFound());
        mvc.perform(post("/api/v2/belts/"+a.belt+"/versions").with(user(a.email)).contentType("application/json")
                .content(mapper.writeValueAsBytes(input(b.chute,"Wrong")))).andExpect(status().isNotFound());
        var foreign=decision(b);
        mvc.perform(multipart("/api/v2/device/events").file(image()).file(new MockMultipartFile("payload","","application/json",mapper.writeValueAsBytes(foreign)))
                .header("X-Device-Id",a.credential.deviceId()).header("X-Device-Key",a.credential.deviceKey()))
                .andExpect(status().isNotFound());
        assertEquals(0L,jdbc.queryForObject("SELECT count(*) FROM parcel.device_events WHERE event_id=?",Long.class,foreign.eventId()));
    }
    @Test void signupCreatesOwnOrganizationButCannotSelfEnrollInAnother() throws Exception {
        var owner=fixture(); String email=UUID.randomUUID()+"@example.test";
        mvc.perform(post("/api/auth/signup").contentType("application/json").content(mapper.writeValueAsBytes(Map.of(
                "email",email,"name","New manager","password","test-password-123","role","MANAGER"))))
                .andExpect(status().isOk());
        long organization=jdbc.queryForObject("SELECT organization_id FROM public.users WHERE email=?",Long.class,email);
        assertNotEquals(owner.org,organization);
        String staff=UUID.randomUUID()+"@example.test";
        mvc.perform(post("/api/auth/signup").contentType("application/json").content(mapper.writeValueAsBytes(Map.of(
                "email",staff,"name","Uninvited staff","password","test-password-123","role","STAFF","managerEmail",owner.email))))
                .andExpect(status().isBadRequest());
        assertEquals(0L,jdbc.queryForObject("SELECT count(*) FROM public.users WHERE email=?",Long.class,staff));
    }
    @Test void errorRetriesNotifyOnceAndRejectUnrelatedChuteFields() throws Exception {
        var f=fixture(); var foreign=fixture();
        var error=new EventInput(UUID.randomUUID(),null,EventType.DEVICE_ERROR,OffsetDateTime.now(),
                null,null,null,null,null,null,null,null,null,"CAMERA_ERROR");
        events.ingest(f.identity(),error,null); assertTrue(events.ingest(f.identity(),error,null).duplicate());
        assertEquals(1L,jdbc.queryForObject("SELECT count(*) FROM parcel.user_notifications WHERE event_id=?",Long.class,error.eventId()));
        var invalid=new EventInput(UUID.randomUUID(),null,EventType.DEVICE_ERROR,error.occurredAt(),
                null,null,null,null,null,null,null,foreign.chute,null,"CAMERA_ERROR");
        mvc.perform(multipart("/api/v2/device/events").file(new MockMultipartFile("payload","","application/json",mapper.writeValueAsBytes(invalid)))
                .header("X-Device-Id",f.credential.deviceId()).header("X-Device-Key",f.credential.deviceKey()))
                .andExpect(status().isBadRequest());
    }
    @Test void constraintsRejectWrongOwnerDestinationDraftAndMutation() throws Exception {
        var a=fixture(); var b=fixture(); var decision=decision(a); events.ingest(a.identity(),decision,image());
        assertThrows(DataAccessException.class,()->jdbc.update("UPDATE parcel.sorting_rules SET item_name='tamper' WHERE id=?",a.rule));
        assertThrows(DataAccessException.class,()->jdbc.update("DELETE FROM parcel.version_chutes WHERE version_id=?",a.version));
        assertThrows(DataAccessException.class,()->jdbc.update("UPDATE parcel.rule_versions SET status='DRAFT',published_at=NULL WHERE id=?",a.version));
        assertThrows(DataAccessException.class,()->jdbc.update("UPDATE parcel.sorting_attempts SET chute_id=? WHERE attempt_id=?",b.chute,decision.attemptId()));
        assertThrows(DataAccessException.class,()->jdbc.update("""
                INSERT INTO parcel.device_events(event_id,organization_id,belt_id,device_id,attempt_id,event_type,error_code,occurred_at,payload_sha256,payload)
                VALUES (?,?,?,?,?,'DEVICE_ERROR','X',now(),?,'{}')
                """,UUID.randomUUID(),a.org,a.belt,b.credential.deviceId(),decision.attemptId(),"a".repeat(64)));
        long draft=configs.createVersion(a.email,a.belt,input(a.chute,"Draft"));
        assertThrows(DataAccessException.class,()->jdbc.update("UPDATE parcel.conveyor_belts SET desired_rule_version_id=?,desired_set_at=now() WHERE id=?",draft,a.belt));
    }
    @Test void concurrentRetriesCreateOneAttemptImageAndEvent() throws Exception {
        var f=fixture(); var in=decision(f); var img=image();
        var executor=Executors.newFixedThreadPool(8);
        try {
            var tasks=new ArrayList<Callable<EventAck>>();
            for(int i=0;i<24;i++) tasks.add(()->events.ingest(f.identity(),in,img));
            int original=0;
            for(var result:executor.invokeAll(tasks)) if(!result.get(30,TimeUnit.SECONDS).duplicate()) original++;
            assertEquals(1,original);
        } finally { executor.shutdownNow(); }
        assertEquals(1L,jdbc.queryForObject("SELECT count(*) FROM parcel.sorting_attempts WHERE attempt_id=?",Long.class,in.attemptId()));
        assertEquals(1L,jdbc.queryForObject("SELECT count(*) FROM parcel.event_images WHERE event_id=?",Long.class,in.eventId()));
        var changed=new EventInput(in.eventId(),in.attemptId(),in.eventType(),in.occurredAt(),in.ruleVersionId(),in.capturedAt(),in.decidedAt(),
                in.decisionStatus(),in.recognizedInputType(),"OTHER",in.ruleId(),in.chuteId(),null,null);
        assertThrows(Exception.class,()->events.ingest(f.identity(),changed,img));
        mvc.perform(get("/api/v2/events/"+in.eventId()+"/image").with(user(f.email))).andExpect(status().isOk())
                .andExpect(content().bytes(img.getBytes()));
        var other=fixture();
        mvc.perform(get("/api/v2/events/"+in.eventId()+"/image").with(user(other.email))).andExpect(status().isNotFound());
    }
    @Test void failedWinsInEitherOrderAndDecisionRetryDoesNotResetDischarge() throws Exception {
        var f=fixture();
        for(boolean reverse:new boolean[]{false,true}) {
            var in=decision(f); var img=image(); events.ingest(f.identity(),in,img);
            var yes=discharge(in,false,2); var no=discharge(in,true,3);
            events.ingest(f.identity(),reverse?no:yes,null);
            events.ingest(f.identity(),reverse?yes:no,null);
            events.ingest(f.identity(),in,img);
            var row=jdbc.queryForMap("SELECT * FROM parcel.sorting_attempts WHERE attempt_id=?",in.attemptId());
            assertEquals("FAILED",row.get("discharge_status")); assertEquals(true,row.get("discharge_conflict"));
            assertEquals(no.occurredAt().toInstant(),((java.sql.Timestamp)row.get("discharge_at")).toInstant());
            assertEquals(1L,jdbc.queryForObject("SELECT count(*) FROM parcel.user_notifications WHERE event_id=?",Long.class,no.eventId()));
            assertThrows(DataAccessException.class,()->jdbc.update("UPDATE parcel.sorting_attempts SET discharge_status='CONFIRMED' WHERE attempt_id=?",in.attemptId()));
        }
    }
    @Test void serverRollbackLeavesNoAttemptEventOrImageAndStaffCannotWrite() throws Exception {
        var f=fixture(); var in=decision(f); var img=image();
        var tx=new TransactionTemplate(transactionManager);
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(status->{
            events.ingest(f.identity(),in,img); throw new IllegalStateException("Injected failure before commit");
        }));
        assertEquals(0L,jdbc.queryForObject("SELECT count(*) FROM parcel.sorting_attempts WHERE attempt_id=?",Long.class,in.attemptId()));
        assertEquals(0L,jdbc.queryForObject("SELECT count(*) FROM parcel.device_events WHERE event_id=?",Long.class,in.eventId()));
        String staff="staff-"+UUID.randomUUID()+"@example.test";
        configs.createStaff(f.email,new StaffInput(staff,"Staff","password-for-test"));
        mvc.perform(post("/api/v2/belts").with(user(staff)).contentType("application/json").content("{\"code\":\"X\",\"name\":\"X\"}"))
                .andExpect(status().isForbidden());
    }
    @Test void publicationLockBlocksConcurrentChildEdit() throws Exception {
        var f=fixture(); long draft=configs.createVersion(f.email,f.belt,input(f.chute,"Draft"));
        var held=new CountDownLatch(1); var release=new CountDownLatch(1); var editing=new CountDownLatch(1);
        var executor=Executors.newFixedThreadPool(2);
        try {
            var publish=executor.submit(()->new TransactionTemplate(transactionManager).executeWithoutResult(status->{
                jdbc.queryForList("SELECT id FROM parcel.rule_versions WHERE id=? FOR UPDATE",draft); held.countDown();
                try { if(!release.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("Timeout"); }
                catch(InterruptedException e) { throw new RuntimeException(e); }
                jdbc.update("UPDATE parcel.rule_versions SET status='PUBLISHED' WHERE id=?",draft);
            }));
            assertTrue(held.await(10,TimeUnit.SECONDS));
            var edit=executor.submit(()->{ editing.countDown(); return jdbc.update("UPDATE parcel.sorting_rules SET item_name='late' WHERE version_id=?",draft); });
            assertTrue(editing.await(10,TimeUnit.SECONDS));
            assertThrows(TimeoutException.class,()->edit.get(200,TimeUnit.MILLISECONDS));
            release.countDown(); publish.get(10,TimeUnit.SECONDS);
            assertThrows(ExecutionException.class,()->edit.get(10,TimeUnit.SECONDS));
            assertEquals("Draft",jdbc.queryForObject("SELECT item_name FROM parcel.sorting_rules WHERE version_id=?",String.class,draft));
        } finally { release.countDown(); executor.shutdownNow(); }
    }
    @Test void pythonSimulatorUploadsRealImageAndErrorThenReplaysSameIds() throws Exception {
        var owner=fixture();
        Path provisionScript=Path.of("..","tools","provision_demo.py").toAbsolutePath().normalize();
        Path credentials=temporary.resolve("demo.json");
        var provision=new ProcessBuilder(System.getenv().getOrDefault("PYTHON","python"),provisionScript.toString(),
                "--base-url","http://127.0.0.1:"+port,"--output",credentials.toString());
        long userId=jdbc.queryForObject("SELECT id FROM public.users WHERE email=?",Long.class,owner.email);
        provision.environment().put("PARCEL_ADMIN_TOKEN",jwt.createAccessToken(owner.email,userId));
        Path provisionLog=temporary.resolve("provision.log"); provision.redirectErrorStream(true).redirectOutput(provisionLog.toFile());
        var provisionProcess=provision.start(); assertTrue(provisionProcess.waitFor(30,TimeUnit.SECONDS));
        assertEquals(0,provisionProcess.exitValue(),Files.readString(provisionLog));
        var json=mapper.readTree(Files.readString(credentials)); assertEquals(2,json.size());
        var first=json.get(0);
        var credential=new DeviceCredential(UUID.fromString(first.get("deviceId").asText()),first.get("beltId").asLong(),first.get("deviceKey").asText());
        var f=new Fixture(owner.email,owner.org,credential.beltId(),0,0,0,credential);
        Path script=Path.of("..","tools","device_simulator.py").toAbsolutePath().normalize();
        Path spool=temporary.resolve("device.sqlite3");
        var process=new ProcessBuilder(System.getenv().getOrDefault("PYTHON","python"),script.toString(),
                "--base-url","http://127.0.0.1:"+port,"--spool",spool.toString(),"demo");
        process.environment().put("PARCEL_DEVICE_ID",f.credential.deviceId().toString());
        process.environment().put("PARCEL_DEVICE_KEY",f.credential.deviceKey());
        process.redirectErrorStream(true);
        Path output=temporary.resolve("simulator.log"); process.redirectOutput(output.toFile());
        var child=process.start(); assertTrue(child.waitFor(40,TimeUnit.SECONDS));
        assertEquals(0,child.exitValue(),Files.readString(output));
        assertEquals(2L,jdbc.queryForObject("SELECT count(*) FROM parcel.device_events WHERE device_id=?",Long.class,f.credential.deviceId()));
        assertEquals(1L,jdbc.queryForObject("SELECT count(*) FROM parcel.event_images i JOIN parcel.device_events e USING(event_id) WHERE e.device_id=?",Long.class,f.credential.deviceId()));
        UUID event=jdbc.queryForObject("SELECT event_id FROM parcel.device_events WHERE device_id=? AND event_type='DECISION'",UUID.class,f.credential.deviceId());
        var command=new ArrayList<>(process.command()); command.set(command.size()-1,"requeue"); command.add(event.toString()); process.command(command);
        child=process.start(); assertTrue(child.waitFor(10,TimeUnit.SECONDS)); assertEquals(0,child.exitValue());
        command.remove(command.size()-1); command.set(command.size()-1,"flush"); process.command(command);
        child=process.start(); assertTrue(child.waitFor(20,TimeUnit.SECONDS)); assertEquals(0,child.exitValue(),Files.readString(output));
        assertEquals(2L,jdbc.queryForObject("SELECT count(*) FROM parcel.device_events WHERE device_id=?",Long.class,f.credential.deviceId()));
    }
}
