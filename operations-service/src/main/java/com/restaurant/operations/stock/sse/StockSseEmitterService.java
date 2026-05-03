package com.restaurant.operations.stock.sse;

import com.restaurant.operations.stock.dto.ReplenishmentEventDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service that manages Server-Sent Events (SSE) connections for real-time
 * stock replenishment notifications.
 */
@Service
@Slf4j
public class StockSseEmitterService {

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * Registers a new SSE connection from a client.
     *
     * @return a new SseEmitter with no timeout
     */
    public SseEmitter register() {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        
        emitter.onCompletion(() -> {
            log.debug("SSE emitter completed, removing from registry");
            emitters.remove(emitter);
        });
        
        emitter.onTimeout(() -> {
            log.debug("SSE emitter timeout, removing from registry");
            emitters.remove(emitter);
        });
        
        emitter.onError(throwable -> {
            log.debug("SSE emitter error: {}, removing from registry", throwable.getMessage());
            emitters.remove(emitter);
        });
        
        emitters.add(emitter);
        log.debug("New SSE emitter registered. Total active emitters: {}", emitters.size());
        
        return emitter;
    }

    /**
     * Broadcasts a replenishment event to all connected clients.
     *
     * @param event the replenishment event containing count and item names
     */
    public void broadcast(ReplenishmentEventDto event) {
        if (emitters.isEmpty()) {
            log.debug("No active SSE connections to broadcast to");
            return;
        }

        log.info("Broadcasting replenishment event to {} connected clients", emitters.size());

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .data(event, MediaType.APPLICATION_JSON)
                        .build());
            } catch (IOException e) {
                log.debug("Failed to send SSE event, removing emitter: {}", e.getMessage());
                emitters.remove(emitter);
            }
        }
    }
}
