package fpt.capstone.edu360managementsystem.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request đăng ký tài khoản qua Google OAuth
 * Yêu cầu thông tin phụ huynh bắt buộc
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleRegisterRequest {
    // Thông tin từ Google
    @NotBlank(message = "Google ID is required")
    private String googleId;
    
    @NotBlank(message = "Google email is required")
    @Email(message = "Invalid email format")
    private String googleEmail;
    
    @NotBlank(message = "Google name is required")
    private String googleName;
    
    private String googlePicture;
    
    // Thông tin học sinh
    @NotBlank(message = "Username là bắt buộc")
    private String username;
    
    @NotBlank(message = "Họ tên học sinh là bắt buộc")
    private String studentFullName;
    
    @NotBlank(message = "Số điện thoại học sinh là bắt buộc")
    private String studentPhone;
    
    // Thông tin phụ huynh (bắt buộc)
    @NotBlank(message = "Họ tên phụ huynh là bắt buộc")
    private String parentFullName;
    
    @NotBlank(message = "Số điện thoại phụ huynh là bắt buộc")
    private String parentPhone;
    
    @Email(message = "Email phụ huynh không hợp lệ")
    private String parentEmail;
}
