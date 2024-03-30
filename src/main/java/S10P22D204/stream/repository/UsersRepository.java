package S10P22D204.stream.repository;

import S10P22D204.stream.entity.Users;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UsersRepository extends ReactiveCrudRepository<Users, Long> {
    Mono<Users> findByInternalId(String internalId);
}
