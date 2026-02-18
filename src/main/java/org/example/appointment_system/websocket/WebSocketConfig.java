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
 * 用于实时通知的WebSocket配置。
 *
 * <p>启用STOMP协议通过WebSocket实现服务器与客户端之间的双向通信。</p>
 *
 * <h3>接口：</h3>
 * <ul>
 *   <li>/ws - WebSocket连接端点（带SockJS回退）</li>
 * </ul>
 *
 * <h3>消息目的地：</h3>
 * <ul>
 *   <li>/topic/merchant/{merchantId} - 商家特定通知</li>
 *   <li>/topic/user/{userId} - 用户特定通知</li>
 *   <li>/topic/public - 公共广播消息</li>
 * </ul>
 *
 * <h3>安全性：</h3>
 * <p>WebSocket连接需要认证。用户的会话在握手和STOMP连接阶段进行验证。</p>
 */
@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单的基于内存的消息代理
        // 目标是/topic的消息路由到消息代理
        // 目标是/queue的消息用于用户特定消息
        config.enableSimpleBroker("/topic", "/queue");

        // 目标为@MessageMapping方法的消息前缀
        config.setApplicationDestinationPrefixes("/app");

        // 用户特定目的地的前缀
        config.setUserDestinationPrefix("/user");

        log.info("WebSocket消息代理已配置");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 在/ws注册STOMP端点，带SockJS回退
        // 使用addInterceptors添加握手拦截器
        registry.addEndpoint("/ws")
            .addInterceptors(new AuthenticationHandshakeInterceptor())
            .setAllowedOriginPatterns("*");  // 开发环境允许所有源

        log.info("WebSocket STOMP端点已在/ws注册");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 添加拦截器验证STOMP消息的认证
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // 验证用户已认证
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
     * 握手拦截器，用于在WebSocket握手期间捕获认证信息。
     */
    private static class AuthenticationHandshakeInterceptor implements HandshakeInterceptor {

        @Override
        public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
        ) throws Exception {

            // 从Spring Security上下文获取认证信息
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
