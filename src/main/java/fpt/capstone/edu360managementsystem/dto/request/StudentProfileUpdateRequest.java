package fpt.capstone.edu360managementsystem.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating student profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileUpdateRequest {

    @Size(max = 100)
    private String fullName;
    
    @Email
    @Size(max = 50)
    private String email;
    
    @Size(max = 15)
    private String phoneNumber;
    
    private LocalDate dob;
    
    @Size(max = 50)
    private String grade;
    
    @Size(max = 255)
    private String school;
    
    private String avatarUrl;
}
