package dev.jacques.contextengine.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.vertexai.VertexAiGeminiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VertexAiConfig {

    @Value("${vertex-ai.project-id}")
    private String projectId;

    @Value("${vertex-ai.location}")
    private String location;

    @Bean
    ChatLanguageModel inferenceModel() {
        return VertexAiGeminiChatModel.builder()
                .project(projectId)
                .location(location)
                .modelName("gemini-2.5-pro")
                .temperature(0.3f)
                .maxOutputTokens(2048)
                .build();
    }

    @Bean
    ChatLanguageModel compactorModel() {
        return VertexAiGeminiChatModel.builder()
                .project(projectId)
                .location(location)
                .modelName("gemini-2.0-flash")
                .temperature(0.1f)
                .maxOutputTokens(1024)
                .build();
    }
}
