package S10P22D204.stream.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ChatUserDTO {

    // From Chat
    private Long chatId;
    private String chatMessage;
    private LocalDateTime createdAt;

    // From Users
    private Long userId;
    private String nickname;
    private String profileImage;
}
