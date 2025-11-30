package fpt.capstone.edu360managementsystem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Teacher entity. Returns teacher information with user
 * details for frontend.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeacherResponse {

    private Long id;              // teacher.id
    private Long userId;          // user.id (for compatibility with existing FE code)
    private String username;      // user.username
    private String fullName;      // user.fullName
    private String email;         // user.email
    private String phoneNumber;   // user.phoneNumber
    private String avatarUrl;     // teacher.avatarUrl
    // Backward compatibility: still expose first subject as subjectId/subjectName
    private Long subjectId;       // first subject id (for legacy FE)
    private String subjectName;   // first subject name
    // New multi-subject fields
    private java.util.List<Long> subjectIds;      // all subject ids
    private java.util.List<String> subjectNames;  // all subject names
    private String specialization; // teacher.specialization
    private String degree;        // teacher.degree
    private Integer yearsOfExperience; // teacher.yearsOfExperience
    private Double rating;        // teacher.rating
    private String bio;           // teacher.bio
    private String workplace;     // teacher.workplace
    private Boolean active;       // user.active
    private long classCount;      // số lớp chưa COMPLETE mà giáo viên đang dạy

}
