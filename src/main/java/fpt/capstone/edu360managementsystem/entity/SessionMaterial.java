package fpt.capstone.edu360managementsystem.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity lưu tài liệu đính kèm cho từng buổi học (ClassSession)
 * Giáo viên upload sau khi điểm danh, học sinh có thể xem/download
 */
@Entity
@Table(name = "session_materials",
        indexes = @Index(name = "idx_session_material_session", columnList = "session_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "session_id")
    private ClassSession session;

    @Column(nullable = false, length = 255)
    private String fileName;  // Tên file gốc

    @Column(nullable = false, length = 500)
    private String fileUrl;   // URL hoặc đường dẫn lưu file

    @Column(length = 100)
    private String fileType;  // MIME type: application/pdf, image/png, etc.

    @Column
    private Long fileSize;    // Kích thước file (bytes)

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
