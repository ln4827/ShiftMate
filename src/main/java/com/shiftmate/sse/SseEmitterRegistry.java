package com.shiftmate.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class SseEmitterRegistry {

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(Long employeeId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.computeIfAbsent(employeeId, id -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> remove(employeeId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> remove(employeeId, emitter));

        log.debug("SSE emitter registered for employee id={}", employeeId);
        return emitter;
    }

    public void send(Long employeeId, String eventName, Object data) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(employeeId);
        if (list == null || list.isEmpty()) return;

        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        dead.forEach(e -> remove(employeeId, e));
    }

    private void remove(Long employeeId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(employeeId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) emitters.remove(employeeId);
        }
    }
}
