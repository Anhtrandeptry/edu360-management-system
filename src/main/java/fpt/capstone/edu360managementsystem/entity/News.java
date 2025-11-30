package fpt.capstone.edu360managementsystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "news")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String excerpt; // Mô tả ngắn

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // Nội dung chi tiết

    @Column(columnDefinition = "LONGTEXT")
    private String imageUrl; // URL ảnh đại diện (base64 hoặc URL)

    @Column(length = 100)
    private String author; // Tác giả

    @Column(nullable = false, length = 20)
    private String status = "DRAFT"; // DRAFT, PUBLISHED, HIDDEN

    @Column(nullable = false)
    private Integer views = 0; // Số lượt xem

    @Column(length = 500)
    private String tags; // Danh sách tags (phân cách bằng dấu phẩy)

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime publishedAt; // Thời điểm xuất bản
}
