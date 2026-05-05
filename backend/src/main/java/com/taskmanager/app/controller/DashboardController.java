package com.taskmanager.app.controller;

import com.taskmanager.app.entity.ProjectMember;
import com.taskmanager.app.entity.Task;
import com.taskmanager.app.entity.User;
import com.taskmanager.app.entity.enums.Role;
import com.taskmanager.app.entity.enums.TaskStatus;
import com.taskmanager.app.repository.ProjectMemberRepository;
import com.taskmanager.app.repository.ProjectRepository;
import com.taskmanager.app.repository.TaskRepository;
import com.taskmanager.app.security.AuthContextService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DashboardController {
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final AuthContextService authContextService;

    @GetMapping
    public Map<String, Object> summary(HttpServletRequest request) {
        User current = authContextService.getCurrentUser(request);
        Map<String, Object> result = new HashMap<>();

        List<Task> visibleTasks = new ArrayList<>();
        if (current.getRole() == Role.ADMIN) {
            visibleTasks = taskRepository.findAll();
        } else {
            List<ProjectMember> memberships = memberRepository.findByUser(current);
            for (ProjectMember member : memberships) {
                visibleTasks.addAll(taskRepository.findByProject(member.getProject()));
            }
        }

        long todo = 0;
        long inProgress = 0;
        long done = 0;
        long overdue = 0;
        LocalDate today = LocalDate.now();

        for (Task task : visibleTasks) {
            if (task.getStatus() == TaskStatus.TODO) {
                todo++;
            }
            if (task.getStatus() == TaskStatus.IN_PROGRESS) {
                inProgress++;
            }
            if (task.getStatus() == TaskStatus.DONE) {
                done++;
            }
            if (task.getDueDate() != null && task.getDueDate().isBefore(today) && task.getStatus() != TaskStatus.DONE) {
                overdue++;
            }
        }

        result.put("myTasks", taskRepository.findByAssignedTo(current).size());
        result.put("totalTasks", visibleTasks.size());
        if (current.getRole() == Role.ADMIN) {
            result.put("totalProjects", projectRepository.count());
        } else {
            result.put("totalProjects", memberRepository.findByUser(current).size());
        }
        result.put("todo", todo);
        result.put("inProgress", inProgress);
        result.put("done", done);
        result.put("overdue", overdue);
        return result;
    }
}
