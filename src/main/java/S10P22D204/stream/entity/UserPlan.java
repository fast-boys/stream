package S10P22D204.stream.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@Table("user_plan")
public class UserPlan {

    @Column("user_id")
    private Long userId;

    @Column("plan_id")
    private Long planId;
}
