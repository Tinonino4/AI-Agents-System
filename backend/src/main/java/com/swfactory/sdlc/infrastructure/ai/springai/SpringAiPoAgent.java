package com.swfactory.sdlc.infrastructure.ai.springai;

import com.swfactory.sdlc.domain.agent.AgentNode;
import com.swfactory.sdlc.domain.model.AgentTask;
import com.swfactory.sdlc.domain.model.ProjectContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Adaptador de Infraestructura. Implementa el agente Product Owner (PO)
 * para refinar ideas en especificaciones Gherkin estructuradas.
 */
@Component
public class SpringAiPoAgent implements AgentNode {

    private static final Logger log = LoggerFactory.getLogger(SpringAiPoAgent.class);

    private final ChatClient chatClient;

    public SpringAiPoAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        Eres el Product Owner de una factoría de software autónoma.
                        Tu rol consiste en convertir descripciones vagas de requisitos del cliente
                        en Historias de Usuario estructuradas con formato Gherkin (Given-When-Then).
                        """)
                .build();
    }

    @Override
    public String getRole() {
        return "PRODUCT_OWNER";
    }

    @Override
    public AgentTask execute(AgentTask task, ProjectContext context) {
        log.info("Agente Product Owner ejecutando tarea: {}", task.getTitle());
        String requirements = context.getDescription();

        try {
            String response = chatClient.prompt()
                    .user("Traduce estos requisitos a Historias de Usuario Gherkin:\n" + requirements)
                    .call()
                    .content();

            task.setOutputData(response);
            log.info("Historias de usuario generadas con éxito por el PO. {}", response);
        } catch (Exception e) {
            log.error("Error en el agente Product Owner", e);
            task.setOutputData("Error al invocar LLM: " + e.getMessage());
        }

        return task;
    }
}
