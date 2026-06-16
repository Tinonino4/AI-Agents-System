package com.swfactory.sdlc.application.service;

import com.swfactory.sdlc.application.usecase.OrchestrateDevelopmentPhaseUseCase;
import com.swfactory.sdlc.domain.agent.AgentNode;
import com.swfactory.sdlc.domain.model.AgentTask;
import com.swfactory.sdlc.domain.model.ProjectContext;
import com.swfactory.sdlc.domain.repository.AgentTaskRepository;
import com.swfactory.sdlc.domain.repository.ProjectContextRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Servicio de Aplicación. Implementación del caso de uso de orquestación,
 * controlando la máquina de estados de las fases de desarrollo (PO -> Architect -> Backend -> QA -> SRE)
 * y gestionando el ciclo de recuperación automática (Self-Healing) ante fallos en pruebas de QA.
 */
@Service
public class OrchestrateDevelopmentPhaseService implements OrchestrateDevelopmentPhaseUseCase {

    private static final Logger log = LoggerFactory.getLogger(OrchestrateDevelopmentPhaseService.class);

    private final ProjectContextRepository projectContextRepository;
    private final AgentTaskRepository agentTaskRepository;
    private final List<AgentNode> agents;

    public OrchestrateDevelopmentPhaseService(ProjectContextRepository projectContextRepository,
                                               AgentTaskRepository agentTaskRepository,
                                               List<AgentNode> agents) {
        this.projectContextRepository = projectContextRepository;
        this.agentTaskRepository = agentTaskRepository;
        this.agents = agents;
    }

    @Override
    public ProjectContext orchestrate(ProjectContext context) {
        log.info("Orquestando fase de desarrollo para el proyecto: {} (Fase actual: {})", 
                context.getName(), context.getCurrentPhase());

        String targetRole = determineRoleForPhase(context.getCurrentPhase());
        AgentNode agent = findAgentByRole(targetRole);

        if (agent == null) {
            log.warn("No se encontró ningún agente para el rol: {}. Orquestación en espera.", targetRole);
            return context;
        }

        // Determinar datos de entrada de la tarea
        String inputData = context.getPhaseOutputs().getOrDefault("LatestInput", "");
        if (inputData.isEmpty()) {
            // Fallback si no hay feedback correctivo: usar salida de la fase anterior
            inputData = getPreviousPhaseOutput(context);
        }

        // Crear una nueva tarea para el agente en esta fase
        AgentTask task = AgentTask.builder()
                .id(UUID.randomUUID())
                .projectId(context.getId())
                .agentRole(agent.getRole())
                .title("Generar salida para la fase " + context.getCurrentPhase())
                .description("Procesamiento automático de la fase " + context.getCurrentPhase() + " del proyecto " + context.getName())
                .status("IN_PROGRESS")
                .inputData(inputData)
                .build();

        agentTaskRepository.save(task);

        // Imprimir traza de auditoría de entrada
        log.info("\n================================================================================\n" +
                 "[AUDIT] INICIANDO EJECUCIÓN DEL AGENTE: {}\n" +
                 "[PROYECTO]: {} | [FASE]: {}\n" +
                 "[INPUT DATA]:\n{}\n" +
                 "================================================================================\n",
                 agent.getRole(), context.getName(), context.getCurrentPhase(), inputData);

        // Ejecutar agente (escribe en filesystem / llama LLM)
        task = agent.execute(task, context);

        // Imprimir traza de auditoría de salida
        log.info("\n================================================================================\n" +
                 "[AUDIT] FINALIZADA EJECUCIÓN DEL AGENTE: {}\n" +
                 "[STATUS]: {}\n" +
                 "[OUTPUT DATA]:\n{}\n" +
                 "================================================================================\n",
                 agent.getRole(), task.getStatus(), task.getOutputData());

        // ciclo de Autocuración (Self-Healing) si falla la verificación de QA
        if ("QA".equals(agent.getRole()) && "FAILED".equals(task.getStatus())) {
            int currentRejections = context.getRejectionCount();
            if (currentRejections < 3) {
                context.setRejectionCount(currentRejections + 1);
                // Enrutar hacia atras: de TESTING a DEVELOPMENT
                context.setCurrentPhase("DEVELOPMENT");
                // Pasar las trazas de error al Backend en LatestInput
                context.getPhaseOutputs().put("LatestInput", "Errores detectados en verificación de QA:\n" + task.getOutputData());
                projectContextRepository.save(context);

                log.warn("Verificación de QA fallida (Reintento {}/3). Devolviendo flujo a BACKEND.", currentRejections + 1);
                
                // Reanudar ejecución automáticamente volviendo a invocar al Backend
                return orchestrate(context);
            } else {
                // Superado el límite de intentos, requerir intervención humana (HITL)
                task.setStatus("WAITING_FOR_HITL");
                task.setReviewerFeedback("Fallo de QA persistente tras 3 reintentos automáticos. Revisar trazas de tests.");
                agentTaskRepository.save(task);
                log.error("Bucle de autocuración de QA superado. Flujo detenido en espera de revisión humana (HITL).");
                return context;
            }
        }

        // Si la fase requiere aprobación humana antes de avanzar (ej: Arquitectura o Despliegue)
        if (requiresHitl(context.getCurrentPhase())) {
            task.setStatus("WAITING_FOR_HITL");
            agentTaskRepository.save(task);
            log.info("Tarea {} asignada a {} requiere aprobación humana (HITL). Deteniendo flujo.", task.getId(), agent.getRole());
            return context;
        }

        // Si es exitoso, resetear contador de rechazos
        if ("QA".equals(agent.getRole())) {
            context.setRejectionCount(0);
        }

        // Si pasa todas las validaciones, completar la tarea
        task.setStatus("COMPLETED");
        agentTaskRepository.save(task);

        // Guardar el artefacto en las salidas del proyecto y limpiar LatestInput
        context.getPhaseOutputs().put(context.getCurrentPhase(), task.getOutputData());
        context.getPhaseOutputs().remove("LatestInput");
        
        // Transición automática de fase
        String nextPhase = getNextPhase(context.getCurrentPhase());
        context.setCurrentPhase(nextPhase);
        projectContextRepository.save(context);

        log.info("Fase completada con éxito. Avanzando a la fase: {}", nextPhase);

        // Si no hemos llegado al final, continuar orquestando de manera secuencial.
        // Las fases que requieran aprobación humana se detendrán en su propia ejecución tras invocar al agente correspondiente.
        if (!"COMPLETED".equals(nextPhase)) {
            return orchestrate(context);
        }

        return context;
    }

