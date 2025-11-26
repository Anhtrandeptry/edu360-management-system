package fpt.capstone.edu360managementsystem.dto.response;

import java.util.List;

import fpt.capstone.edu360managementsystem.dto.request.TeacherCertificateRequest;
import fpt.capstone.edu360managementsystem.dto.request.TeacherEducationRequest;
import fpt.capstone.edu360managementsystem.dto.request.TeacherExperienceRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for teacher profile information.
 * Contains comprehensive teacher details for profile display.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherProfileResponse {

    private Long id;
    private Long userId;
    private String username;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String avatarUrl;
    
    // Professional information
    private String degree;
    private String specialization;
    private String workplace;
    
    // Subject information
    private String subject;
    private List<String> subjects;
    
    // Social links
    private String linkedinUrl;
    private String facebookUrl;
    
    // Bio/description
    private String bio;
    
    // Statistics
    private Integer classCount;
    private Integer studentCount;
    private Integer yearsOfExperience;
    private Double rating;
    private String achievements;
    
    // Complex profile data from separate tables
    private List<TeacherCertificateRequest> certificates;
    private List<TeacherExperienceRequest> experiences;
    private List<TeacherEducationRequest> educations;
    
    // Status
    private Boolean isActive;
}
