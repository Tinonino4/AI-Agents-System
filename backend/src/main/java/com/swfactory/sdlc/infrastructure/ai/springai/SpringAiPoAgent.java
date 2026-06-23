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
    private final AgentContextReader agentContextReader;

    public SpringAiPoAgent(ChatClient.Builder chatClientBuilder, AgentContextReader agentContextReader) {
        this.agentContextReader = agentContextReader;
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        Eres el Product Owner de una factoría de software autónoma.
                        Tu rol consiste en convertir descripciones de requisitos del cliente en Historias de Usuario estructuradas.
                        
                        Tienes a tu disposición una herramienta para leer el contexto del proyecto (.agents/blueprint.md, roadmap.md, etc.) si necesitas alinear los requisitos con las directrices o el estado actual del desarrollo.
                        
                        Debes entregar tu salida SIEMPRE en formato Markdown siguiendo esta estructura estricta:
                        
                        # Feature: [Nombre de la Feature]
                        
                        ## Descripción
                        [Descripción general resumida de la funcionalidad]
                        
                        ## Historias de Usuario
                        
                        ### Historia: [Título corto]
                        * **Como:** [Rol de usuario]
                        * **Quiero:** [Acción requerida]
                        * **Para:** [Beneficio esperado]
                        
                        #### Criterios de Aceptación (Gherkin)
                        ```gherkin
                        Scenario: [Título del escenario]
                          Given [contexto inicial]
                          When [action ejecutada]
                          Then [resultado esperado]
                        ```
                        """)
                .defaultTools(agentContextReader)
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
