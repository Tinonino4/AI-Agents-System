package com.swfactory.sdlc.infrastructure.persistence.repository;

import com.swfactory.sdlc.infrastructure.persistence.entity.ProjectContextEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repositorio Spring Data JPA para gestionar entidades de contexto de proyecto.
 */
@Repository
public interface SpringDataProjectContextRepository extends JpaRepository<ProjectContextEntity, UUID> {
}
