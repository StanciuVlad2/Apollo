package com.restaurant.feedback.dto;

import java.time.LocalDateTime;

public record FeedbackResponse(
        Long id,
        Long orderId,
        Long userId,
        String foodQualityRating,
        String serviceSpeedRating,
        boolean wouldRecommend,
        String comment,
        LocalDateTime createdAt
) {
}
