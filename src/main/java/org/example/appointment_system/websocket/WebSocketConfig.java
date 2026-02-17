package org.example.appointment_system.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket configuration for real-time notifications.
 *
 * <p>Enables STOMP protocol over WebSocket for bidirectional communication
 * between the server and clients.</p>
 *
 * <h3>Endpoint:</h3>
 * <ul>
 *   <li>/ws - WebSocket connection endpoint (with SockJS fallback)</li>
 * </ul>
 *
 * <h3>Message Destinations:</h3>
 * <ul>
 *   <li>/topic/merchant/{merchantId} - Merchant-specific notifications</li>
 *   <li>/topic/user/{userId} - User-specific notifications</li>
 *   <li>/topic/public - Public broadcast messages</li>
 * </ul>
 *
 * <h3>Security:</h3>
 * <p>WebSocket connections require authentication. The user's session is
 * validated during the handshake and STOMP connect phase.</p>
 */
@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple memory-based message broker
        // Messages destined for /topic are routed to the message broker
        // Messages destined for /queue are for user-specific messages
        config.enableSimpleBroker("/topic", "/queue");

        // Prefix for messages bound for @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");

        // Prefix for user-specific destinations
        config.setUserDestinationPrefix("/user");

        log.info("WebSocket message broker configured");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register STOMP endpoint at /ws with SockJS fallback
        // Use addInterceptors for handshake interceptor
        registry.addEndpoint("/ws")
            .addInterceptors(new AuthenticationHandshakeInterceptor())
            .setAllowedOriginPatterns("*");  // Allow all origins for development

        log.info("WebSocket STOMP endpoints registered at /ws");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Add interceptor to validate authentication for STOMP messages
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // Validate user is authenticated
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    if (auth == null || !auth.isAuthenticated()) {
                        log.warn("WebSocket connection rejected - not authenticated");
                        throw new IllegalArgumentException("Authentication required for WebSocket connection");
                    }
                    accessor.setUser(auth);
                    log.debug("WebSocket connection authenticated for user: {}", auth.getName());
                }

                return message;
            }
        });
    }

    /**
     * Handshake interceptor to capture authentication during WebSocket handshake.
     */
    private static class AuthenticationHandshakeInterceptor implements HandshakeInterceptor {

        @Override
        public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
        ) throws Exception {

            // Get authentication from Spring Security context
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && auth.isAuthenticated()) {
                attributes.put("user", auth.getPrincipal());
                attributes.put("username", auth.getName());
                log.debug("WebSocket handshake authenticated for user: {}", auth.getName());
                return true;
            }

            log.warn("WebSocket handshake rejected - no authentication");
            return false;
        }

        @Override
        public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
        ) {
            if (exception != null) {
                log.error("WebSocket handshake failed", exception);
            }
        }
    }
}
