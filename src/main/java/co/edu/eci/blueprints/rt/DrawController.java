package co.edu.eci.blueprints.rt;

import co.edu.eci.blueprints.model.Blueprint;
import co.edu.eci.blueprints.model.Point;
import co.edu.eci.blueprints.persistence.BlueprintNotFoundException;
import co.edu.eci.blueprints.services.BlueprintsServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.regex.Pattern;

@Controller
public class DrawController {

    private static final Logger log = LoggerFactory.getLogger(DrawController.class);
    private static final Pattern ID = Pattern.compile(Blueprint.ID_REGEX);

    private final BlueprintsServices services;
    private final BlueprintEventsPublisher events;

    public DrawController(BlueprintsServices services, BlueprintEventsPublisher events) {
        this.services = services;
        this.events = events;
    }

    /** Cliente -> /app/draw. Persiste el punto y lo difunde a todos los suscritos al plano (incluido el emisor). */
    @MessageMapping("/draw")
    public void onDraw(DrawEvent evt) {
        if (!isValid(evt)) {
            log.warn("Discarding invalid draw event: {}", evt);
            return;
        }
        Point p = evt.point();
        try {
            services.addPoint(evt.author(), evt.name(), p.x(), p.y());
        } catch (BlueprintNotFoundException e) {
            // El plano no existe (p.ej. no se ha guardado aun): se reenvia igual pero sin persistir
            log.debug("Draw on non-persisted blueprint {}/{}", evt.author(), evt.name());
        }
        events.publishPoints(evt.author(), evt.name(), List.of(p), evt.senderId(), evt.ts());
    }

    private static boolean isValid(DrawEvent evt) {
        return evt != null
                && isValidId(evt.author())
                && isValidId(evt.name())
                && evt.point() != null
                && Point.inRange(evt.point().x())
                && Point.inRange(evt.point().y());
    }

    private static boolean isValidId(String s) {
        return s != null && s.length() <= Blueprint.ID_MAX_LENGTH && ID.matcher(s).matches();
    }
}
