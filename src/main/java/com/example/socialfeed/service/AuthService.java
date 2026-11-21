package com.example.socialfeed.service;

import com.example.socialfeed.dto.request.LoginRequest;
import com.example.socialfeed.dto.request.RegisterRequest;
import com.example.socialfeed.dto.response.AuthResponse;
import com.example.socialfeed.entity.User;
import com.example.socialfeed.exception.UserNameTaken;
import com.example.socialfeed.repository.UserRepository;
import com.example.socialfeed.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public AuthResponse register(RegisterRequest request) {
        // Fast path: rejects ordinary duplicates without attempting an insert.
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserNameTaken("Username already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserNameTaken("Email already registered");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        try {
            // Real guard: two concurrent requests can both pass the checks above,
            // so the database unique constraints decide who wins.
            // saveAndFlush sends the INSERT now, so the error is raised inside this try.
            // This method must NOT be @Transactional: after a failed flush the
            // persistence context is unusable, so the re-check below would break.
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            // The other request committed first. Ask again which field collided.
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new UserNameTaken("Username already taken");
            }
            throw new UserNameTaken("Email already registered");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        return AuthResponse.builder()
                .token(jwtUtil.generateToken(userDetails))
                .username(user.getUsername())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        return AuthResponse.builder()
                .token(jwtUtil.generateToken(userDetails))
                .username(request.getUsername())
                .build();
    }
}