package dev.jacques.contextengine.service;

import dev.jacques.contextengine.model.ChatRequest;
import dev.jacques.contextengine.model.ChatRequest.Message;
import dev.jacques.contextengine.model.ChatResponse;
import dev.jacques.contextengine.model.ChatResponse.TokenUsage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatLanguageModel inferenceModel;
    private final CompactorService compactorService;

    public ChatService(
            @Qualifier("inferenceModel") ChatLanguageModel inferenceModel,
            CompactorService compactorService
    ) {
        this.inferenceModel = inferenceModel;
        this.compactorService = compactorService;
    }

    public ChatResponse chat(ChatRequest request) {
        List<Message> history = request.history() != null ? request.history() : List.of();
        boolean compacted = false;
        String context;
        int compactorInputTokens = 0;
        int compactorOutputTokens = 0;

        if (compactorService.shouldCompact(history)) {
            compactorInputTokens = compactorService.estimateTokens(history);
            context = compactorService.compact(history);
            compactorOutputTokens = context.length() / 4;
            compacted = true;
            log.info("Context compacted: {} -> ~{} tokens", compactorInputTokens, compactorOutputTokens);
        } else {
            context = history.stream()
                    .map(m -> m.role() + ": " + m.content())
                    .collect(Collectors.joining("\n"));
        }

        String prompt = buildPrompt(context, request.message());
        int inputTokens = prompt.length() / 4;

        String answer = inferenceModel.chat(prompt);
        int outputTokens = answer.length() / 4;

        return new ChatResponse(
                answer,
                new TokenUsage(inputTokens, outputTokens, compactorInputTokens, compactorOutputTokens),
                compacted
        );
    }

    public ChatResponse chatRaw(ChatRequest request) {
        List<Message> history = request.history() != null ? request.history() : List.of();

        String context = history.stream()
                .map(m -> m.role() + ": " + m.content())
                .collect(Collectors.joining("\n"));

        String prompt = buildPrompt(context, request.message());
        int inputTokens = prompt.length() / 4;

        String answer = inferenceModel.chat(prompt);
        int outputTokens = answer.length() / 4;

        return new ChatResponse(
                answer,
                new TokenUsage(inputTokens, outputTokens, 0, 0),
                false
        );
    }

    private String buildPrompt(String context, String userMessage) {
        if (context.isBlank()) {
            return userMessage;
        }
        return """
                Conversation context:
                %s

                Current question: %s
                """.formatted(context, userMessage);
    }
}
