package com.capstone.smart_parcel.parcel;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.*;
import static com.capstone.smart_parcel.parcel.ParcelAccess.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2")
public class ParcelHistoryController {
    private final ParcelAccess access;
    private final JdbcTemplate jdbc;
    @GetMapping("/attempts")
    public Object history(Authentication auth,@RequestParam long beltId,@RequestParam OffsetDateTime from,
                          @RequestParam OffsetDateTime to,@RequestParam(defaultValue="50") int size,
                          @RequestParam(defaultValue="0") int page) {
        long org=access.actor(auth.getName(),false).organizationId(); access.belt(org,beltId);
        require(from.isBefore(to),"from must precede to");
        require(size>=1 && size<=100 && page>=0 && page<=1000000,"Invalid page/size");
        return jdbc.queryForList("""
                SELECT * FROM parcel.sorting_attempts WHERE organization_id=? AND belt_id=? AND captured_at>=? AND captured_at<?
                ORDER BY captured_at DESC,attempt_id DESC LIMIT ? OFFSET ?
                """,org,beltId,from,to,size,(long)page*size);
    }
    @GetMapping("/attempts/{id}/events")
    public Object events(Authentication auth,@PathVariable UUID id) {
        long org=access.actor(auth.getName(),false).organizationId();
        access.one("SELECT attempt_id FROM parcel.sorting_attempts WHERE organization_id=? AND attempt_id=?",org,id);
        return jdbc.queryForList("""
                SELECT event_id,event_type,error_code,occurred_at,received_at,image_uri FROM parcel.device_events
                WHERE organization_id=? AND attempt_id=? ORDER BY occurred_at,event_id
                """,org,id);
    }
    @GetMapping("/events/{id}/image")
    public ResponseEntity<byte[]> image(Authentication auth,@PathVariable UUID id) {
        long org=access.actor(auth.getName(),false).organizationId();
        var row=access.one("""
                SELECT i.content,i.content_type FROM parcel.event_images i
                JOIN parcel.device_events e ON e.event_id=i.event_id WHERE e.organization_id=? AND e.event_id=?
                """,org,id);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options","nosniff")
                .contentType(MediaType.parseMediaType((String)row.get("content_type"))).body((byte[])row.get("content"));
    }
    @GetMapping("/stats")
    public Object stats(Authentication auth,@RequestParam long beltId,@RequestParam OffsetDateTime from,@RequestParam OffsetDateTime to) {
        long org=access.actor(auth.getName(),false).organizationId(); access.belt(org,beltId);
        require(from.isBefore(to),"from must precede to");
        return jdbc.queryForMap("""
                SELECT count(*) AS attempts,
                count(*) FILTER (WHERE decision_status='MATCHED') AS matched,
                count(*) FILTER (WHERE decision_status='ERROR') AS decision_errors,
                count(*) FILTER (WHERE discharge_status='FAILED') AS discharge_failed,
                count(*) FILTER (WHERE discharge_conflict) AS discharge_conflicts
                FROM parcel.sorting_attempts WHERE organization_id=? AND belt_id=? AND captured_at>=? AND captured_at<?
                """,org,beltId,from,to);
    }
    @GetMapping("/notifications")
    public Object notifications(Authentication auth) {
        var actor=access.actor(auth.getName(),false);
        return jdbc.queryForList("""
                SELECT n.id,n.event_id,n.created_at,e.error_code,e.belt_id FROM parcel.user_notifications n
                JOIN parcel.device_events e ON e.event_id=n.event_id
                WHERE n.organization_id=? AND n.recipient_user_id=? AND n.read_at IS NULL
                ORDER BY n.created_at DESC,n.id DESC LIMIT 100
                """,actor.organizationId(),actor.id());
    }
}
