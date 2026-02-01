package com.example.backend.modules.auth.service;

import com.example.backend.modules.auth.dto.AuthenticationRequest;
import com.example.backend.modules.auth.dto.AuthenticationResponse;
import com.example.backend.modules.admin.usermanagement.entity.User;
import com.example.backend.modules.admin.usermanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

        private final UserRepository repository;
        private final JwtService jwtService;
        private final AuthenticationManager authenticationManager;

        public AuthenticationResponse authenticate(AuthenticationRequest request) {

                authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                request.getUsername(),
                                                request.getPassword()));

                var user = repository.findByUsername(request.getUsername())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                var jwtToken = jwtService.generateToken(user);

                return AuthenticationResponse.builder()
                                .token(jwtToken)
                                .build();
        }
}
