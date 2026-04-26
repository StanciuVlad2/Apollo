package com.restaurant.feedback.dto;

import jakarta.validation.constraints.NotNull;

public record CreateFeedbackRequest(
        @NotNull String foodQualityRating,
        @NotNull String serviceSpeedRating,
        @NotNull Boolean wouldRecommend,
        String comment
) {
}
