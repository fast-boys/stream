package S10P22D204.stream.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@Table("users")
public class Users {

    @Id
    private Long id;

    @Column("provider_id")
    private String providerId;

    @Column("internal_id")
    private String internalId;

    @Column("nickname")
    private String nickname;

    @Column("profile_image")
    private String profileImage;
}