package com.swfactory.sdlc.domain.repository;

import com.swfactory.sdlc.domain.model.ProjectContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de Dominio. Interfaz de persistencia para el contexto del proyecto.
 */
public interface ProjectContextRepository {
    ProjectContext save(ProjectContext context);
    Optional<ProjectContext> findById(UUID id);
    List<ProjectContext> findAll();
}
