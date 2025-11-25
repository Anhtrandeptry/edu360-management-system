package fpt.capstone.edu360managementsystem.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "session_chapters",
        uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "chapter_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionChapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "session_id")
    private ClassSession session;

    @ManyToOne(optional = false)
    @JoinColumn(name = "chapter_id")
    private CourseChapter chapter;
}
