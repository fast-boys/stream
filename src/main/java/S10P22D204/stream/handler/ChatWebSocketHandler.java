package S10P22D204.stream.handler;

import S10P22D204.stream.dto.ChatUserDTO;
import S10P22D204.stream.repository.ChatRepository;
import S10P22D204.stream.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler implements WebSocketHandler {

    private final Map<Long, Map<String, WebSocketSession>> chatRooms = new ConcurrentHashMap<>();
    private final ChatRepository chatRepository;
    private final UsersRepository usersRepository;

    private Long extractTravelId(WebSocketSession session) {
        String query = session.getHandshakeInfo().getUri().getQuery();
        Map<String, String> queryParams = parseQueryParams(query);
        String travelIdStr = queryParams.get("travelId");
        return travelIdStr != null ? Long.parseLong(travelIdStr) : null;
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> queryParams = new ConcurrentHashMap<>();
        if (query != null && !query.isEmpty()) {
            for (String param : query.split("&")) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    queryParams.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return queryParams;
    }

    private Flux<ChatUserDTO> getInitialMessages(Long travelId) {
        return chatRepository.findTop100ByTravelIdOrderByCreatedAtDesc(travelId)
                .flatMap(chat -> usersRepository.findById(chat.getUserId())
                        .map(user -> ChatUserDTO.builder()
                                .chatId(chat.getId())
                                .chatMessage(chat.getChat())
                                .createdAt(chat.getCreatedAt())
                                .userId(user.getId())
                                .nickname(user.getNickname())
                                .profileImage(user.getProfileImage())
                                .build()
                        )
                );
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        Long travelId = extractTravelId(session);
        chatRooms.computeIfAbsent(travelId, k -> new ConcurrentHashMap<>()).put(session.getId(), session);

        Flux<ChatUserDTO> initialMessages = getInitialMessages(travelId);

        return initialMessages.collectList().flatMap(list -> session.send(Flux.fromIterable(list)
                .map(dto -> session.textMessage(serializeChatUserDtoToJson(dto)))
        ));
    }

    private String serializeChatUserDtoToJson(ChatUserDTO dto) {
        return String.format("{\"chatId\": %d, \"message\": \"%s\", \"userId\": %d, \"nickname\": \"%s\", \"profileImage\": \"%s\"}",
                dto.getChatId(), dto.getChatMessage(), dto.getUserId(), dto.getNickname(), dto.getProfileImage());
    }
}