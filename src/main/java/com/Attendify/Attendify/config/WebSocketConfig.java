package com.Attendify.Attendify.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.Attendify.Attendify.WebSocketHandler.FaceRecognitionWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final FaceRecognitionWebSocketHandler faceRecognitionWebSocketHandler;

    @Autowired
    public WebSocketConfig(FaceRecognitionWebSocketHandler faceRecognitionWebSocketHandler) {
        this.faceRecognitionWebSocketHandler = faceRecognitionWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(faceRecognitionWebSocketHandler, "/ws/face").setAllowedOrigins("http://localhost:5500");
    }
}
