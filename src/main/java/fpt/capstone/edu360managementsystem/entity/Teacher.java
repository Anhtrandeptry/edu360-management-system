package fpt.capstone.edu360managementsystem.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "teachers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Giữ lại subject chính (primary) để không phá vỡ schema cũ (cột subject_id NOT NULL)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    // Bổ sung danh sách môn phụ (ManyToMany). Hibernate sẽ tự tạo bảng teacher_subjects.
    @ManyToMany(fetch = FetchType.LAZY, cascade = {jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.MERGE})
    @JoinTable(
            name = "teacher_subjects",
            joinColumns = @JoinColumn(name = "teacher_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    @Builder.Default
    private Set<Subject> subjects = new HashSet<>();

    private String specialization;
    
    @Column(length = 50)
    private String degree;
    
    @Column(length = 1000)
    private String note;
    
    // Profile fields for teacher profile page
    @Column(name = "workplace", length = 255)
    private String workplace;
    
    @Column(name = "avatar_url", columnDefinition = "LONGTEXT")
    private String avatarUrl;
    
    @Column(name = "linkedin_url", length = 500)
    private String linkedinUrl;
    
    @Column(name = "facebook_url", length = 500)
    private String facebookUrl;
    
    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;
    
    // Profile statistics
    @Column(name = "years_of_experience")
    @Builder.Default
    private Integer yearsOfExperience = 0;
    
    @Column(name = "rating")
    @Builder.Default
    private Double rating = 0.0;
    
    @Column(name = "achievements", columnDefinition = "TEXT")
    private String achievements;
    
    // Relationships with profile tables
    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TeacherCertificate> certificates = new ArrayList<>();
    
    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TeacherExperience> experiences = new ArrayList<>();
    
    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TeacherEducation> educations = new ArrayList<>();

}
