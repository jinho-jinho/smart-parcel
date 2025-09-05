package com.capstone.smart_parcel.config.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-validity-ms}")
    private long accessValidityMs;

    @Value("${jwt.refresh-token-validity-ms}")
    private long refreshValidityMs;

    private SecretKey key;

    @PostConstruct
    void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String buildToken(String email, Long userId, long validityMs, String typ, String jti) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + validityMs);
        return Jwts.builder()
                .id(jti)
                .subject(normalizeEmail(email))
                .claim("uid", userId)
                .claim("typ", typ) // "access" | "refresh"
                .issuedAt(now)
                .expiration(exp)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String createAccessToken(String email, Long userId) {
        return buildToken(email, userId, accessValidityMs, "access", UUID.randomUUID().toString());
    }

    public String createRefreshToken(String email, Long userId) {
        return buildToken(email, userId, refreshValidityMs, "refresh", UUID.randomUUID().toString());
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
    }

    public String getEmail(String token) { return parse(token).getPayload().getSubject(); }

    public Long getUserId(String token) {
        Number n = (Number) parse(token).getPayload().get("uid");
        return n == null ? null : n.longValue();
    }

    public String getType(String token) { return (String) parse(token).getPayload().get("typ"); }

    public String getJti(String token) { return parse(token).getPayload().getId(); }

    public Date getExpiration(String token) { return parse(token).getPayload().getExpiration(); }

    public boolean isAccessToken(String token) { return "access".equals(getType(token)); }

    public boolean isRefreshToken(String token) { return "refresh".equals(getType(token)); }
}