package fpt.capstone.edu360managementsystem.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterStudentWithParentRequest {

    // ----- Student -----
    @NotBlank
    private String studentFullName;

    @NotBlank
    @Size(min = 3, max = 20)
    private String studentUsername;

    @NotBlank
    @Size(min = 6, max = 40)
    private String studentPassword;

    @NotBlank
    private String studentRePassword;

    @NotBlank
    private String studentPhoneNumber;

    @Email
    private String studentEmail;

    // ----- Parent -----
    @NotBlank
    private String parentFullName;

    @Email
    private String parentEmail;

    @NotBlank
    private String parentPhoneNumber;

    // ----- Existing Parent (optional - dùng khi liên kết học sinh với phụ huynh đã có trong hệ thống) -----
    private Long existingParentId;

}
