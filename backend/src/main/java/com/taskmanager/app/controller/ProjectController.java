package com.taskmanager.app.controller;

import com.taskmanager.app.entity.Project;
import com.taskmanager.app.entity.ProjectMember;
import com.taskmanager.app.entity.User;
import com.taskmanager.app.entity.enums.Role;
import com.taskmanager.app.repository.ProjectMemberRepository;
import com.taskmanager.app.repository.ProjectRepository;
import com.taskmanager.app.repository.UserRepository;
import com.taskmanager.app.security.AuthContextService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProjectController {
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final AuthContextService authContextService;

    @GetMapping
    public List<Project> all(HttpServletRequest request) {
        User current = authContextService.getCurrentUser(request);
        if (current.getRole() == Role.ADMIN) {
            return projectRepository.findAll();
        }

        List<Project> projects = new ArrayList<>();
        List<ProjectMember> members = memberRepository.findByUser(current);
        for (ProjectMember member : members) {
            projects.add(member.getProject());
        }
        return projects;
    }

    @PostMapping
    public Project create(@RequestBody Project project, HttpServletRequest request) {
        User current = authContextService.getCurrentUser(request);
        if (current.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only admin can create project");
        }
        if (project.getName() == null || project.getName().isBlank()) {
            throw new RuntimeException("Project name is required");
        }
        project.setCreatedBy(current);
        Project saved = projectRepository.save(project);
        ProjectMember ownerMembership = new ProjectMember();
        ownerMembership.setProject(saved);
        ownerMembership.setUser(current);
        memberRepository.save(ownerMembership);
        return saved;
    }

    @PostMapping("/{projectId}/members/{userId}")
    public Map<String, String> addMember(@PathVariable Long projectId, @PathVariable Long userId, HttpServletRequest request) {
        User current = authContextService.getCurrentUser(request);
        if (current.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only admin can manage team");
        }
        Project project = projectRepository.findById(projectId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        if (memberRepository.findByProjectAndUser(project, user).isEmpty()) {
            ProjectMember member = new ProjectMember();
            member.setProject(project);
            member.setUser(user);
            memberRepository.save(member);
        }
        Map<String, String> response = new HashMap<>();
        response.put("message", "Member added");
        return response;
    }

    @GetMapping("/{projectId}/members")
    public List<User> members(@PathVariable Long projectId, HttpServletRequest request) {
        User current = authContextService.getCurrentUser(request);
        Project project = projectRepository.findById(projectId).orElseThrow();
        if (current.getRole() != Role.ADMIN && memberRepository.findByProjectAndUser(project, current).isEmpty()) {
            throw new RuntimeException("Not allowed to view project team");
        }

        List<User> users = new ArrayList<>();
        List<ProjectMember> members = memberRepository.findByProject(project);
        for (ProjectMember member : members) {
            users.add(member.getUser());
        }
        return users;
    }
}
