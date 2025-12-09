package fpt.capstone.edu360managementsystem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response cho Google OAuth login
 * Nếu needsRegistration = true → FE hiển thị form nhập thông tin phụ huynh
 * Nếu needsRegistration = false → Login thành công, trả về user info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleAuthResponse {
    private boolean needsRegistration;
    
    // Thông tin từ Google (dùng khi cần đăng ký)
    private String googleEmail;
    private String googleName;
    private String googlePicture;
    private String googleId;
    
    // Thông tin user (khi login thành công)
    private Long userId;
    private String username;
    private String fullName;
    private String email;
    private List<String> roles;
    private String avatarUrl;
    
    // JWT token (backup nếu cookie không hoạt động)
    private String token;
    
    private String message;
}
