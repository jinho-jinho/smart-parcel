package com.capstone.smart_parcel.config.jwt;

import com.capstone.smart_parcel.config.security.CustomUserDetailsService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwt;
    private final CustomUserDetailsService uds;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String auth = req.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                // access 토큰만 인증에 사용
                if (!jwt.isAccessToken(token)) {
                    SecurityContextHolder.clearContext();
                    chain.doFilter(req, res);
                    return;
                }

                String email = jwt.getEmail(token); // subject (정규화 가정)
                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    var userDetails = uds.loadUserByUsername(email);
                    var authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext(); // 손상/만료/형식오류 등
            }
        }
        chain.doFilter(req, res);
    }
}
