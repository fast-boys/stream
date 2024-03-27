package S10P22D204.stream.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ChatUserDTO {
    private Long id;
    private String content;
    private LocalDateTime timestamp;

    private Long userId;
    private String sender;
    private String profileImage;
    private String type;
}

