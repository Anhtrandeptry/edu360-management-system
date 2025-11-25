package fpt.capstone.edu360managementsystem.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "course_chapters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseChapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "course_id")
    private Course course;     // 1 course có nhiều chapter

    @Column(nullable = false)
    private String title;      // tiêu đề chương

    @Column(columnDefinition = "TEXT")
    private String description; // giới thiệu chương

    @Column(nullable = false)
    private Integer orderIndex; // thứ tự chương trong course (1,2,3,...)
}
