package fpt.capstone.edu360managementsystem.entity;

import fpt.capstone.edu360managementsystem.enums.SubjectStatus;
import jakarta.persistence.*;
import lombok.*;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SubjectStatus getStatus() {
        return status;
    }

    public void setStatus(SubjectStatus status) {
        this.status = status;
    }
}
