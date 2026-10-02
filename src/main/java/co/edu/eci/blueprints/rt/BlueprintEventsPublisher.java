package co.edu.eci.blueprints.rt;

import co.edu.eci.blueprints.model.Point;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/** Publica los cambios de un plano en su propio topico: /topic/blueprints.{author}.{name} */
@Component
public class BlueprintEventsPublisher {

    public static final String TOPIC_PREFIX = "/topic/blueprints.";

    private final SimpMessagingTemplate template;

    public BlueprintEventsPublisher(SimpMessagingTemplate template) {
        this.template = template;
    }

    public static String topic(String author, String name) {
        return TOPIC_PREFIX + author + "." + name;
    }

    public void publishPoints(String author, String name, List<Point> points, String senderId, Long ts) {
        publish(new BlueprintUpdate(BlueprintUpdate.POINTS, author, name, points, senderId, ts));
    }

    public void publishReplace(String author, String name, List<Point> points) {
        publish(new BlueprintUpdate(BlueprintUpdate.REPLACE, author, name, points, null, System.currentTimeMillis()));
    }

    public void publishDeleted(String author, String name) {
        publish(new BlueprintUpdate(BlueprintUpdate.DELETED, author, name, List.of(), null, System.currentTimeMillis()));
    }

    private void publish(BlueprintUpdate update) {
        template.convertAndSend(topic(update.author(), update.name()), update);
    }
}
