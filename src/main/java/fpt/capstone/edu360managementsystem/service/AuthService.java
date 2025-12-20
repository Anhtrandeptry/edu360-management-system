package fpt.capstone.edu360managementsystem.service;

import org.springframework.http.ResponseEntity;

import fpt.capstone.edu360managementsystem.dto.request.ForgotPasswordRequest;
import fpt.capstone.edu360managementsystem.dto.request.RegisterStudentWithParentRequest;
import fpt.capstone.edu360managementsystem.dto.request.RegisterTeacherRequest;
import fpt.capstone.edu360managementsystem.dto.request.ResetPasswordRequest;

public interface AuthService {

    ResponseEntity<?> registerStudentWithParent(RegisterStudentWithParentRequest request);

    ResponseEntity<?> registerTeacher(RegisterTeacherRequest request);

    ResponseEntity<?> forgotPassword(ForgotPasswordRequest request);

    /**
     * Reset password with token verification
     *
     * @param request contains token and new password
     * @return success or error response
     */
    ResponseEntity<?> resetPassword(ResetPasswordRequest request);

    /**
     * Validate reset token without resetting password
     *
     * @param token the reset token
     * @return true if token is valid
     */
    boolean validateResetToken(String token);
}
