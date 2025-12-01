package fpt.capstone.edu360managementsystem.dto.response;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for student profile information.
 * Contains comprehensive student details including parent info.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileResponse {

    // Student info
    private Long id;
    private Long userId;
    private String username;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String avatarUrl;
    
    // Student specific info
    private LocalDate dob;
    private String grade;
    private String school;
    
    // Parent info
    private ParentInfo parent;
    
    // Status
    private Boolean isActive;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParentInfo {
        private Long id;
        private String fullName;
        private String email;
        private String phoneNumber;
        private String occupation;
        private String address;
    }
}
