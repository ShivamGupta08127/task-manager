package com.taskmanager.app.repository;

import com.taskmanager.app.entity.Project;
import com.taskmanager.app.entity.ProjectMember;
import com.taskmanager.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    List<ProjectMember> findByUser(User user);
    List<ProjectMember> findByProject(Project project);
    Optional<ProjectMember> findByProjectAndUser(Project project, User user);
}
