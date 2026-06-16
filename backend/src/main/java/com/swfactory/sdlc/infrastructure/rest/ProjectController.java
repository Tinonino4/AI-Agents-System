package com.swfactory.sdlc.infrastructure.rest;

import com.swfactory.sdlc.application.usecase.OrchestrateDevelopmentPhaseUseCase;
import com.swfactory.sdlc.domain.model.ProjectContext;
import com.swfactory.sdlc.domain.repository.ProjectContextRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

/**
 * Controlador REST de Infraestructura.
 * Expone endpoints para crear un proyecto con requisitos de entrada e iniciar
 * la tubería de agentes autónomos.
 */
@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Projects", description = "Endpoints para la gestión y ejecución de proyectos de software")
public class ProjectController {

    private final OrchestrateDevelopmentPhaseUseCase orchestrateUseCase;
    private final ProjectContextRepository projectRepository;

    public ProjectController(OrchestrateDevelopmentPhaseUseCase orchestrateUseCase,
                             ProjectContextRepository projectRepository) {
        this.orchestrateUseCase = orchestrateUseCase;
        this.projectRepository = projectRepository;
    }

    /**
     * DTO para la solicitud de orquestación.
     */
    public record OrchestrateRequest(
            String name,
            String description,
            String repositoryUrl
    ) {}

    /**
     * Endpoint para inicializar un proyecto con requisitos y arrancar el pipeline de agentes.
     *
     * @param request Datos del proyecto y descripción de la feature a construir.
     */
    @PostMapping("/orchestrate")
    @Operation(summary = "Crear un contexto e iniciar el pipeline de agentes (PO -> Architect -> ...)")
    public ResponseEntity<ProjectContext> orchestrateProject(@RequestBody OrchestrateRequest request) {
        ProjectContext context = ProjectContext.builder()
                .id(UUID.randomUUID())
                .name(request.name())
                .description(request.description())
                .repositoryUrl(request.repositoryUrl())
                .currentPhase("ANALYSIS")
                .phaseOutputs(new HashMap<>())
                .rejectionCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Persistir el contexto inicial del proyecto
        context = projectRepository.save(context);

        // Disparar la tubería agéntica (comienza por la fase ANALYSIS invocando a @po-agent)
        ProjectContext result = orchestrateUseCase.orchestrate(context);
        return ResponseEntity.ok(result);
    }
}
