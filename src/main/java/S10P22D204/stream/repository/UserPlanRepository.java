package S10P22D204.stream.repository;

import S10P22D204.stream.entity.UserPlan;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserPlanRepository extends ReactiveCrudRepository<UserPlan, Long> {
    Mono<UserPlan> findByPlanIdAndUserId(Long PlanId, Long UserId);
}
