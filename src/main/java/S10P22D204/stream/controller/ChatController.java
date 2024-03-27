package S10P22D204.stream.controller;

import S10P22D204.stream.entity.Chat;
import S10P22D204.stream.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/chat-history")
    public Flux<Chat> getMessages(@RequestParam("planId") Long planId,
                                  @RequestParam("before") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime before,
                                  @RequestParam("limit") int limit) {
        return chatService.findMessagesBefore(planId, before, limit);
    }
}