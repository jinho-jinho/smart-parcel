package com.capstone.smart_parcel.config;
import com.capstone.smart_parcel.config.security.DeviceAuthentication;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@RequiredArgsConstructor
public class DeviceSecurityConfig {
    private final DeviceAuthentication devices;
    @Bean @Order(1)
    SecurityFilterChain deviceChain(HttpSecurity http) throws Exception {
        return http.securityMatcher("/api/devices/setup", "/api/devices/applied-version", "/api/devices/events", "/api/devices/events/**")
                .csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.anyRequest().hasAuthority("DEVICE"))
                .exceptionHandling(e -> e.authenticationEntryPoint((q,r,x) -> r.sendError(401))
                        .accessDeniedHandler((q,r,x) -> r.sendError(403)))
                .addFilterBefore(new OncePerRequestFilter() {
                    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                                               FilterChain chain) throws ServletException, IOException {
                        var identity = devices.authenticate(req.getHeader("X-Device-Id"), req.getHeader("X-Device-Key"));
                        if (identity == null) { res.sendError(401, "Valid device credentials required"); return; }
                        SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(identity,null,
                                        List.of(new SimpleGrantedAuthority("DEVICE"))));
                        chain.doFilter(req,res);
                    }
                }, UsernamePasswordAuthenticationFilter.class).build();
    }
}
