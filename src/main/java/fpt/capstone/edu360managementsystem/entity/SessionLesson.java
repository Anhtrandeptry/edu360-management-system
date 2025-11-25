package fpt.capstone.edu360managementsystem.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "session_lessons",
        uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "lesson_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionLesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "session_id")
    private ClassSession session;

    @ManyToOne(optional = false)
    @JoinColumn(name = "lesson_id")
    private CourseLesson lesson;
}
