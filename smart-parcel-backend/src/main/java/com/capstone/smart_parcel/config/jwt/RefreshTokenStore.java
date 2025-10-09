package com.capstone.smart_parcel.config.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private final StringRedisTemplate redis;

    /**
     * 새로운 RefreshToken jti 저장
     * @param jti 토큰 고유 ID
     * @param userId 사용자 ID
     * @param ttlMillis TTL (RefreshToken 만료 시간과 동일)
     */
    public void saveJti(String jti, Long userId, long ttlMillis) {
        String key = "RTJTI:" + jti;
        redis.opsForValue().set(key, String.valueOf(userId), Duration.ofMillis(ttlMillis));
    }

    /** jti가 Redis에 존재하는지 확인 (유효성 체크) */
    public boolean existsJti(String jti) {
        String key = "RTJTI:" + jti;
        return redis.hasKey(key);
    }
    /** jti 삭제 (로그아웃 or 회전 시 사용) */
    public void deleteJti(String jti) {
        redis.delete("RTJTI:" + jti);
    }
}
