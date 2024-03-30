package S10P22D204.stream.service;

import S10P22D204.stream.dto.AlarmDto;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AlarmService {

    private final Map<String, Sinks.Many<AlarmDto>> userSinks = new ConcurrentHashMap<>();

    public Flux<AlarmDto> subscribe(String internalId) {
        Sinks.Many<AlarmDto> sink = Sinks.many().multicast().onBackpressureBuffer();
        userSinks.putIfAbsent(internalId, sink);
        return sink.asFlux();
    }

    public void notifyUser(String internalId) {
        Sinks.Many<AlarmDto> sink = userSinks.get(internalId);
        if (sink != null) {
            AlarmDto alarmDto = AlarmDto.builder()
                    .status("success")
                    .build();
            sink.tryEmitNext(alarmDto);
        }
    }
}
