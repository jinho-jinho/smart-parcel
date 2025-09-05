package com.capstone.smart_parcel.config.security;

import com.capstone.smart_parcel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var u = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("user not found"));
        return org.springframework.security.core.userdetails.User
                .withUsername(u.getEmail())
                .password(u.getPassword())
                .authorities("ROLE_USER")
                .build();
    }
}

