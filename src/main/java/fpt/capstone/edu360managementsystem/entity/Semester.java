package fpt.capstone.edu360managementsystem.entity;

import fpt.capstone.edu360managementsystem.enums.SubjectStatus; // chỉ để đồng bộ import style
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "semesters")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Semester {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true)
    private String name;            // ví dụ: "HK1-2025"

    @Column(nullable=false)
    private LocalDate startDate;

    @Column(nullable=false)
    private LocalDate endDate;
}
