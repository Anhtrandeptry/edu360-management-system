package fpt.capstone.edu360managementsystem.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "class_enrollments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"class_id","student_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClassEnrollment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false) @JoinColumn(name="class_id")
    private Clazz clazz;

    @ManyToOne(optional=false) @JoinColumn(name="student_id")
    private Student student;
}
