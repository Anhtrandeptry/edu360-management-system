package fpt.capstone.edu360managementsystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "session_content_configs",
        indexes = {
            @Index(name = "idx_scc_session", columnList = "session_id", unique = true)
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionContentConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "session_id", unique = true)
    private ClassSession session;

    @Column(name = "source_type", length = 16, nullable = false)
    private String sourceType; // ADMIN | PERSONAL

    @Column(name = "base_course_id")
    private Long baseCourseId; // nếu ADMIN

    @Column(name = "teacher_course_id")
    private Long teacherCourseId; // nếu PERSONAL

    @Column(name = "chapter_id")
    private Long chapterId;

    @Column(name = "lesson_id")
    private Long lessonId;
}
