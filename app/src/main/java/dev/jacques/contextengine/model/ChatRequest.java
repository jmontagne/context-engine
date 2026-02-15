package dev.jacques.contextengine.model;

import java.util.List;

public record ChatRequest(
        String message,
        List<Message> history
) {
    public record Message(String role, String content) {}
}
