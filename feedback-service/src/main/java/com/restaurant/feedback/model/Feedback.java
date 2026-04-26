package com.restaurant.feedback.model;

import com.restaurant.feedback.enums.FoodQualityRating;
import com.restaurant.feedback.enums.ServiceSpeedRating;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "feedback")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FoodQualityRating foodQualityRating;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceSpeedRating serviceSpeedRating;

    @Column(nullable = false)
    private boolean wouldRecommend;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
