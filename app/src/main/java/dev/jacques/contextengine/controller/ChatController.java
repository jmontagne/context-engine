package dev.jacques.contextengine.controller;

import dev.jacques.contextengine.model.ChatRequest;
import dev.jacques.contextengine.model.ChatResponse;
import dev.jacques.contextengine.service.ChatService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "context-engine");
    }

    /**
     * Chat with context compaction — Gemini Flash summarizes history, Gemini Pro answers.
     */
    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return chatService.chat(request);
    }

    /**
     * Chat without compaction — full history sent to Gemini Pro. For A/B comparison.
     */
    @PostMapping("/chat-raw")
    public ChatResponse chatRaw(@RequestBody ChatRequest request) {
        return chatService.chatRaw(request);
    }
}
