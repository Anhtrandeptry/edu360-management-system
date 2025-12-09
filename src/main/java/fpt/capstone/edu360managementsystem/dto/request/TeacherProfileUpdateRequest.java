package fpt.capstone.edu360managementsystem.dto.request;

import java.util.List;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating teacher profile information.
 * Allows teachers to update their personal information, avatar, and professional details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherProfileUpdateRequest {

    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phoneNumber;

    @Size(max = 50, message = "Degree must not exceed 50 characters")
    private String degree;

    @Size(max = 500, message = "Specialization must not exceed 500 characters")
    private String specialization;

    @Size(max = 255, message = "Workplace must not exceed 255 characters")
    private String workplace;

    @Size(max = 500, message = "LinkedIn URL must not exceed 500 characters")
    private String linkedinUrl;

    @Size(max = 500, message = "Facebook URL must not exceed 500 characters")
    private String facebookUrl;

    @Size(max = 1000, message = "Bio must not exceed 1000 characters")
    private String bio;

    @Size(max = 1000, message = "Note must not exceed 1000 characters")
    private String note;


    private String avatarUrl;
    

    private Integer yearsOfExperience;
    private Double rating;
    private String achievements;
    

    private List<TeacherCertificateRequest> certificates;
    private List<TeacherExperienceRequest> experiences;
    private List<TeacherEducationRequest> educations;
}
