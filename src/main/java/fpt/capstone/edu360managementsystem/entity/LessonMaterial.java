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
    private String fileName;  // Tên file gốc hoặc title cho link

    @Column(nullable = false, length = 1000)
    private String fileUrl;   // URL hoặc đường dẫn lưu file

    @Column(length = 100)
    private String fileType;  // MIME type: application/pdf, image/png, LINK, etc.

    @Column
    private Long fileSize;    // Kích thước file (bytes), null cho link

    @Column(length = 500)
    private String description;  // Mô tả tài liệu (optional)

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @ManyToOne
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;  // Giáo viên upload

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}
