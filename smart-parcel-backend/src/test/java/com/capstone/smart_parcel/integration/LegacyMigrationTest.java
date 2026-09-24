package com.capstone.smart_parcel.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class LegacyMigrationTest {
    @Test void adoptsLegacySchemaWithoutInventingHistoricalDevicesOrDiscardingAmbiguities() throws Exception {
        String root=System.getenv("TEST_DB_URL");
        assertNotNull(root,"TEST_DB_URL must reference a disposable PostgreSQL database");
        String user=System.getenv().getOrDefault("TEST_DB_USER","postgres");
        String password=System.getenv().getOrDefault("TEST_DB_PASSWORD","");
        String database="parcel_migration_"+UUID.randomUUID().toString().replace("-","")+"_test";
        String url=root.substring(0,root.lastIndexOf('/')+1)+database;
        try(var connection=DriverManager.getConnection(root,user,password);var statement=connection.createStatement()) {
            statement.execute("CREATE DATABASE "+database);
        }
        try {
            Flyway.configure().dataSource(url,user,password).target("1").load().migrate();
            try(var connection=DriverManager.getConnection(url,user,password);var s=connection.createStatement()) {
                s.execute("""
                    INSERT INTO users(id,email,name,password,role) VALUES (10,'manager@example.test','Manager','hash','MANAGER');
                    INSERT INTO users(id,email,name,password,role,manager_id) VALUES (11,'staff@example.test','Staff','hash','STAFF',10);
                    INSERT INTO sorting_groups(id,group_name,manager_id,enabled) VALUES (20,'Existing group',10,true);
                    INSERT INTO chutes(id,chute_name,servo_deg) VALUES (30,'A',45),(31,'B',90);
                    INSERT INTO sorting_rules(id,rule_name,input_type,input_value,group_id,item_name)
                    VALUES (40,'Valid','TEXT','K1S',20,'First'),(41,'Ambiguous','COLOR','RED',20,'Second');
                    INSERT INTO rule_chutes(rule_id,chute_id) VALUES (40,30),(41,30),(41,31);
                    INSERT INTO sorting_history(image_url,sorting_group_name_snapshot,chute_name_snapshot,manager_id,group_id,chute_id)
                    VALUES ('original.png','Original snapshot','A',10,20,30);
                    DROP TABLE flyway_schema_history;
                    """);
            }
            var flyway=Flyway.configure().dataSource(url,user,password).baselineOnMigrate(true).baselineVersion("1").load();
            assertEquals(3,flyway.migrate().migrationsExecuted);
            try(var c=DriverManager.getConnection(url,user,password);var s=c.createStatement()) {
                assertEquals(1,scalar(s,"SELECT count(DISTINCT organization_id) FROM public.users"));
                assertEquals(1,scalar(s,"SELECT count(*) FROM legacy.sorting_history WHERE image_url='original.png'"));
                assertEquals(0,scalar(s,"SELECT count(*) FROM public.sorting_attempts"));
                assertEquals(0,scalar(s,"SELECT count(*) FROM public.devices"));
                assertEquals(1,scalar(s,"SELECT count(*) FROM public.migration_issues WHERE legacy_rule_id=41"));
                assertEquals(1,scalar(s,"SELECT count(*) FROM public.sorting_rules"));
                assertEquals(1,scalar(s,"SELECT count(*) FROM public.rule_versions WHERE status='DRAFT'"));
                assertEquals(0,scalar(s,"SELECT count(*) FROM public.conveyor_belts WHERE desired_rule_version_id IS NOT NULL"));
            }
            assertEquals(0,flyway.migrate().migrationsExecuted);
        } finally {
            // This database name is generated above and never comes from a user's database name.
            try(var connection=DriverManager.getConnection(root,user,password);var statement=connection.createStatement()) {
                statement.execute("DROP DATABASE "+database);
            }
        }
    }

    @Test void upgradesAnAlreadyPopulatedV3WithoutDiscardingEventsOrImages() throws Exception {
        String root = System.getenv("TEST_DB_URL");
        assertNotNull(root);
        String user = System.getenv().getOrDefault("TEST_DB_USER", "postgres");
        String password = System.getenv().getOrDefault("TEST_DB_PASSWORD", "");
        String database = "parcel_upgrade_" + UUID.randomUUID().toString().replace("-", "") + "_test";
        String url = root.substring(0, root.lastIndexOf('/') + 1) + database;
        try (var c = DriverManager.getConnection(root, user, password); var st = c.createStatement()) {
            st.execute("CREATE DATABASE " + database);
        }
        try {
            Flyway.configure().dataSource(url, user, password).target("3").load().migrate();
            try (var c = DriverManager.getConnection(url, user, password); var st = c.createStatement()) {
                st.execute("""
                    INSERT INTO parcel.organizations(id,code,name) VALUES (1,'OLD','Existing');
                    INSERT INTO public.users(email,name,password,role,organization_id) VALUES ('existing@example.test','Existing','hash','MANAGER',1);
                    INSERT INTO parcel.conveyor_belts(id,organization_id,code,name) VALUES (1,1,'A','A');
                    INSERT INTO parcel.devices(id,organization_id,belt_id,device_code,credential_hash)
                    VALUES ('00000000-0000-0000-0000-000000000001',1,1,'device','original-hash');
                    INSERT INTO parcel.device_events(event_id,organization_id,belt_id,device_id,event_type,error_code,occurred_at,payload_sha256,payload,image_uri)
                    VALUES ('00000000-0000-0000-0000-000000000002',1,1,'00000000-0000-0000-0000-000000000001',
                    'DEVICE_ERROR','CAMERA',now(),repeat('a',64),'{}','/api/v2/events/00000000-0000-0000-0000-000000000002/image');
                    INSERT INTO parcel.event_images(event_id,content_type,content)
                    VALUES ('00000000-0000-0000-0000-000000000002','image/png',decode('010203','hex'));
                    """);
            }
            var flyway = Flyway.configure().dataSource(url, user, password).load();
            assertEquals(1, flyway.migrate().migrationsExecuted);
            flyway.validate();
            try (var c = DriverManager.getConnection(url, user, password); var st = c.createStatement()) {
                assertEquals(0, scalar(st, "SELECT count(*) FROM pg_namespace WHERE nspname='parcel'"));
                assertEquals(1, scalar(st, "SELECT count(*) FROM public.devices WHERE credential_hash='original-hash'"));
                assertEquals(1, scalar(st, "SELECT count(*) FROM public.event_images WHERE content=decode('010203','hex')"));
                assertEquals(1, scalar(st, "SELECT count(*) FROM public.device_events WHERE image_uri='/api/events/00000000-0000-0000-0000-000000000002/image'"));
                assertEquals(1, scalar(st, "SELECT count(*) FROM pg_tables WHERE schemaname='legacy' AND tablename='sorting_history'"));
            }
            assertEquals(0, flyway.migrate().migrationsExecuted);
        } finally {
            try (var c = DriverManager.getConnection(root, user, password); var st = c.createStatement()) {
                st.execute("DROP DATABASE " + database);
            }
        }
    }

    long scalar(Statement s,String sql) throws SQLException { try(var r=s.executeQuery(sql)) { r.next(); return r.getLong(1); } }
}
