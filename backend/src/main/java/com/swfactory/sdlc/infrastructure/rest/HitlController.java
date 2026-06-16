package com.swfactory.sdlc.infrastructure.rest;

import com.swfactory.sdlc.application.usecase.OrchestrateDevelopmentPhaseUseCase;
import com.swfactory.sdlc.domain.model.AgentTask;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador REST de Infraestructura.
 * Proporciona el punto de entrada para que el supervisor humano apruebe o rechace
 * las entregas de los agentes (Arquitectura, DevOps, etc.) mediante el patrón HITL.
 */
@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "Human In The Loop", description = "Endpoints para la supervisión humana de tareas de agentes")
public class HitlController {

    private final OrchestrateDevelopmentPhaseUseCase useCase;

    public HitlController(OrchestrateDevelopmentPhaseUseCase useCase) {
        this.useCase = useCase;
    }

    /**
     * DTO para representar la decisión de aprobación.
     */
    public record ApprovalRequest(
            boolean approved,
            String feedback
    ) {}

    /**
     * Endpoint para aprobar o rechazar una tarea agéntica.
     *
     * @param taskId  El identificador único de la tarea.
     * @param request La decisión del revisor (aprobado/rechazado) y su retroalimentación.
     */
    @PostMapping("/{taskId}/approve")
    @Operation(summary = "Aprobar o rechazar una tarea en espera de revisión humana")
    public ResponseEntity<AgentTask> approveOrRejectTask(
            @PathVariable UUID taskId,
            @RequestBody ApprovalRequest request) {
        
        AgentTask updatedTask = useCase.handleHitlApproval(taskId, request.approved(), request.feedback());
        return ResponseEntity.ok(updatedTask);
    }
}
