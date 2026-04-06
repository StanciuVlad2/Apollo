package com.restaurant.Apollo.Feedback.repository;

import com.restaurant.Apollo.Feedback.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Optional<Feedback> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    List<Feedback> findAllByOrderByCreatedAtDesc();

    List<Feedback> findByUserIdOrderByCreatedAtDesc(Long userId);
}
