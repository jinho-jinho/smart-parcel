package com.capstone.smart_parcel.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.protocol.ProtocolVersion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.password}")
    private String password;

    @Value("${spring.data.redis.username}")
    private String username;

    @Value("${spring.data.redis.ssl.enabled:true}")
    private boolean ssl;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration();
        serverConfig.setHostName(host);
        serverConfig.setPort(port);
        serverConfig.setPassword(password);
        serverConfig.setUsername(username);

        var clientBuilder = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(60000)) // 60초 타임아웃
                .clientOptions(ClientOptions.builder()
                        .protocolVersion(ProtocolVersion.RESP2) // Azure 호환성 유지
                        .build());
        if (ssl) clientBuilder.useSsl();
        LettuceClientConfiguration clientConfig = clientBuilder.build();

        return new LettuceConnectionFactory(serverConfig, clientConfig);
    }
}