package fpt.capstone.edu360managementsystem.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "course_lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseLesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "chapter_id")
    private CourseChapter chapter; // 1 chapter nhiều lesson

    @Column(nullable = false)
    private String title;          // tiêu đề bài học

    @Column(columnDefinition = "TEXT")
    private String description;    // phần giới thiệu tóm tắt (khung nội dung)

    @Column(nullable = false)
    private Integer orderIndex;    // thứ tự trong chapter
}
