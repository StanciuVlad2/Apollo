package com.restaurant.Apollo.Feedback.controller;

import com.restaurant.Apollo.Feedback.dto.CreateFeedbackRequest;
import com.restaurant.Apollo.Feedback.dto.FeedbackResponse;
import com.restaurant.Apollo.Feedback.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping("/order/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FeedbackResponse> create(
            @PathVariable Long orderId,
            @Valid @RequestBody CreateFeedbackRequest request,
            Principal principal) {
        FeedbackResponse response = feedbackService.create(orderId, request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
