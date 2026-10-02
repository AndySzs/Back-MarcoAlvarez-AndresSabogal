package co.edu.eci.blueprints.rt;

import co.edu.eci.blueprints.model.Point;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StompIntegrationTest {

    @LocalServerPort
    private int port;

    private WebSocketStompClient stompClient;
    private final List<StompSession> sessions = new ArrayList<>();

    @BeforeEach
    void setUp() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @AfterEach
    void tearDown() {
        sessions.forEach(s -> {
            if (s.isConnected()) s.disconnect();
        });
        stompClient.stop();
    }

    @Test
    void drawIsBroadcastToAllSubscribersOfTheBlueprintOnly() throws Exception {
        StompSession alice = connect();
        StompSession bob = connect();
        StompSession carol = connect();

        BlockingQueue<BlueprintUpdate> aliceInbox = subscribe(alice, "/topic/blueprints.juan.plano-1");
        BlockingQueue<BlueprintUpdate> bobInbox = subscribe(bob, "/topic/blueprints.juan.plano-1");
        BlockingQueue<BlueprintUpdate> carolInbox = subscribe(carol, "/topic/blueprints.juan.plano-2");

        // SUBSCRIBE es asincrono: damos tiempo a que el broker registre las suscripciones
        Thread.sleep(500);

        alice.send("/app/draw", new DrawEvent("juan", "plano-1", new Point(123, 45), "alice-tab", 42L));

        BlueprintUpdate toAlice = aliceInbox.poll(5, TimeUnit.SECONDS);
        BlueprintUpdate toBob = bobInbox.poll(5, TimeUnit.SECONDS);

        assertNotNull(toAlice, "the sender also receives the echo");
        assertNotNull(toBob, "another subscriber of the same blueprint receives the point");
        for (BlueprintUpdate upd : List.of(toAlice, toBob)) {
            assertEquals("points", upd.type());
            assertEquals("juan", upd.author());
            assertEquals("plano-1", upd.name());
            assertEquals(List.of(new Point(123, 45)), upd.points());
            assertEquals("alice-tab", upd.senderId());
            assertEquals(42L, upd.ts());
        }

        assertNull(carolInbox.poll(1, TimeUnit.SECONDS), "subscribers of another blueprint must not receive it");
    }

    @Test
    void invalidDrawIsDiscarded() throws Exception {
        StompSession s = connect();
        BlockingQueue<BlueprintUpdate> inbox = subscribe(s, "/topic/blueprints.juan.plano-2");
        Thread.sleep(500);

        s.send("/app/draw", new DrawEvent("juan", "plano-2", new Point(50000, 1), null, null));
        s.send("/app/draw", new DrawEvent("juan", "plano-2", null, null, null));

        assertNull(inbox.poll(1, TimeUnit.SECONDS));
    }

    @Test
    void restChangesArePublishedToTheBlueprintTopic() throws Exception {
        StompSession s = connect();
        BlockingQueue<BlueprintUpdate> inbox = subscribe(s, "/topic/blueprints.john.house");
        Thread.sleep(500);

        RestTemplate rest = new RestTemplate();
        String url = "http://localhost:" + port + "/api/v1/blueprints/john/house";
        HttpHeaders json = new HttpHeaders();
        json.setContentType(MediaType.APPLICATION_JSON);

        rest.put(url, new HttpEntity<>("{\"points\":[{\"x\":1,\"y\":1},{\"x\":2,\"y\":2}]}", json));
        BlueprintUpdate replace = inbox.poll(5, TimeUnit.SECONDS);
        assertNotNull(replace);
        assertEquals("replace", replace.type());
        assertEquals(List.of(new Point(1, 1), new Point(2, 2)), replace.points());

        rest.put(url + "/points", new HttpEntity<>("{\"x\":3,\"y\":3}", json));
        BlueprintUpdate added = inbox.poll(5, TimeUnit.SECONDS);
        assertNotNull(added);
        assertEquals("points", added.type());
        assertEquals(List.of(new Point(3, 3)), added.points());

        rest.delete(url);
        BlueprintUpdate deleted = inbox.poll(5, TimeUnit.SECONDS);
        assertNotNull(deleted);
        assertEquals("deleted", deleted.type());
        assertEquals(List.of(), deleted.points());
    }

    private StompSession connect() throws Exception {
        StompSession session = stompClient
                .connectAsync("ws://localhost:" + port + "/ws-blueprints", new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);
        sessions.add(session);
        return session;
    }

    private static BlockingQueue<BlueprintUpdate> subscribe(StompSession session, String destination) {
        BlockingQueue<BlueprintUpdate> inbox = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return BlueprintUpdate.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                inbox.offer((BlueprintUpdate) payload);
            }
        });
        return inbox;
    }
}
