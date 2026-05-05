package com.taskmanager.app.repository;

import com.taskmanager.app.entity.Project;
import com.taskmanager.app.entity.Task;
import com.taskmanager.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProject(Project project);
    List<Task> findByAssignedTo(User user);
    long countByAssignedToAndDueDateBeforeAndStatusNot(User user, LocalDate date, com.taskmanager.app.entity.enums.TaskStatus status);
}
