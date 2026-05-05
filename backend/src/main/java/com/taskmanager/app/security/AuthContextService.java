package com.taskmanager.app.security;

import com.taskmanager.app.entity.User;
import com.taskmanager.app.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class AuthContextService {
    @Value("${app.jwt-secret}")
    private String secret;
    private final UserRepository userRepository;

    public User getCurrentUser(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new RuntimeException("Missing token");
        }
        String token = header.substring(7);
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String email = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }
}
