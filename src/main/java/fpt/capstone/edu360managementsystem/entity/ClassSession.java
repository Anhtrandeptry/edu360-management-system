package fpt.capstone.edu360managementsystem.entity;

import fpt.capstone.edu360managementsystem.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "class_sessions",
        indexes = @Index(name = "idx_class_session_class_date", columnList = "class_id,date"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "class_id")
    private Clazz clazz;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Integer dayOfWeek;

    @ManyToOne(optional = false)
    @JoinColumn(name = "timeslot_id")
    private TimeSlot timeSlot;

    @ManyToOne(optional = true)
    @JoinColumn(name = "room_id", nullable = true)
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.PLANNED;

    @Column(columnDefinition = "TEXT")
    private String lessonContent;
}
