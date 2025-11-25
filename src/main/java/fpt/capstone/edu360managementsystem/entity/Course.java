package fpt.capstone.edu360managementsystem.entity;

import fpt.capstone.edu360managementsystem.enums.CourseStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id")
    private Subject subject;   // 1 course thuộc 1 môn

    @Column(nullable = false)
    private String title;      // tiêu đề course (tên khóa/giáo trình)

    @Column(columnDefinition = "TEXT")
    private String description;  // phần giới thiệu tổng quan

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CourseStatus status = CourseStatus.PENDING;

    // Ai tạo (để phân biệt admin / teacher)
    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    // optional: nếu muốn lưu luôn giáo viên "owner" course
    @ManyToOne(optional = true)
    @JoinColumn(name = "teacher_id")
    private Teacher ownerTeacher;
}
