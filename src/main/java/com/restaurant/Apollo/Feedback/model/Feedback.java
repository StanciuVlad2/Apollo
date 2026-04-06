package com.restaurant.Apollo.Feedback.model;

import com.restaurant.Apollo.Feedback.enums.FoodQualityRating;
import com.restaurant.Apollo.Feedback.enums.ServiceSpeedRating;
import com.restaurant.Apollo.Orders.model.Order;
import com.restaurant.Apollo.UserManagement.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"order_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "food_quality_rating", nullable = false)
    private FoodQualityRating foodQualityRating;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_speed_rating", nullable = false)
    private ServiceSpeedRating serviceSpeedRating;

    @Column(name = "would_recommend", nullable = false)
    private boolean wouldRecommend;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
