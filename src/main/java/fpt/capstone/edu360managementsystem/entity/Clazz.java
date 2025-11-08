package fpt.capstone.edu360managementsystem.entity;

import fpt.capstone.edu360managementsystem.enums.ClassStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "classes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"name","subject_id","semester_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Clazz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false) private String name;  // ví dụ: "Toán 12A"
    @Column(unique = true)  private String code;  // ví dụ: "MATH12A"

    @ManyToOne(optional=false) @JoinColumn(name="semester_id")
    private Semester semester;

    @ManyToOne(optional=false) @JoinColumn(name="subject_id")
    private Subject subject;

    @ManyToOne(optional=false) @JoinColumn(name="teacher_id")
    private Teacher teacher;

    @ManyToOne(optional=false) @JoinColumn(name="room_id")
    private Room room;

    @Column(nullable=false) private LocalDate startDate;
    @Column(nullable=false) private LocalDate endDate;

    @Column(nullable=false) private Integer maxStudents;

    @Column(columnDefinition="TEXT") private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private ClassStatus status = ClassStatus.AVAILABLE;
}
