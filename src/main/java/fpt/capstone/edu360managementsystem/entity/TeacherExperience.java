package fpt.capstone.edu360managementsystem.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "teacher_experience")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherExperience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Column(length = 255)
    private String position;

    @Column(length = 255)
    private String company;

    @Column(name = "start_year")
    private Integer startYear;

    @Column(name = "end_year")
    private Integer endYear;

    @Column(columnDefinition = "TEXT")
    private String description;
}
