package fpt.capstone.edu360managementsystem.dto.response;

import java.util.List;

import fpt.capstone.edu360managementsystem.dto.request.TeacherCertificateRequest;
import fpt.capstone.edu360managementsystem.dto.request.TeacherEducationRequest;
import fpt.capstone.edu360managementsystem.dto.request.TeacherExperienceRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


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
    

    private String degree;
    private String specialization;
    private String workplace;
    

    private String subject;
    private List<String> subjects;
    

    private String linkedinUrl;
    private String facebookUrl;
    

    private String bio;
    

    private Integer classCount;
    private Integer studentCount;
    private Integer yearsOfExperience;
    private Double rating;
    private String achievements;
    

    private List<TeacherCertificateRequest> certificates;
    private List<TeacherExperienceRequest> experiences;
    private List<TeacherEducationRequest> educations;
    

    private Boolean isActive;
}
