package com.restaurant.Apollo.Feedback.service;

import com.restaurant.Apollo.Feedback.dto.CreateFeedbackRequest;
import com.restaurant.Apollo.Feedback.dto.FeedbackResponse;
import com.restaurant.Apollo.Feedback.enums.FoodQualityRating;
import com.restaurant.Apollo.Feedback.enums.ServiceSpeedRating;
import com.restaurant.Apollo.Feedback.model.Feedback;
import com.restaurant.Apollo.Feedback.repository.FeedbackRepository;
import com.restaurant.Apollo.Orders.model.Order;
import com.restaurant.Apollo.Orders.enums.OrderStatus;
import com.restaurant.Apollo.Orders.repository.OrderRepository;
import com.restaurant.Apollo.UserManagement.model.User;
import com.restaurant.Apollo.UserManagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Transactional
    public FeedbackResponse create(Long orderId, CreateFeedbackRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));

        if (!order.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("You can only leave feedback on your own orders");
        }

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new IllegalArgumentException("Feedback can only be left on completed orders");
        }

        if (feedbackRepository.existsByOrderId(orderId)) {
            throw new IllegalArgumentException("Feedback has already been submitted for this order");
        }

        FoodQualityRating foodQuality = FoodQualityRating.valueOf(request.foodQualityRating());
        ServiceSpeedRating serviceSpeed = ServiceSpeedRating.valueOf(request.serviceSpeedRating());

        Feedback feedback = Feedback.builder()
                .order(order)
                .user(user)
                .foodQualityRating(foodQuality)
                .serviceSpeedRating(serviceSpeed)
                .wouldRecommend(request.wouldRecommend())
                .comment(request.comment())
                .build();

        Feedback saved = feedbackRepository.save(feedback);
        return toResponse(saved);
    }

    public FeedbackResponse getByOrderId(Long orderId) {
        Feedback feedback = feedbackRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoSuchElementException("No feedback found for this order"));
        return toResponse(feedback);
    }

    public boolean existsByOrderId(Long orderId) {
        return feedbackRepository.existsByOrderId(orderId);
    }

    public List<FeedbackResponse> getAll() {
        return feedbackRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private FeedbackResponse toResponse(Feedback feedback) {
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getOrder().getId(),
                feedback.getUser().getId(),
                feedback.getUser().getEmail(),
                feedback.getFoodQualityRating().name(),
                feedback.getServiceSpeedRating().name(),
                feedback.isWouldRecommend(),
                feedback.getComment(),
                feedback.getCreatedAt()
        );
    }
}
