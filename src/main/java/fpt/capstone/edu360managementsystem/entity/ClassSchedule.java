package fpt.capstone.edu360managementsystem.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "class_schedules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClassSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false) @JoinColumn(name="class_id")
    private Clazz clazz;

    @Column(nullable=false)
    private Integer dayOfWeek; // 1=Mon ... 7=Sun

    @ManyToOne(optional=false) @JoinColumn(name="timeslot_id")
    private TimeSlot timeSlot;
}
