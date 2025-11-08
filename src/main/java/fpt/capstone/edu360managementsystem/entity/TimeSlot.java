package fpt.capstone.edu360managementsystem.entity;

import jakarta.persistence.*;
import lombok.*;
import java.sql.Time;

@Entity
@Table(name = "time_slots")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TimeSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private Time startTime;

    @Column(nullable=false)
    private Time endTime;
}
