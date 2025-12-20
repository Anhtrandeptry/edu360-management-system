package fpt.capstone.edu360managementsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for resetting password with token verification.
 *
 * @author 360edu
 * @version 1.0
 */
@Data
public class ResetPasswordRequest {

    @NotBlank(message = "Token không được để trống")
    private String token;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 6, max = 40, message = "Mật khẩu phải từ 6-40 ký tự")
    private String newPassword;
}
