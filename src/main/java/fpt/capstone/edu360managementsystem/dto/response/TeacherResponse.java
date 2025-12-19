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

    private Long id;
    private Long userId;
    private String username;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String avatarUrl;

    private Long subjectId;
    private String subjectName;

    private java.util.List<Long> subjectIds;
    private java.util.List<String> subjectNames;
    private String specialization;
    private String degree;
    private Integer yearsOfExperience;
    private Double rating;
    private String bio;
    private String workplace;
    private Boolean active;
    private long classCount;

}
