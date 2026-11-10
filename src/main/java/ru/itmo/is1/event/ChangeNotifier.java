package ru.itmo.is1.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChangeNotifier {
    public static final String TOPIC = "/topic/changes";

    private final ApplicationEventPublisher publisher;
    private final SimpMessagingTemplate messagingTemplate;

    public void notify(ChangeEvent.EntityType entity, ChangeEvent.Action action, Integer id) {
        publisher.publishEvent(new ChangeEvent(entity, action, id));
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void broadcast(ChangeEvent event) {
        log.atInfo()
                .setMessage("Data changed")
                .addKeyValue("event", "data_changed")
                .addKeyValue("entity", event.getEntity())
                .addKeyValue("action", event.getAction())
                .addKeyValue("entityId", event.getId())
                .log();
        messagingTemplate.convertAndSend(TOPIC, event);
    }
}
