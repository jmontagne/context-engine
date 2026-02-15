package dev.jacques.contextengine.model;

public record ChatResponse(
        String answer,
        TokenUsage usage,
        boolean compacted
) {
    public record TokenUsage(
            int inputTokens,
            int outputTokens,
            int compactorInputTokens,
            int compactorOutputTokens
    ) {
        public int totalTokens() {
            return inputTokens + outputTokens + compactorInputTokens + compactorOutputTokens;
        }
    }
}
