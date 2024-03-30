package S10P22D204.stream.common.rabbitmq;

import S10P22D204.stream.service.AlarmService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MessageListener {

    @Autowired
    private AlarmService alarmService;

    @RabbitListener(queues = "#{stream}")
    public void receiveMessage(String internalId) {
        alarmService.notifyUser(internalId);
    }
}
