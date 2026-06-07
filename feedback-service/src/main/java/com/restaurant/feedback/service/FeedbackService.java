package com.restaurant.feedback.service;

import com.restaurant.feedback.dto.CreateFeedbackRequest;
import com.restaurant.feedback.dto.FeedbackResponse;
import com.restaurant.feedback.enums.FoodQualityRating;
import com.restaurant.feedback.enums.ServiceSpeedRating;
import com.restaurant.feedback.model.Feedback;
import com.restaurant.feedback.repository.CompletableOrderRepository;
import com.restaurant.feedback.repository.FeedbackRepository;
import com.restaurant.shared.security.UserHolder;
import com.restaurant.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private static final Set<String> STAFF_ROLES =
            Set.of("ROLE_WAITER", "ROLE_CHEF", "ROLE_MANAGER", "ROLE_ADMIN");

    private final FeedbackRepository feedbackRepository;
    private final CompletableOrderRepository completableOrderRepository;

    @Transactional
    public FeedbackResponse create(Long orderId, CreateFeedbackRequest request) {
        Long userId = UserHolder.getCurrentUser().userId();

        completableOrderRepository.findByOrderIdAndUserId(orderId, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Order not found, not completed, or does not belong to you"));

        if (feedbackRepository.existsByOrderId(orderId)) {
            throw new IllegalArgumentException("Feedback already submitted for this order");
        }

        Feedback feedback = Feedback.builder()
                .orderId(orderId)
                .userId(userId)
                .foodQualityRating(FoodQualityRating.valueOf(request.foodQualityRating()))
                .serviceSpeedRating(ServiceSpeedRating.valueOf(request.serviceSpeedRating()))
                .wouldRecommend(request.wouldRecommend())
                .comment(request.comment())
                .build();

        return toResponse(feedbackRepository.save(feedback));
    }

    public FeedbackResponse getByOrderId(Long orderId) {
        Feedback feedback = feedbackRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoSuchElementException("No feedback for order " + orderId));
        UserPrincipal current = UserHolder.getCurrentUser();
        if (!isStaff(current) && !current.userId().equals(feedback.getUserId())) {
            throw new AccessDeniedException("Access denied");
        }
        return toResponse(feedback);
    }

    public boolean existsByOrderId(Long orderId) {
        UserPrincipal current = UserHolder.getCurrentUser();
        if (isStaff(current)) {
            return feedbackRepository.existsByOrderId(orderId);
        }
        return feedbackRepository.existsByOrderIdAndUserId(orderId, current.userId());
    }

    public List<FeedbackResponse> getAll() {
        return feedbackRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse).toList();
    }

    private boolean isStaff(UserPrincipal user) {
        return user.roles().stream().anyMatch(STAFF_ROLES::contains);
    }

    private FeedbackResponse toResponse(Feedback f) {
        return new FeedbackResponse(
                f.getId(), f.getOrderId(), f.getUserId(),
                f.getFoodQualityRating().name(), f.getServiceSpeedRating().name(),
                f.isWouldRecommend(), f.getComment(), f.getCreatedAt()
        );
    }
}
