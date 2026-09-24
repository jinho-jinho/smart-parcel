package com.capstone.smart_parcel.parcel;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.io.*;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.*;
import static com.capstone.smart_parcel.parcel.ParcelAccess.*;
import static com.capstone.smart_parcel.parcel.ParcelDtos.*;

@Service
@RequiredArgsConstructor
public class DeviceEventIngestService {
    private final JdbcTemplate jdbc;
    private final ParcelAccess access;
    private final ObjectMapper mapper;
    private record ImageData(byte[] bytes,String type,String hash) {}

    @Transactional
    public EventAck ingest(DeviceIdentity device,EventInput in,MultipartFile file) {
        var image=readImage(file);
        String payload=json(in);
        String hash=sha(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        // Same global lock order for every writer. Locks survive until the DB commit.
        lock("event:"+in.eventId());
        var prior=jdbc.queryForList("SELECT * FROM parcel.device_events WHERE event_id=?",in.eventId());
        if (!prior.isEmpty()) {
            var p=prior.get(0);
            if (!Objects.equals(p.get("device_id"),device.id()) ||
                    number(p,"organization_id")!=device.organizationId() ||
                    !Objects.equals(p.get("payload_sha256"),hash) ||
                    !Objects.equals(p.get("image_sha256"),image==null?null:image.hash()))
                throw conflict("Event ID already exists with different content or owner");
            return new EventAck(in.eventId(),in.attemptId(),true);
        }
        if (in.eventType()!=EventType.DECISION) {
            require(in.ruleVersionId()==null && in.capturedAt()==null && in.decidedAt()==null &&
                    in.decisionStatus()==null && in.recognizedInputType()==null && in.recognizedValue()==null &&
                    in.ruleId()==null && in.chuteId()==null,"Decision fields are not accepted on other event types");
            if (in.eventType()==EventType.DEVICE_ERROR)
                require(in.observedChuteId()==null,"Observed chute belongs in a discharge event");
        }
        if (in.eventType()!=EventType.DEVICE_ERROR) require(in.attemptId()!=null,"attemptId is required");
        if (in.attemptId()!=null) lock("attempt:"+in.attemptId());
        if (in.eventType()==EventType.DECISION) {
            require(image!=null,"DECISION requires a PNG or JPEG image");
            createAttempt(device,in);
        } else if (in.eventType()==EventType.DEVICE_ERROR) {
            require(in.errorCode()!=null && !in.errorCode().isBlank(),"errorCode is required");
            if (in.attemptId()!=null) attempt(device,in.attemptId());
        } else {
            discharge(device,in);
        }
        String uri=image==null?null:"/api/v2/events/"+in.eventId()+"/image";
        jdbc.update("""
                INSERT INTO parcel.device_events(event_id,organization_id,belt_id,device_id,attempt_id,event_type,
                error_code,occurred_at,payload_sha256,image_sha256,image_uri,payload)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?::jsonb)
                """,in.eventId(),device.organizationId(),device.beltId(),device.id(),in.attemptId(),
                in.eventType().name(),in.errorCode(),in.occurredAt(),hash,image==null?null:image.hash(),uri,payload);
        if (image!=null) {
            jdbc.update("INSERT INTO parcel.event_images(event_id,content_type,content) VALUES (?,?,?)",
                    in.eventId(),image.type(),image.bytes());
            if(in.eventType()==EventType.DECISION)
                jdbc.update("UPDATE parcel.sorting_attempts SET image_uri=? WHERE attempt_id=?",uri,in.attemptId());
        }
        if (in.eventType()==EventType.DEVICE_ERROR || in.eventType()==EventType.DISCHARGE_FAILED ||
                (in.eventType()==EventType.DECISION && in.decisionStatus()==Decision.ERROR)) {
            jdbc.update("""
                    INSERT INTO parcel.user_notifications(organization_id,event_id,recipient_user_id)
                    SELECT organization_id,?,id FROM public.users WHERE organization_id=?
                    ON CONFLICT (event_id,recipient_user_id) DO NOTHING
                    """,in.eventId(),device.organizationId());
        }
        jdbc.update("UPDATE parcel.devices SET last_seen_at=now() WHERE id=?",device.id());
        return new EventAck(in.eventId(),in.attemptId(),false);
    }
    private void createAttempt(DeviceIdentity d,EventInput in) {
        require(in.ruleVersionId()!=null && in.capturedAt()!=null && in.decidedAt()!=null && in.decisionStatus()!=null,
                "DECISION requires ruleVersionId, capturedAt, decidedAt and decisionStatus");
        require(!in.decidedAt().isBefore(in.capturedAt()),"decidedAt precedes capturedAt");
        if (!jdbc.queryForList("SELECT attempt_id FROM parcel.sorting_attempts WHERE attempt_id=?",in.attemptId()).isEmpty())
            throw conflict("Attempt already has a decision; retransmit its original eventId");
        var version=access.one("""
                SELECT * FROM parcel.rule_versions WHERE organization_id=? AND belt_id=? AND id=? AND status='PUBLISHED'
                """,d.organizationId(),d.beltId(),in.ruleVersionId());
        require((in.recognizedInputType()==null)==(in.recognizedValue()==null),"Input type and value must be provided together");
        String input=in.recognizedValue()==null?null:BeltConfigurationService.normalize(in.recognizedValue());
        String item=null, chuteName=null;
        if (in.decisionStatus()==Decision.MATCHED) {
            require(in.ruleId()!=null && in.chuteId()!=null && in.recognizedInputType()!=null && in.errorCode()==null,
                    "MATCHED requires a rule, chute, recognized input and no error");
            var rule=access.one("""
                    SELECT r.*,c.chute_name_snapshot FROM parcel.sorting_rules r
                    JOIN parcel.version_chutes c ON c.version_id=r.version_id AND c.chute_id=r.chute_id
                    WHERE r.organization_id=? AND r.belt_id=? AND r.version_id=? AND r.id=? AND r.chute_id=?
                    """,d.organizationId(),d.beltId(),in.ruleVersionId(),in.ruleId(),in.chuteId());
            require(Objects.equals(rule.get("input_type"),in.recognizedInputType().name()) &&
                    Objects.equals(rule.get("input_value"),input),"Recognized input does not match the selected rule");
            item=(String)rule.get("item_name"); chuteName=(String)rule.get("chute_name_snapshot");
        } else {
            require(in.ruleId()==null && in.chuteId()==null,"Unmatched/error decisions cannot have a destination");
            require(in.decisionStatus()==Decision.ERROR
                    ? in.errorCode()!=null && !in.errorCode().isBlank() : in.errorCode()==null,"Invalid decision errorCode");
        }
        require(in.observedChuteId()==null,"Observed chute belongs in a discharge event");
        var p=new MapSqlParameterSource()
                .addValue("attempt",in.attemptId()).addValue("org",d.organizationId()).addValue("belt",d.beltId())
                .addValue("device",d.id()).addValue("version",in.ruleVersionId()).addValue("captured",in.capturedAt())
                .addValue("decided",in.decidedAt()).addValue("status",in.decisionStatus().name()).addValue("error",in.errorCode())
                .addValue("type",in.recognizedInputType()==null?null:in.recognizedInputType().name()).addValue("value",input)
                .addValue("rule",in.ruleId()).addValue("chute",in.chuteId()).addValue("groupName",version.get("group_name_snapshot"))
                .addValue("item",item).addValue("chuteName",chuteName);
        new NamedParameterJdbcTemplate(jdbc).update("""
                INSERT INTO parcel.sorting_attempts(attempt_id,organization_id,belt_id,device_id,rule_version_id,
                captured_at,decided_at,decision_status,decision_error_code,recognized_input_type,recognized_value,
                rule_id,chute_id,group_name_snapshot,item_name_snapshot,chute_name_snapshot)
                VALUES (:attempt,:org,:belt,:device,:version,:captured,:decided,:status,:error,:type,:value,
                :rule,:chute,:groupName,:item,:chuteName)
                """,p);
    }
    private Map<String,Object> attempt(DeviceIdentity d,UUID attempt) {
        return access.one("""
                SELECT * FROM parcel.sorting_attempts WHERE organization_id=? AND belt_id=? AND device_id=? AND attempt_id=? FOR UPDATE
                """,d.organizationId(),d.beltId(),d.id(),attempt);
    }
    private void discharge(DeviceIdentity d,EventInput in) {
        var row=attempt(d,in.attemptId());
        require("MATCHED".equals(row.get("decision_status")),"Only matched attempts accept discharge events");
        boolean failed=in.eventType()==EventType.DISCHARGE_FAILED;
        require(failed ? in.errorCode()!=null && !in.errorCode().isBlank() : in.errorCode()==null,"Invalid discharge errorCode");
        if(in.observedChuteId()!=null) {
            access.one("SELECT id FROM parcel.chutes WHERE organization_id=? AND belt_id=? AND id=?",
                    d.organizationId(),d.beltId(),in.observedChuteId());
            require(failed || number(row,"chute_id")==in.observedChuteId(),"Confirmed destination differs from target");
        }
        String previous=(String)row.get("discharge_status");
        String incoming=failed?"FAILED":"CONFIRMED";
        boolean conflict=(Boolean)row.get("discharge_conflict") ||
                (!"UNCONFIRMED".equals(previous) && !incoming.equals(previous));
        String status=failed || "FAILED".equals(previous) ? "FAILED" : "CONFIRMED";
        OffsetDateTime at=in.occurredAt();
        Long observed=in.observedChuteId();
        // Earliest event of the winning status determines the time. Ties use the lowest observed chute.
        if (!"UNCONFIRMED".equals(previous) && (previous.equals(incoming) || ("FAILED".equals(previous) && !failed))) {
            var old=((java.sql.Timestamp)row.get("discharge_at")).toInstant();
            if (old.isBefore(at.toInstant()) || ("FAILED".equals(previous) && !failed)) {
                at=old.atOffset(java.time.ZoneOffset.UTC);
                observed=row.get("observed_chute_id")==null?null:number(row,"observed_chute_id");
            } else if (old.equals(at.toInstant()) && row.get("observed_chute_id")!=null) {
                long oldChute=number(row,"observed_chute_id");
                observed=observed==null?oldChute:Math.min(observed,oldChute);
            }
        }
        jdbc.update("""
                UPDATE parcel.sorting_attempts SET discharge_status=?,discharge_conflict=?,discharge_at=?,observed_chute_id=?
                WHERE attempt_id=?
                """,status,conflict,at,observed,in.attemptId());
    }
    private void lock(String key) {
        jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",key);
    }
    private String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (Exception e) { throw new IllegalArgumentException("Invalid event payload",e); }
    }
    private static String sha(byte[] data) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private ImageData readImage(MultipartFile file) {
        if(file==null || file.isEmpty()) return null;
        require(file.getSize()<=5*1024*1024,"Image limit is 5 MiB");
        try {
            byte[] bytes=file.getBytes(); String type;
            try(var stream=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers=ImageIO.getImageReaders(stream);
                require(readers.hasNext(),"Invalid image");
                var reader=readers.next();
                try {
                    reader.setInput(stream);
                    String format=reader.getFormatName();
                    require(format.equalsIgnoreCase("png") || format.equalsIgnoreCase("jpeg"),"Only PNG/JPEG supported");
                    require((long)reader.getWidth(0)*reader.getHeight(0)<=16_000_000,"Image dimensions too large");
                    require(reader.read(0)!=null,"Invalid image");
                    type=format.equalsIgnoreCase("png")?"image/png":"image/jpeg";
                } finally { reader.dispose(); }
            }
            return new ImageData(bytes,type,sha(bytes));
        } catch(IOException e) { throw new IllegalArgumentException("Cannot decode image",e); }
    }
}
