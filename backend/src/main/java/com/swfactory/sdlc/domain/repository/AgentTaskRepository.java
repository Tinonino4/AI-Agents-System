package com.swfactory.sdlc.domain.repository;

import com.swfactory.sdlc.domain.model.AgentTask;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de Dominio. Interfaz de persistencia para tareas agénticas.
 */
public interface AgentTaskRepository {
    AgentTask save(AgentTask task);
    Optional<AgentTask> findById(UUID id);
}
