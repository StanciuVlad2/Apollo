package com.restaurant.operations.stock.controller;

import com.restaurant.operations.stock.sse.StockSseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * REST controller for Server-Sent Events (SSE) stock replenishment notifications.
 * 
 * Note: The /api/stock/events endpoint is permitted without authentication
 * to simplify EventSource setup on the frontend. The Stock page itself is
 * protected by ProtectedRoute which requires MANAGER or ADMIN role.
 */
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@Slf4j
public class StockSseController {

    private final StockSseEmitterService sseEmitterService;

    /**
     * Establishes an SSE connection for real-time stock replenishment notifications.
     *
     * @return SseEmitter for streaming events
     */
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        log.info("New SSE client connected for stock events");
        return sseEmitterService.register();
    }
}
