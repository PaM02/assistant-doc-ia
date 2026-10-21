package com.matdev.assistantdoc.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /** Pose une question sur le contenu des documents importés. */
    @PostMapping
    public ChatAnswer ask(@Valid @RequestBody ChatRequest request) {
        return chatService.ask(request.question());
    }

    public record ChatRequest(@NotBlank @Size(max = 1000) String question) {
    }
}
