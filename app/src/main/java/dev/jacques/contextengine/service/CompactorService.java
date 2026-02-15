package dev.jacques.contextengine.service;

import dev.jacques.contextengine.model.ChatRequest.Message;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CompactorService {

    private static final Logger log = LoggerFactory.getLogger(CompactorService.class);

    private final ChatLanguageModel compactorModel;
    private final int tokenThreshold;

    public CompactorService(
            @Qualifier("compactorModel") ChatLanguageModel compactorModel,
            @Value("${compactor.token-threshold:2000}") int tokenThreshold
    ) {
        this.compactorModel = compactorModel;
        this.tokenThreshold = tokenThreshold;
    }

    public boolean shouldCompact(List<Message> history) {
        int estimatedTokens = estimateTokens(history);
        log.info("Estimated history tokens: {} (threshold: {})", estimatedTokens, tokenThreshold);
        return estimatedTokens > tokenThreshold;
    }

    public String compact(List<Message> history) {
        String historyText = history.stream()
                .map(m -> m.role() + ": " + m.content())
                .collect(Collectors.joining("\n"));

        String prompt = """
                Summarize the following conversation history into a concise context summary.
                Preserve: key facts, decisions, user preferences, and any unresolved questions.
                Discard: greetings, filler, repetition, and pleasantries.
                Output only the summary, no preamble.

                ---
                %s
                ---
                """.formatted(historyText);

        log.info("Compacting {} messages ({} estimated tokens)", history.size(), estimateTokens(history));
        String summary = compactorModel.chat(prompt);
        log.info("Compacted to ~{} estimated tokens", estimateTokenCount(summary));
        return summary;
    }

    public int estimateTokens(List<Message> history) {
        return history.stream()
                .mapToInt(m -> estimateTokenCount(m.content()))
                .sum();
    }

    private int estimateTokenCount(String text) {
        // ~4 chars per token is a reasonable approximation for English
        return text.length() / 4;
    }
}
