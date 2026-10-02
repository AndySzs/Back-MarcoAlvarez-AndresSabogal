package co.edu.eci.blueprints.rt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.AbstractSubProtocolEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Observabilidad: registra conexiones, desconexiones y suscripciones STOMP. */
@Component
public class WebSocketEventsLogger {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventsLogger.class);

    // Set en vez de contador: Spring puede emitir SessionDisconnectEvent mas de una vez por sesion
    private final Set<String> activeSessions = ConcurrentHashMap.newKeySet();

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        String sessionId = sessionId(event);
        activeSessions.add(sessionId);
        log.info("WS connected: session={} active={}", sessionId, activeSessions.size());
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        activeSessions.remove(event.getSessionId());
        log.info("WS disconnected: session={} status={} active={}",
                event.getSessionId(), event.getCloseStatus(), activeSessions.size());
    }

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor acc = StompHeaderAccessor.wrap(event.getMessage());
        log.info("WS subscribe: session={} destination={} active={}",
                acc.getSessionId(), acc.getDestination(), activeSessions.size());
    }

    @EventListener
    public void onUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor acc = StompHeaderAccessor.wrap(event.getMessage());
        log.info("WS unsubscribe: session={} subscription={} active={}",
                acc.getSessionId(), acc.getSubscriptionId(), activeSessions.size());
    }

    public int getActiveSessions() {
        return activeSessions.size();
    }

    private static String sessionId(AbstractSubProtocolEvent event) {
        return StompHeaderAccessor.wrap(event.getMessage()).getSessionId();
    }
}
