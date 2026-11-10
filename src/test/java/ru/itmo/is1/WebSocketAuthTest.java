package ru.itmo.is1;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import ru.itmo.is1.dto.auth.RegisterRequest;
import ru.itmo.is1.service.AuthService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketAuthTest extends EmbeddedDatabaseTest {
    @LocalServerPort
    int port;

    @Autowired
    AuthService authService;

    @Test
    void connectRequiresValidToken() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("ws_user");
        request.setPassword("secret123");
        String token = authService.register(request).getAccessToken();

        StompSession session = connect("Bearer " + token).get(5, TimeUnit.SECONDS);
        assertTrue(session.isConnected());
        session.disconnect();

        CompletableFuture<StompSession> anonymous = connect(null);
        assertThrows(Exception.class, () -> anonymous.get(3, TimeUnit.SECONDS));

        CompletableFuture<StompSession> forged = connect("Bearer forged.token.value");
        assertThrows(Exception.class, () -> forged.get(3, TimeUnit.SECONDS));
    }

    private CompletableFuture<StompSession> connect(String authorization) {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new MappingJackson2MessageConverter());
        StompHeaders connectHeaders = new StompHeaders();
        if (authorization != null) {
            connectHeaders.add(HttpHeaders.AUTHORIZATION, authorization);
        }
        return client.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(),
                connectHeaders, new StompSessionHandlerAdapter() {
                });
    }
}
