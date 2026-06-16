package com.swfactory.sdlc.domain.agent;

import com.swfactory.sdlc.domain.model.AgentTask;
import com.swfactory.sdlc.domain.model.ProjectContext;

/**
 * Puerto de Dominio. Interfaz genérica para definir el comportamiento de un Nodo Agente
 * en la factoría de software autónoma.
 */
public interface AgentNode {
    
    /**
     * Obtiene el rol específico del agente (ej: PRODUCT_OWNER, ARCHITECT).
     */
    String getRole();

    /**
     * Ejecuta una tarea agéntica utilizando el contexto actual del proyecto.
     *
     * @param task    La tarea asignada a procesar.
     * @param context El contexto global del proyecto.
     * @return La tarea actualizada con el resultado de la ejecución.
     */
    AgentTask execute(AgentTask task, ProjectContext context);
}
