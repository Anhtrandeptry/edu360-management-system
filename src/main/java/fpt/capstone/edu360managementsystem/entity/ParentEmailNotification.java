package fpt.capstone.edu360managementsystem.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tracking email đã gửi cho phụ huynh về điểm danh + nội dung buổi học Mỗi
 * student + ngày chỉ gửi 1 email duy nhất (gộp tất cả slot trong ngày)
 */
@Entity
@Table(name = "parent_email_notifications",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "notification_date"}),
        indexes = {
            @Index(name = "idx_pen_student_date", columnList = "student_id, notification_date"),
            @Index(name = "idx_pen_sent_at", columnList = "sent_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParentEmailNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(name = "notification_date", nullable = false)
    private LocalDate notificationDate; // Ngày học

    @Column(name = "parent_email", nullable = false)
    private String parentEmail; // Email phụ huynh tại thời điểm gửi

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt; // Thời điểm gửi email

    @Column(columnDefinition = "TEXT")
    private String emailContent; // Nội dung email đã gửi (để debug/reference)

    @Column(name = "session_count")
    private Integer sessionCount; // Số slot trong ngày
}
