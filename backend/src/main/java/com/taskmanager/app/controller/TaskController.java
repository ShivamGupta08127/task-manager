package com.taskmanager.app.controller;

import com.taskmanager.app.entity.Project;
import com.taskmanager.app.entity.ProjectMember;
import com.taskmanager.app.entity.Task;
import com.taskmanager.app.entity.User;
import com.taskmanager.app.entity.enums.Role;
import com.taskmanager.app.entity.enums.TaskStatus;
import com.taskmanager.app.repository.ProjectMemberRepository;
import com.taskmanager.app.repository.ProjectRepository;
import com.taskmanager.app.repository.TaskRepository;
import com.taskmanager.app.repository.UserRepository;
import com.taskmanager.app.security.AuthContextService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TaskController {
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final AuthContextService authContextService;

    @PostMapping
    public Task create(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        User current = authContextService.getCurrentUser(httpRequest);

        if (request.get("projectId") == null || request.get("projectId").isBlank()) {
            throw new RuntimeException("Project is required");
        }

        Project project = projectRepository.findById(Long.parseLong(request.get("projectId"))).orElseThrow();
        if (current.getRole() != Role.ADMIN) {
            if (memberRepository.findByProjectAndUser(project, current).isEmpty()) {
                throw new RuntimeException("Not allowed to create tasks in this project");
            }
        }

        String title = request.get("title");
        if (title == null || title.isBlank()) {
            throw new RuntimeException("Task title is required");
        }

        User assigned = null;
        String assignedToId = request.get("assignedToId");
        if (assignedToId != null && !assignedToId.isBlank()) {
            assigned = userRepository.findById(Long.parseLong(assignedToId)).orElseThrow();
            if (memberRepository.findByProjectAndUser(project, assigned).isEmpty()) {
                throw new RuntimeException("Assignee must be a project team member");
            }
        }

        Task task = new Task();
        task.setTitle(title.trim());
        task.setDescription(request.get("description"));
        task.setProject(project);
        task.setAssignedTo(assigned);
        if (request.get("dueDate") != null && !request.get("dueDate").isBlank()) {
            task.setDueDate(LocalDate.parse(request.get("dueDate")));
        }
        return taskRepository.save(task);
    }

    @GetMapping
    public List<Task> all(HttpServletRequest httpRequest) {
        User current = authContextService.getCurrentUser(httpRequest);
        if (current.getRole() == Role.ADMIN) {
            return taskRepository.findAll();
        }

        List<Task> tasks = new ArrayList<>();
        List<ProjectMember> memberships = memberRepository.findByUser(current);
        for (ProjectMember member : memberships) {
            List<Task> projectTasks = taskRepository.findByProject(member.getProject());
            tasks.addAll(projectTasks);
        }
        return tasks;
    }

    @PatchMapping("/{id}/status")
    public Task updateStatus(@PathVariable Long id, @RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        User current = authContextService.getCurrentUser(httpRequest);
        Task task = taskRepository.findById(id).orElseThrow();
        if (current.getRole() != Role.ADMIN) {
            if (memberRepository.findByProjectAndUser(task.getProject(), current).isEmpty()) {
                throw new RuntimeException("Not allowed to update this task");
            }
        }
        task.setStatus(TaskStatus.valueOf(request.get("status")));
        return taskRepository.save(task);
    }

    @PutMapping("/{id}")
    public Task update(@PathVariable Long id, @RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        User current = authContextService.getCurrentUser(httpRequest);
        Task task = taskRepository.findById(id).orElseThrow();

        if (current.getRole() != Role.ADMIN) {
            if (memberRepository.findByProjectAndUser(task.getProject(), current).isEmpty()) {
                throw new RuntimeException("Not allowed to update this task");
            }
        }

        String title = request.get("title");
        if (title == null || title.isBlank()) {
            throw new RuntimeException("Task title is required");
        }
        task.setTitle(title.trim());
        task.setDescription(request.get("description"));
        if (request.get("dueDate") == null || request.get("dueDate").isBlank()) {
            task.setDueDate(null);
        } else {
            task.setDueDate(LocalDate.parse(request.get("dueDate")));
        }

        User assigned = null;
        String assignedToId = request.get("assignedToId");
        if (assignedToId != null && !assignedToId.isBlank()) {
            assigned = userRepository.findById(Long.parseLong(assignedToId)).orElseThrow();
            if (memberRepository.findByProjectAndUser(task.getProject(), assigned).isEmpty()) {
                throw new RuntimeException("Assignee must be a project team member");
            }
        }
        task.setAssignedTo(assigned);

        if (request.get("status") != null && !request.get("status").isBlank()) {
            task.setStatus(TaskStatus.valueOf(request.get("status")));
        }
        return taskRepository.save(task);
    }
}
