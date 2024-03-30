package S10P22D204.stream.controller;

import S10P22D204.stream.service.AlarmService;
import S10P22D204.stream.dto.AlarmDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import org.springframework.http.codec.ServerSentEvent;

@RestController
@RequestMapping("/alarm")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AlarmDto>> subscribe(ServerWebExchange exchange) {
        String internalId = exchange.getRequest().getHeaders().getFirst("INTERNAL_ID_HEADER");
        return alarmService.subscribe(internalId)
                .map(data -> ServerSentEvent.<AlarmDto>builder()
                        .data(data)
                        .build());
    }
}
