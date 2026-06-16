package com.swfactory.sdlc.infrastructure.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Clase de Configuración de Infraestructura.
 * Resuelve la ambigüedad al tener tanto el Starter de OpenAI (LM Studio)
 * como el de Google GenAI (Gemini) en el classpath, exponiendo un bean @Primary ChatModel.
 */
@Configuration
public class AiConfig {

    private final ChatModel googleGenAiChatModel;
    private final ChatModel openAiChatModel;
    private final String provider;

    public AiConfig(
            @Qualifier("googleGenAiChatModel") ChatModel googleGenAiChatModel,
            @Qualifier("openAiChatModel") ChatModel openAiChatModel,
            @Value("${app.ai.provider:gemini}") String provider) {
        this.googleGenAiChatModel = googleGenAiChatModel;
        this.openAiChatModel = openAiChatModel;
        this.provider = provider;
    }

    /**
     * Define el ChatModel principal que consumirá el constructor de ChatClient.Builder.
     */
    @Bean
    @Primary
    public ChatModel primaryChatModel() {
        if ("lmstudio".equalsIgnoreCase(provider) || "openai".equalsIgnoreCase(provider)) {
            return openAiChatModel;
        }
        return googleGenAiChatModel;
    }
}
