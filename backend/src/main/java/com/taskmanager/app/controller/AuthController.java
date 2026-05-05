package com.taskmanager.app.controller;

import com.taskmanager.app.dto.auth.AuthDtos;
import com.taskmanager.app.entity.User;
import com.taskmanager.app.entity.enums.Role;
import com.taskmanager.app.repository.UserRepository;
import com.taskmanager.app.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/signup")
    public AuthDtos.AuthResponse signup(@Valid @RequestBody AuthDtos.SignupRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));

        if ("ADMIN".equalsIgnoreCase(request.role())) {
            user.setRole(Role.ADMIN);
        } else {
            user.setRole(Role.MEMBER);
        }

        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return new AuthDtos.AuthResponse(token, user.getFullName(), user.getEmail(), user.getRole().name());
    }

    @PostMapping("/login")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return new AuthDtos.AuthResponse(token, user.getFullName(), user.getEmail(), user.getRole().name());
    }
}
