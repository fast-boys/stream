package S10P22D204.stream.handler;

import S10P22D204.stream.common.exception.CustomException;
import S10P22D204.stream.common.exception.ExceptionType;
import S10P22D204.stream.dto.ChatUserDTO;
import S10P22D204.stream.repository.ChatRepository;
import S10P22D204.stream.repository.UserPlanRepository;
import S10P22D204.stream.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler implements WebSocketHandler {

    private final Map<Long, Map<String, WebSocketSession>> chatRooms = new ConcurrentHashMap<>();
    private final ChatRepository chatRepository;
    private final UsersRepository usersRepository;
    private final UserPlanRepository userPlanRepository;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        // Extract internalId from session headers
        Mono<String> internalIdMono = Mono.justOrEmpty(session.getHandshakeInfo().getHeaders().getFirst("internalId"));

        // Extract planId from query params
        Mono<Long> planIdMono = Mono.justOrEmpty(extractPlanId(session))
                .switchIfEmpty(Mono.error(new CustomException(ExceptionType.PLAN_ID_MISSING)));

        // Combine internalId and planId into a tuple and process
        return Mono.zip(internalIdMono, planIdMono, Tuples::of)
                .flatMap(tuple -> {
                    String internalId = tuple.getT1();
                    Long planId = tuple.getT2();

                    return usersRepository.findByInternalId(internalId) // Find user by internalId
                            .switchIfEmpty(Mono.error(new CustomException(ExceptionType.USER_NOT_FOUND)))
                            .onErrorResume(e -> Mono.error(new CustomException(ExceptionType.DATABASE_ERROR)))
                            .flatMap(user ->
                                    userPlanRepository.findByPlanIdAndUserId(planId, user.getId()) // Check if user belongs to the plan
                                            .switchIfEmpty(Mono.error(new CustomException(ExceptionType.NOT_VALID_USER))) // Raise error if no association found
                                            .onErrorResume(e -> Mono.error(new CustomException(ExceptionType.DATABASE_ERROR)))
                                            .then(Mono.just(Tuples.of(planId, user)))
                            );
                })
                .flatMap(tuple2 -> {
                    Long planId = tuple2.getT1();
                    // Logic to add user to chatRooms and send initial messages
                    chatRooms.computeIfAbsent(planId, k -> new ConcurrentHashMap<>()).put(session.getId(), session);
                    return getInitialMessages(planId).collectList()
                            .flatMap(messages -> session.send(Flux.fromIterable(messages)
                                    .map(this::serializeChatUserDtoToJson)
                                    .map(session::textMessage)))
                                    .onErrorResume(e -> Mono.error(new CustomException(ExceptionType.MESSAGE_SEND_FAILED)));
                })
                .onErrorResume(e -> handleException(session, e));
    }

    private Long extractPlanId(WebSocketSession session) {
        String query = session.getHandshakeInfo().getUri().getQuery();
        Map<String, String> queryParams = parseQueryParams(query);
        String planIdStr = queryParams.get("planId");
        return planIdStr != null ? Long.parseLong(planIdStr) : null;
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

    private Flux<ChatUserDTO> getInitialMessages(Long planId) {
        return chatRepository.findTop100ByPlanIdOrderByCreatedAtDesc(planId)
                .onErrorResume(e -> Mono.error(new CustomException(ExceptionType.DATABASE_ERROR)))
                .flatMap(chat -> usersRepository.findById(chat.getUserId())
                        .onErrorResume(e -> Mono.error(new CustomException(ExceptionType.DATABASE_ERROR)))
                        .map(user -> ChatUserDTO.builder()
                                .chatId(chat.getId())
                                .chatMessage(chat.getChat())
                                .createdAt(chat.getCreatedAt())
                                .userId(user.getId())
                                .nickname(user.getNickname())
                                .profileImage(user.getProfileImage())
                                .providerId(user.getProviderId())
                                .internalId(user.getInternalId())
                                .build()
                        )
                );
    }

    private String serializeChatUserDtoToJson(ChatUserDTO dto) {
        return String.format(
                "{\"chatId\": %d, \"message\": \"%s\", \"userId\": %d, \"nickname\": \"%s\", \"profileImage\": \"%s\", \"providerId\": \"%s\", \"internalId\": \"%s\"}",
                dto.getChatId(), dto.getChatMessage(), dto.getUserId(), dto.getNickname(), dto.getProfileImage(), dto.getProviderId(), dto.getInternalId()
        );
    }

    private Mono<Void> handleException(WebSocketSession session, Throwable e) {
        String errorMsg = "An error occurred";
        if (e instanceof CustomException) {
            errorMsg = e.getMessage();
        }
        return session.send(Mono.just(session.textMessage("{\"error\":\"" + errorMsg + "\"}")));
    }

}
