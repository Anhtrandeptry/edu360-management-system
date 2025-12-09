package fpt.capstone.edu360managementsystem.entity;

import fpt.capstone.edu360managementsystem.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


    @Column(nullable = false)
    private String title;


    @Column(columnDefinition = "TEXT")
    private String message;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;


    private String link;


    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false;


    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;


    private LocalDateTime readAt;
}
