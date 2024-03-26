package S10P22D204.stream.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Setter
@Table("chat")
public class Chat {

    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("travel_id")
    private Long travelId;

    @Column("chat")
    private String chat;

    @Column("created_at")
    private LocalDateTime createdAt;
}
