package fpt.capstone.edu360managementsystem.dto.response;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileResponse {


    private Long id;
    private Long userId;
    private String username;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String avatarUrl;
    

    private LocalDate dob;
    private String grade;
    private String school;
    

    private ParentInfo parent;
    

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
