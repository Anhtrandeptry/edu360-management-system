package fpt.capstone.edu360managementsystem.entity;

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

@Entity
@Table(name = "teacher_course_versions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"base_course_id", "teacher_course_id", "teacher_id"}),
        indexes = {
            @Index(name = "idx_tcv_base_teacher", columnList = "base_course_id,teacher_id"),
            @Index(name = "idx_tcv_teacher_course", columnList = "teacher_course_id,teacher_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherCourseVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Khóa học gốc được Admin gán cho lớp
    @ManyToOne(optional = false)
    @JoinColumn(name = "base_course_id")
    private Course baseCourse;

    // Phiên bản tùy chỉnh do giáo viên tạo
    @ManyToOne(optional = false)
    @JoinColumn(name = "teacher_course_id")
    private Course teacherCourse;

    // Giáo viên sở hữu phiên bản này
    @ManyToOne(optional = false)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;
}
