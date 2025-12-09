package fpt.capstone.edu360managementsystem.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity lưu tài liệu đính kèm cho từng bài học (CourseLesson)
 * Giáo viên upload trong trang quản lý khóa học, học sinh có thể xem/download
 */
@Entity
@Table(name = "lesson_materials",
        indexes = @Index(name = "idx_lesson_material_lesson", columnList = "lesson_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "lesson_id")
    private CourseLesson lesson;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 1000)
    private String fileUrl;

    @Column(length = 100)
    private String fileType;

    @Column
    private Long fileSize;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @ManyToOne
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}
