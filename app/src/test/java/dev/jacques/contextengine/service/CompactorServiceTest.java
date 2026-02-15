package dev.jacques.contextengine.service;

import dev.jacques.contextengine.model.ChatRequest.Message;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompactorServiceTest {

    private CompactorService compactorService;
    private ChatLanguageModel mockModel;

    @BeforeEach
    void setUp() {
        mockModel = mock(ChatLanguageModel.class);
        compactorService = new CompactorService(mockModel, 2000);
    }

    @Test
    void shouldNotCompactShortHistory() {
        var history = List.of(new Message("user", "Hello"));
        assertFalse(compactorService.shouldCompact(history));
    }

    @Test
    void shouldCompactLongHistory() {
        var longMessage = "x".repeat(10_000);
        var history = List.of(new Message("user", longMessage));
        assertTrue(compactorService.shouldCompact(history));
    }

    @Test
    void compactReturnsModelSummary() {
        when(mockModel.chat(anyString())).thenReturn("Summary of conversation.");

        var history = List.of(
                new Message("user", "Tell me about Java"),
                new Message("assistant", "Java is a programming language...")
        );

        String result = compactorService.compact(history);
        assertEquals("Summary of conversation.", result);
    }
}
