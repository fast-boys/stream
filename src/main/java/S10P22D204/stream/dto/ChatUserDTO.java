package S10P22D204.stream.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ChatUserDTO {
    private Long chatId;
    private String chatMessage;
    private LocalDateTime createdAt;

    private Long userId;
    private String nickname;
    private String profileImage;
    private String providerId;
    private String internalId;
}

