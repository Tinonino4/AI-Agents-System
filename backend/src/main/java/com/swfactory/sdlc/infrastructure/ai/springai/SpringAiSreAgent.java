package com.swfactory.sdlc.infrastructure.ai.springai;

import com.swfactory.sdlc.domain.agent.AgentNode;
import com.swfactory.sdlc.domain.model.AgentTask;
import com.swfactory.sdlc.domain.model.ProjectContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Adaptador de Infraestructura. Implementa el agente DevOps (SRE)
 * para generar configuraciones Docker, CI/CD y despliegue (VPS Hetzner).
 */
@Component
public class SpringAiSreAgent implements AgentNode {

    private static final Logger log = LoggerFactory.getLogger(SpringAiSreAgent.class);

    private final ChatClient chatClient;
    private final AgentContextReader agentContextReader;

    public SpringAiSreAgent(ChatClient.Builder chatClientBuilder, AgentContextReader agentContextReader) {
        this.agentContextReader = agentContextReader;
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        Eres el Ingeniero DevOps/SRE de una factoría de software autónoma.
                        Tu rol es empaquetar el proyecto (Docker), definir los pipelines de integración continua (CI/CD)
                        y los scripts de despliegue sobre proveedores en la nube como Hetzner.
                        
                        Tienes a tu disposición una herramienta para leer el contexto del proyecto (.agents/blueprint.md, roadmap.md, etc.) si necesitas consultar las decisiones del sistema, estándares de infraestructura o el estado actual del desarrollo.
                        """)
                .defaultTools(agentContextReader)
                .build();
    }

    @Override
    public String getRole() {
        return "SRE";
    }

    @Override
    public AgentTask execute(AgentTask task, ProjectContext context) {
        log.info("Agente DevOps/SRE ejecutando tarea: {}", task.getTitle());

        try {
            String response = chatClient.prompt()
                    .user("Genera un Dockerfile multi-etapa para compilar y ejecutar nuestra aplicación Java 21 y Spring Boot.")
                    .call()
                    .content();

            task.setOutputData(response);
            log.info("Script SRE/DevOps generado con éxito.");
        } catch (Exception e) {
            log.error("Error en agente SRE", e);
            task.setOutputData("Error al invocar LLM: " + e.getMessage());
        }

        return task;
    }
}
