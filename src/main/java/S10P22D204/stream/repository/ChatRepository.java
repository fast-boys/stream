package S10P22D204.stream.repository;

import S10P22D204.stream.entity.Chat;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ChatRepository extends ReactiveCrudRepository<Chat, Long> {
    Flux<Chat> findTop100ByPlanIdOrderByCreatedAtDesc(Long travelId);
}
