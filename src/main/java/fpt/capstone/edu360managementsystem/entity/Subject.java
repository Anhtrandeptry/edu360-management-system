package fpt.capstone.edu360managementsystem.entity;

import fpt.capstone.edu360managementsystem.enums.SubjectStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "subjects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    private SubjectStatus status = SubjectStatus.AVAILABLE;


    @ManyToMany(mappedBy = "subjects")
    private Set<Teacher> teachers = new HashSet<>();

}
