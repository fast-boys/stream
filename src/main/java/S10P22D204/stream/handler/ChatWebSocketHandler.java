package S10P22D204.stream.handler;

import S10P22D204.stream.common.exception.CustomException;
import S10P22D204.stream.common.exception.ExceptionType;
import S10P22D204.stream.dto.ChatUserDTO;
import S10P22D204.stream.entity.Chat;
import S10P22D204.stream.entity.Users;
import S10P22D204.stream.repository.ChatRepository;
import S10P22D204.stream.repository.UserPlanRepository;
import S10P22D204.stream.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler implements WebSocketHandler {

    private final Map<Long, Map<String, WebSocketSession>> chatRooms = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionPlanMap = new ConcurrentHashMap<>();
    private final ChatRepository chatRepository;
    private final UsersRepository usersRepository;
    private final UserPlanRepository userPlanRepository;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        Mono<String> internalIdMono = Mono.justOrEmpty(session.getHandshakeInfo().getHeaders().getFirst("internalId"));
        Mono<Long> planIdMono = Mono.justOrEmpty(extractPlanId(session))
                .switchIfEmpty(Mono.error(new CustomException(ExceptionType.PLAN_ID_MISSING)));

        Mono<Users> userMono = Mono.zip(internalIdMono, planIdMono)
                .flatMap(tuple -> {
                    String internalId = tuple.getT1();
                    Long planId = tuple.getT2();

                    return usersRepository.findByInternalId(internalId)
                            .switchIfEmpty(Mono.error(new CustomException(ExceptionType.USER_NOT_FOUND)))
                            .flatMap(user ->
                                    userPlanRepository.findByPlanIdAndUserId(planId, user.getId())
                                            .switchIfEmpty(Mono.error(new CustomException(ExceptionType.NOT_VALID_USER)))
                                            .thenReturn(user)
                            );
                })
                .cache();

        Mono<Void> initialSetup = userMono.flatMap(user -> planIdMono.flatMap(planId -> {
            chatRooms.computeIfAbsent(planId, k -> new ConcurrentHashMap<>()).put(session.getId(), session);
            sessionPlanMap.put(session.getId(), planId);
            return getInitialMessages(planId).collectList()
                    .flatMap(messages -> session.send(Flux.fromIterable(messages)
                            .map(this::serializeChatUserDtoToJson)
                            .map(session::textMessage)));
        })).onErrorResume(e -> handleException(session, e));

        Mono<Void> messageHandling = session.receive()
                .flatMap(message -> {
                    if (message.getType() == WebSocketMessage.Type.TEXT) {
                        String payload = message.getPayloadAsText();
                        return Mono.zip(planIdMono, userMono)
                                .flatMap(tuple -> {
                                    Long planId = tuple.getT1();
                                    Users user = tuple.getT2();
                                    return broadcastMessage(payload, planId, user);
                                });
                    } else if (message.getType() == WebSocketMessage.Type.BINARY) {
                        System.out.println("File received. Service not ready.");
                        return Mono.empty();
//                        return Mono.fromCallable(() -> Files.createTempFile("upload-", ".bin"))
//                                .flatMap(path -> DataBufferUtils.write(Flux.just(message.getPayload()), path, StandardOpenOption.CREATE)
//                                        .then(Mono.fromRunnable(() -> System.out.println("File saved: " + path))))
//                                .subscribeOn(Schedulers.boundedElastic());
                    }
                    return Mono.empty();
                }).then();

        return initialSetup
                .then(messageHandling)
                .doFinally(signalType -> removeSessionFromRooms(session));
    }

    private void removeSessionFromRooms(WebSocketSession session) {
        Long planId = sessionPlanMap.get(session.getId());
        if (planId != null) {
            Map<String, WebSocketSession> sessions = chatRooms.get(planId);
            if (sessions != null) {
                sessions.remove(session.getId());
                if (sessions.isEmpty()) {
                    chatRooms.remove(planId);
                }
            }
            sessionPlanMap.remove(session.getId());
        }
    }

    private Mono<Void> broadcastMessage(String message, Long planId, Users user) {
        Chat chatMessage = new Chat();
        chatMessage.setUserId(user.getId());
        chatMessage.setPlanId(planId);
        chatMessage.setChat(message);
        chatMessage.setCreatedAt(LocalDateTime.now());

        Mono<Void> saveToDb = chatRepository.save(chatMessage).then();

        ChatUserDTO chatUserDTO = ChatUserDTO.builder()
                .content(message)
                .timestamp(chatMessage.getCreatedAt())
                .userId(user.getId())
                .sender(user.getNickname())
                .profileImage(user.getProfileImage())
                .build();

        Mono<Void> broadcast = Mono.justOrEmpty(chatRooms.get(planId))
                .flatMapMany(sessions -> Flux.fromIterable(sessions.values()))
                .flatMap(session ->
                        session.send(Mono.just(session.textMessage(serializeChatUserDtoToJson(chatUserDTO))))
                                .onErrorResume(e -> {
                                    System.out.println("Error sending message to session " + session.getId() + ": " + e.getMessage());
                                    return Mono.empty();
                                })
                ).then();

        return saveToDb.then(broadcast);
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
                                .id(chat.getId())
                                .content(chat.getChat())
                                .timestamp(chat.getCreatedAt())
                                .userId(user.getId())
                                .sender(user.getNickname())
                                .profileImage(user.getProfileImage())
                                .build()
                        ))
                .onErrorResume(e -> {
                    if (e instanceof DataAccessException) {
                        return Mono.error(new CustomException(ExceptionType.DATABASE_ERROR));
                    }
                    return Mono.error(e);
                });
    }

    private String serializeChatUserDtoToJson(ChatUserDTO dto) {
        return String.format(
                "{\"id\": %d, \"content\": \"%s\", \"userId\": %d, \"sender\": \"%s\", \"profileImage\": \"%s\"}",
                dto.getId(), dto.getContent(), dto.getUserId(), dto.getSender(), dto.getProfileImage()
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
