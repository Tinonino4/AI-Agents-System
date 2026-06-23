package com.swfactory.sdlc.application.usecase;

import com.swfactory.sdlc.domain.model.AgentTask;
import com.swfactory.sdlc.domain.model.ProjectContext;

import java.util.UUID;

/**
 * Caso de uso de Aplicación. Orquesta la transición de fases del ciclo de vida
 * de desarrollo (SDLC) delegando tareas en los subagentes correspondientes y
 * gestionando el patrón Human-in-the-Loop (HITL).
 */
public interface OrchestrateDevelopmentPhaseUseCase {

    /**
     * Inicia u orquesta la fase actual del desarrollo del proyecto.
     *
     * @param context El contexto de proyecto actual.
     * @return El contexto de proyecto actualizado.
     */
    ProjectContext orchestrate(ProjectContext context);

    /**
     * Inicia u orquesta de forma asíncrona la fase actual del desarrollo del proyecto en segundo plano.
     *
     * @param projectId El ID del proyecto.
     */
    void orchestrateAsync(UUID projectId);

    /**
     * Resuelve el estado de una tarea en espera de aprobación humana (HITL).
     *
     * @param taskId   El ID de la tarea.
     * @param approved Determina si la tarea es aprobada o rechazada.
     * @param feedback Comentarios del revisor (obligatorio si se rechaza).
     * @return La tarea actualizada con su nuevo estado.
     */
    AgentTask handleHitlApproval(UUID taskId, boolean approved, String feedback);
}
