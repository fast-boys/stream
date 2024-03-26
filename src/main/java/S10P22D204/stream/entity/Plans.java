package S10P22D204.stream.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@Table("plans")
public class Plans {

    @Id
    private Long id;

}
