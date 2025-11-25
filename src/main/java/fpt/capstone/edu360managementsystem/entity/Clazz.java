package fpt.capstone.edu360managementsystem.entity;

import java.time.LocalDate;

import fpt.capstone.edu360managementsystem.enums.ClassStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "classes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Clazz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;  // ví dụ: "Toán 12A" (code đã loại bỏ)

    @ManyToOne(optional = true)
    @JoinColumn(name = "semester_id", nullable = true)
    private Semester semester;

    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne(optional = false)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @ManyToOne(optional = true)  // nullable for online classes
    @JoinColumn(name = "room_id", nullable = true)
    private Room room;

    @Column(nullable = false)
    private LocalDate startDate;
    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private Integer maxStudents;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String meetingLink; // For online classes

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ClassStatus status = ClassStatus.AVAILABLE;

    @ManyToOne(optional = true)
    @JoinColumn(name = "course_id")
    private Course course;
}
