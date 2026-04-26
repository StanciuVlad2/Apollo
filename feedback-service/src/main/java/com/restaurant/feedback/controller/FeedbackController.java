package com.restaurant.feedback.controller;

import com.restaurant.feedback.dto.CreateFeedbackRequest;
import com.restaurant.feedback.dto.FeedbackResponse;
import com.restaurant.feedback.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping("/order/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FeedbackResponse> create(@PathVariable Long orderId,
                                                    @RequestBody @Valid CreateFeedbackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(feedbackService.create(orderId, request));
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FeedbackResponse> getByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(feedbackService.getByOrderId(orderId));
    }

    @GetMapping("/order/{orderId}/exists")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Boolean> existsByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(feedbackService.existsByOrderId(orderId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<List<FeedbackResponse>> getAll() {
        return ResponseEntity.ok(feedbackService.getAll());
    }
}
