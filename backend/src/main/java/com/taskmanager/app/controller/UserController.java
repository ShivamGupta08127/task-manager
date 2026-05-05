package com.taskmanager.app.controller;

import com.taskmanager.app.entity.User;
import com.taskmanager.app.entity.enums.Role;
import com.taskmanager.app.repository.UserRepository;
import com.taskmanager.app.security.AuthContextService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {
    private final UserRepository userRepository;
    private final AuthContextService authContextService;

    @GetMapping("/me")
    public User me(HttpServletRequest request) {
        return authContextService.getCurrentUser(request);
    }

    @GetMapping
    public List<User> list(HttpServletRequest request) {
        User current = authContextService.getCurrentUser(request);
        if (current.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only admin can list users");
        }
        return userRepository.findAll();
    }
}
