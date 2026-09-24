package com.capstone.smart_parcel.parcel;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ParcelAccess {
    private final JdbcTemplate jdbc;
    public record Actor(long id, long organizationId, boolean manager) {}
    public Actor actor(String email, boolean write) {
        var rows = jdbc.queryForList("SELECT id,organization_id,role::text FROM public.users WHERE email=?", email);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var r = rows.get(0);
        var actor = new Actor(number(r,"id"), number(r,"organization_id"), "MANAGER".equals(r.get("role")));
        if (write && !actor.manager()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Manager role required");
        return actor;
    }
    public Map<String,Object> belt(long organization, long belt) {
        return one("SELECT * FROM parcel.conveyor_belts WHERE organization_id=? AND id=?", organization, belt);
    }
    public Map<String,Object> one(String sql, Object... args) {
        var rows = jdbc.queryForList(sql,args);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found in this organization");
        return rows.get(0);
    }
    public static long number(Map<String,Object> row, String key) { return ((Number)row.get(key)).longValue(); }
    public static void require(boolean condition, String message) {
        if (!condition) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message);
    }
    public static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT,message);
    }
}
