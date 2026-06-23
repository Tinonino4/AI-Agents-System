package com.swfactory.sdlc.infrastructure.rest;

import com.swfactory.sdlc.application.usecase.OrchestrateDevelopmentPhaseUseCase;
import com.swfactory.sdlc.domain.model.AgentTask;
import com.swfactory.sdlc.domain.model.ProjectContext;
import com.swfactory.sdlc.domain.repository.AgentTaskRepository;
import com.swfactory.sdlc.domain.repository.ProjectContextRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
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
    private final AgentTaskRepository agentTaskRepository;

    public ProjectController(OrchestrateDevelopmentPhaseUseCase orchestrateUseCase,
                             ProjectContextRepository projectRepository,
                             AgentTaskRepository agentTaskRepository) {
        this.orchestrateUseCase = orchestrateUseCase;
        this.projectRepository = projectRepository;
        this.agentTaskRepository = agentTaskRepository;
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
     * Endpoint para crear un proyecto con requisitos (inicialmente en estado IDLE).
     *
     * @param request Datos del proyecto.
     */
    @PostMapping
    @Operation(summary = "Crear un nuevo contexto de proyecto sin arrancar la orquestación")
    public ResponseEntity<ProjectContext> createProject(@RequestBody OrchestrateRequest request) {
        ProjectContext context = ProjectContext.builder()
                .id(UUID.randomUUID())
                .name(request.name())
                .description(request.description())
                .repositoryUrl(request.repositoryUrl())
                .currentPhase("ANALYSIS")
                .status("IDLE")
                .phaseOutputs(new HashMap<>())
                .rejectionCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        context = projectRepository.save(context);
        return ResponseEntity.ok(context);
    }

    /**
     * Endpoint para arrancar o reanudar de forma asíncrona la orquestación de un proyecto.
     *
     * @param projectId El ID del proyecto.
     */
    @PostMapping("/{projectId}/orchestrate")
    @Operation(summary = "Arrancar o reanudar la orquestación del proyecto en segundo plano")
    public ResponseEntity<ProjectContext> orchestrateProject(@PathVariable UUID projectId) {
        ProjectContext context = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado: " + projectId));

        context.setStatus("RUNNING");
        context.setUpdatedAt(LocalDateTime.now());
        context = projectRepository.save(context);

        // Disparar la orquestación en segundo plano (hilo virtual asíncrono)
        orchestrateUseCase.orchestrateAsync(projectId);

        return ResponseEntity.accepted().body(context);
    }

    /**
     * Endpoint heredado/compatible para arrancar un flujo creando un proyecto e iniciando orquestación asíncrona.
     */
    @PostMapping("/orchestrate")
    @Operation(summary = "Crear un contexto e iniciar el pipeline de agentes asíncronamente")
    public ResponseEntity<ProjectContext> orchestrateProjectLegacy(@RequestBody OrchestrateRequest request) {
        ProjectContext context = ProjectContext.builder()
                .id(UUID.randomUUID())
                .name(request.name())
                .description(request.description())
                .repositoryUrl(request.repositoryUrl())
                .currentPhase("ANALYSIS")
                .status("RUNNING")
                .phaseOutputs(new HashMap<>())
                .rejectionCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        context = projectRepository.save(context);

        // Disparar la orquestación en segundo plano
        orchestrateUseCase.orchestrateAsync(context.getId());

        return ResponseEntity.accepted().body(context);
    }

    /**
     * Endpoint para pausar la orquestación de un proyecto en ejecución.
     */
    @PostMapping("/{projectId}/pause")
    @Operation(summary = "Pausar la ejecución del flujo del proyecto")
    public ResponseEntity<ProjectContext> pauseProject(@PathVariable UUID projectId) {
        ProjectContext context = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado: " + projectId));

        context.setStatus("PAUSED");
        context.setUpdatedAt(LocalDateTime.now());
        context = projectRepository.save(context);

        return ResponseEntity.ok(context);
    }

    /**
     * Endpoint para reiniciar el estado y salidas del proyecto a la fase inicial.
     */
    @PostMapping("/{projectId}/reset")
    @Operation(summary = "Reiniciar de cero el estado del proyecto y sus artefactos generados")
    public ResponseEntity<ProjectContext> resetProject(@PathVariable UUID projectId) {
        ProjectContext context = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado: " + projectId));

        context.setCurrentPhase("ANALYSIS");
        context.setStatus("IDLE");
        context.setRejectionCount(0);
        context.setPhaseOutputs(new HashMap<>());
        context.setUpdatedAt(LocalDateTime.now());
        context = projectRepository.save(context);

        return ResponseEntity.ok(context);
    }

    /**
     * Endpoint para obtener el contexto de un proyecto por su ID.
     */
    @GetMapping("/{projectId}")
    @Operation(summary = "Obtener el contexto del proyecto por su ID")
    public ResponseEntity<ProjectContext> getProjectById(@PathVariable UUID projectId) {
        return projectRepository.findById(projectId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Endpoint para obtener todos los proyectos.
     */
    @GetMapping
    @Operation(summary = "Obtener la lista de todos los proyectos")
    public ResponseEntity<List<ProjectContext>> getAllProjects() {
        List<ProjectContext> projects = projectRepository.findAll();
        return ResponseEntity.ok(projects);
    }

    /**
     * Endpoint para obtener todas las tareas asociadas a un proyecto.
     */
    @GetMapping("/{projectId}/tasks")
    @Operation(summary = "Obtener todas las tareas asociadas a un proyecto")
    public ResponseEntity<List<AgentTask>> getProjectTasks(@PathVariable UUID projectId) {
        List<AgentTask> tasks = agentTaskRepository.findByProjectId(projectId);
        return ResponseEntity.ok(tasks);
    }
}
