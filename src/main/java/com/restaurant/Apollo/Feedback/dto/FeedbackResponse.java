package com.restaurant.Apollo.Feedback.dto;

import java.time.LocalDateTime;

public record FeedbackResponse(
        Long id,
        Long orderId,
        Long userId,
        String userEmail,
        String foodQualityRating,
        String serviceSpeedRating,
        boolean wouldRecommend,
        String comment,
        LocalDateTime createdAt
) {
}
