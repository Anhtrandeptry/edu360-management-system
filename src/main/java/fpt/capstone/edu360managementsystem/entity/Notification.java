package fpt.capstone.edu360managementsystem.entity;

import fpt.capstone.edu360managementsystem.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity lưu thông báo cho user (student, teacher, admin).
 * Các loại thông báo: thay đổi lịch học, được thêm vào lớp, thanh toán thành công, v.v.
 */
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

    /**
     * User nhận thông báo
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Tiêu đề thông báo
     */
    @Column(nullable = false)
    private String title;

    /**
     * Nội dung chi tiết
     */
    @Column(columnDefinition = "TEXT")
    private String message;

    /**
     * Loại thông báo
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    /**
     * Link để redirect khi click vào thông báo (optional)
     */
    private String link;

    /**
     * Đã đọc chưa
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    /**
     * Thời gian tạo
     */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /**
     * Thời gian đọc
     */
    private LocalDateTime readAt;
}
