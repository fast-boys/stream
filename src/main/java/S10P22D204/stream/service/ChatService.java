package S10P22D204.stream.service;

import S10P22D204.stream.entity.Chat;
import S10P22D204.stream.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;

    public Flux<Chat> findMessagesBefore(Long planId, LocalDateTime before, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return chatRepository.findByPlanIdAndCreatedAtBefore(planId, before, pageable);
    }
}