    @Override
    public AgentTask handleHitlApproval(UUID taskId, boolean approved, String feedback) {
        AgentTask task = agentTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada con ID: " + taskId));

        if (!"WAITING_FOR_HITL".equals(task.getStatus())) {
            throw new IllegalStateException("La tarea no está en espera de revisión humana. Estado actual: " + task.getStatus());
        }

        ProjectContext context = projectContextRepository.findById(task.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado para la tarea: " + task.getProjectId()));

        if (approved) {
            log.info("Tarea {} aprobada por el revisor humano.", taskId);
            task.setStatus("APPROVED");
            agentTaskRepository.save(task);

            // Guardar salida en el contexto
            context.getPhaseOutputs().put(context.getCurrentPhase(), task.getOutputData());
            context.getPhaseOutputs().remove("LatestInput");
            
            // Avanzar a la siguiente fase
            String nextPhase = getNextPhase(context.getCurrentPhase());
            context.setCurrentPhase(nextPhase);
            projectContextRepository.save(context);

            // Reanudar orquestación
            orchestrate(context);
        } else {
            log.info("Tarea {} rechazada por el revisor humano. Motivo: {}", taskId, feedback);
            task.setStatus("REJECTED");
            task.setReviewerFeedback(feedback);
            agentTaskRepository.save(task);

            // Guardar feedback para retroalimentar al agente actual
            context.getPhaseOutputs().put("LatestInput", "Retroalimentación de supervisor humano: " + feedback);
            projectContextRepository.save(context);
        }

        return task;
    }

    private String determineRoleForPhase(String phase) {
        return switch (phase) {
            case "ANALYSIS" -> "PRODUCT_OWNER";
            case "ARCHITECTURE" -> "ARCHITECT";
            case "DEVELOPMENT" -> "BACKEND";
            case "TESTING" -> "QA";
            case "DEPLOYMENT" -> "SRE";
            default -> throw new IllegalArgumentException("Fase desconocida: " + phase);
        };
    }

    private AgentNode findAgentByRole(String role) {
        return agents.stream()
                .filter(a -> a.getRole().equalsIgnoreCase(role))
                .findFirst()
                .orElse(null);
    }

    private boolean requiresHitl(String phase) {
        return "ARCHITECTURE".equals(phase) || "DEPLOYMENT".equals(phase);
    }

    private String getNextPhase(String currentPhase) {
        return switch (currentPhase) {
            case "ANALYSIS" -> "ARCHITECTURE";
            case "ARCHITECTURE" -> "DEVELOPMENT";
            case "DEVELOPMENT" -> "TESTING";
            case "TESTING" -> "DEPLOYMENT";
            case "DEPLOYMENT" -> "COMPLETED";
            default -> "COMPLETED";
        };
    }

    private String getPreviousPhaseOutput(ProjectContext context) {
        return switch (context.getCurrentPhase()) {
            case "ARCHITECTURE" -> context.getPhaseOutputs().getOrDefault("ANALYSIS", "");
            case "DEVELOPMENT" -> context.getPhaseOutputs().getOrDefault("ARCHITECTURE", "");
            case "TESTING" -> context.getPhaseOutputs().getOrDefault("DEVELOPMENT", "");
            case "DEPLOYMENT" -> context.getPhaseOutputs().getOrDefault("TESTING", "");
            default -> "";
        };
    }
}
