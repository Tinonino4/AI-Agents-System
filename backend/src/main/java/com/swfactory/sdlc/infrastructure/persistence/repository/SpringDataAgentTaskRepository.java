package com.swfactory.sdlc.infrastructure.persistence.repository;

import com.swfactory.sdlc.infrastructure.persistence.entity.AgentTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para gestionar entidades de tareas agénticas.
 */
@Repository
public interface SpringDataAgentTaskRepository extends JpaRepository<AgentTaskEntity, UUID> {
    List<AgentTaskEntity> findByProjectId(UUID projectId);
}
