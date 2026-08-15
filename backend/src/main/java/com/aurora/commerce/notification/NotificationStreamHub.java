package com.aurora.commerce.notification;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
class NotificationStreamHub {

    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        CopyOnWriteArrayList<SseEmitter> userEmitters = emitters.computeIfAbsent(
                userId, ignored -> new CopyOnWriteArrayList<>());
        userEmitters.add(emitter);
        Runnable cleanup = () -> remove(userId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ignored -> cleanup.run());
        try {
            emitter.send(SseEmitter.event().name("connected").data("ready"));
        } catch (IOException exception) {
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    void publish(Long userId, NotificationFacade.NotificationView notification) {
        List<SseEmitter> userEmitters = emitters.getOrDefault(userId, new CopyOnWriteArrayList<>());
        userEmitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("notification").data(notification));
            } catch (IOException exception) {
                emitter.complete();
                remove(userId, emitter);
            }
        });
    }

    private void remove(Long userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null) return;
        userEmitters.remove(emitter);
        if (userEmitters.isEmpty()) emitters.remove(userId, userEmitters);
    }
}
