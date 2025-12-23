package fpt.capstone.edu360managementsystem.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fpt.capstone.edu360managementsystem.dto.request.ForgotPasswordRequest;
import fpt.capstone.edu360managementsystem.dto.request.GoogleAuthRequest;
import fpt.capstone.edu360managementsystem.dto.request.GoogleRegisterRequest;
import fpt.capstone.edu360managementsystem.dto.request.LoginRequest;
import fpt.capstone.edu360managementsystem.dto.request.RegisterStudentWithParentRequest;
import fpt.capstone.edu360managementsystem.dto.request.RegisterTeacherRequest;
import fpt.capstone.edu360managementsystem.dto.request.ResetPasswordRequest;
import fpt.capstone.edu360managementsystem.dto.request.SendOtpRequest;
import fpt.capstone.edu360managementsystem.dto.request.VerifyOtpRequest;
import fpt.capstone.edu360managementsystem.dto.response.GoogleAuthResponse;
import fpt.capstone.edu360managementsystem.dto.response.MessageResponse;
import fpt.capstone.edu360managementsystem.dto.response.UserInfoResponse;
import fpt.capstone.edu360managementsystem.entity.Student;
import fpt.capstone.edu360managementsystem.entity.Teacher;
import fpt.capstone.edu360managementsystem.entity.User;
import fpt.capstone.edu360managementsystem.repository.RoleRepository;
import fpt.capstone.edu360managementsystem.repository.StudentRepository;
import fpt.capstone.edu360managementsystem.repository.TeacherRepository;
import fpt.capstone.edu360managementsystem.repository.UserRepository;
import fpt.capstone.edu360managementsystem.security.jwt.JwtUtils;
import fpt.capstone.edu360managementsystem.service.AuthService;
import fpt.capstone.edu360managementsystem.service.EmailVerificationService;
import fpt.capstone.edu360managementsystem.service.UserDetailsImpl;
import jakarta.validation.Valid;

/**
 * REST controller for authentication and authorization. Handles login, logout,
 * registration, password reset, and Google OAuth.
 *
 * @author 360edu
 * @version 1.0
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    TeacherRepository teacherRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    AuthService authService;

    @Autowired
    fpt.capstone.edu360managementsystem.service.GoogleAuthService googleAuthService;

    @Autowired
    fpt.capstone.edu360managementsystem.service.RateLimiterService rateLimiterService;

    @Autowired
    EmailVerificationService emailVerificationService;

    // Rate limit config cho forgot password: 3 lần trong 5 phút
    private static final int FORGOT_PASSWORD_MAX_ATTEMPTS = 3;
    private static final long FORGOT_PASSWORD_WINDOW_MS = 5 * 60 * 1000; // 5 phút

    // Rate limit config cho register: 3 lần/IP trong 1 giờ
    private static final int REGISTER_MAX_ATTEMPTS = 3;
    private static final long REGISTER_WINDOW_MS = 60 * 60 * 1000; // 1 giờ

    /**
     * Authenticates user with username and password. Sets JWT token in
     * HTTP-only cookie upon successful authentication. Rate limited: 5 failed
     * attempts triggers progressive lockout.
     *
     * @param loginRequest the login credentials
     * @param request HTTP request for IP extraction
     * @return user info with roles and JWT cookie
     */
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(
            @Valid @RequestBody LoginRequest loginRequest,
            jakarta.servlet.http.HttpServletRequest request) {

        String username = loginRequest.getUsername();
        String clientIp = getClientIp(request);
        String rateLimitKeyByUsername = "login:user:" + username;
        String rateLimitKeyByIp = "login:ip:" + clientIp;

        // Kiểm tra lockout theo username
        long lockoutByUsername = rateLimiterService.getLoginLockoutSeconds(rateLimitKeyByUsername);
        if (lockoutByUsername > 0) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new MessageResponse("Tài khoản tạm thời bị khóa do đăng nhập sai nhiều lần. Vui lòng thử lại sau "
                            + formatDuration(lockoutByUsername) + "."));
        }

        // Kiểm tra lockout theo IP (chống brute force từ cùng IP)
        long lockoutByIp = rateLimiterService.getLoginLockoutSeconds(rateLimitKeyByIp);
        if (lockoutByIp > 0) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new MessageResponse("Quá nhiều lần đăng nhập thất bại từ địa chỉ IP này. Vui lòng thử lại sau "
                            + formatDuration(lockoutByIp) + "."));
        }

        try {
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword()));

            // Login thành công - xóa record thất bại
            rateLimiterService.clearLoginAttempts(rateLimitKeyByUsername);
            rateLimiterService.clearLoginAttempts(rateLimitKeyByIp);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);

            List<String> roles = userDetails.getAuthorities().stream()
                    .map(item -> item.getAuthority())
                    .collect(Collectors.toList());

            String fullName = null;
            String avatarUrl = null;

            User user = userRepository.findById(userDetails.getId()).orElse(null);
            if (user != null) {
                fullName = user.getFullName();

                Student student = studentRepository.findByUser_Id(userDetails.getId()).orElse(null);
                if (student != null && student.getAvatarUrl() != null) {
                    avatarUrl = student.getAvatarUrl();
                } else {
                    Teacher teacher = teacherRepository.findByUserId(userDetails.getId()).orElse(null);
                    if (teacher != null && teacher.getAvatarUrl() != null) {
                        avatarUrl = teacher.getAvatarUrl();
                    }
                }
            }

            return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                    .body(new UserInfoResponse(userDetails.getId(),
                            userDetails.getUsername(),
                            userDetails.getEmail(),
                            fullName,
                            avatarUrl,
                            roles));

        } catch (org.springframework.security.core.AuthenticationException e) {
            // Login thất bại - ghi nhận và có thể trigger lockout
            rateLimiterService.recordFailedLoginAndGetLockoutSeconds(rateLimitKeyByUsername);
            long nextLockout = rateLimiterService.recordFailedLoginAndGetLockoutSeconds(rateLimitKeyByIp);

            String message = "Sai tên đăng nhập hoặc mật khẩu.";
            if (nextLockout > 0) {
                message += " Tài khoản sẽ bị khóa " + formatDuration(nextLockout) + " nếu tiếp tục nhập sai.";
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse(message));
        }
    }

    /**
     * Format duration in human readable format
     */
    private String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds + " giây";
        }
        if (seconds < 3600) {
            return (seconds / 60) + " phút";
        }
        return (seconds / 3600) + " giờ";
    }

    /**
     * Extract client IP from request (handle proxy)
     */
    private String getClientIp(jakarta.servlet.http.HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    /**
     * Logs out the current user by clearing the JWT cookie.
     *
     * @return success message
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        ResponseCookie cookie = jwtUtils.getCleanJwtCookie();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new MessageResponse("You've been signed out!"));
    }

    /**
     * Retrieves current authenticated user information.
     *
     * @param user the authenticated user details
     * @return user info with roles and avatar
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(@org.springframework.security.core.annotation.AuthenticationPrincipal fpt.capstone.edu360managementsystem.service.UserDetailsImpl user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        var roles = user.getAuthorities().stream().map(a -> a.getAuthority()).toList();

        String fullName = null;
        String avatarUrl = null;

        User userEntity = userRepository.findById(user.getId()).orElse(null);
        if (userEntity != null) {
            fullName = userEntity.getFullName();

            Student student = studentRepository.findByUser_Id(user.getId()).orElse(null);
            if (student != null && student.getAvatarUrl() != null) {
                avatarUrl = student.getAvatarUrl();
            } else {
                Teacher teacher = teacherRepository.findByUserId(user.getId()).orElse(null);
                if (teacher != null && teacher.getAvatarUrl() != null) {
                    avatarUrl = teacher.getAvatarUrl();
                }
            }
        }

        return ResponseEntity.ok(new fpt.capstone.edu360managementsystem.dto.response.UserInfoResponse(
                user.getId(), user.getUsername(), user.getEmail(), fullName, avatarUrl, roles
        ));
    }

    /**
     * Registers a new teacher account. Admin only endpoint.
     *
     * @param request teacher registration data
     * @return registration result
     */
    @PostMapping("/register-teacher")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> registerTeacher(@Valid @RequestBody RegisterTeacherRequest request) {
        return authService.registerTeacher(request);
    }

    // ===================== EMAIL VERIFICATION (OTP) =====================
    /**
     * Gửi mã OTP xác thực email khi đăng ký. Dùng để kiểm tra email thật sự tồn
     * tại trước khi cho phép đăng ký. Rate limited: 3 lần / email / 5 phút.
     *
     * @param request chứa email cần xác thực
     * @return kết quả gửi OTP
     */
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        // Kiểm tra email đã tồn tại trong hệ thống chưa
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Email này đã được sử dụng trong hệ thống."));
        }

        var result = emailVerificationService.sendOtp(email);

        if (result.success()) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", result.message(),
                    "expiryMinutes", result.waitMinutes()
            ));
        } else {
            // Phân biệt rate limit (waitMinutes > 0) vs email error (waitMinutes = 0)
            if (result.waitMinutes() > 0) {
                // Rate limited
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(new MessageResponse(result.message()));
            } else {
                // Email error hoặc lỗi khác
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new MessageResponse(result.message()));
            }
        }
    }

    /**
     * Xác thực mã OTP. User nhập OTP nhận được qua email để xác thực.
     *
     * @param request chứa email và OTP
     * @return kết quả xác thực
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        String otp = request.getOtp().trim();

        var result = emailVerificationService.verifyOtp(email, otp);

        if (result.success()) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", result.message(),
                    "verified", true
            ));
        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", result.message(),
                            "remainingAttempts", result.remainingAttempts()
                    ));
        }
    }

    // ===================== STUDENT REGISTRATION =====================
    /**
     * Registers a new student account with parent information. Public endpoint
     * for student self-registration. Rate limited: 3 registrations per IP per
     * hour to prevent spam.
     *
     * @param request student and parent registration data
     * @param httpRequest HTTP request for IP extraction
     * @return registration result
     */
    @PostMapping("/signup")
    public ResponseEntity<?> registerStudentWithParent(
            @Valid @RequestBody RegisterStudentWithParentRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {

        String clientIp = getClientIp(httpRequest);
        String rateLimitKey = "register:ip:" + clientIp;

        // Kiểm tra rate limit: 3 lần đăng ký/IP/giờ
        if (!rateLimiterService.isAllowed(rateLimitKey, REGISTER_MAX_ATTEMPTS, REGISTER_WINDOW_MS)) {
            long remainingSeconds = rateLimiterService.getRemainingCooldownSeconds(rateLimitKey, REGISTER_WINDOW_MS);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new MessageResponse("Bạn đã đăng ký quá nhiều tài khoản. Vui lòng thử lại sau "
                            + formatDuration(remainingSeconds) + "."));
        }

        return authService.registerStudentWithParent(request);
    }

    /**
     * Initiates password reset process. Sends reset link to user's email.
     *
     * @param request forgot password data with email
     * @return operation result
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        String rateLimitKey = "forgot_password:" + email;

        // Kiểm tra rate limit
        if (!rateLimiterService.isAllowed(rateLimitKey, FORGOT_PASSWORD_MAX_ATTEMPTS, FORGOT_PASSWORD_WINDOW_MS)) {
            long remainingSeconds = rateLimiterService.getRemainingCooldownSeconds(rateLimitKey, FORGOT_PASSWORD_WINDOW_MS);
            long remainingMinutes = (remainingSeconds / 60) + 1;

            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new MessageResponse(
                            String.format("Bạn đã yêu cầu quá nhiều lần. Vui lòng thử lại sau %d phút.", remainingMinutes)));
        }

        return authService.forgotPassword(request);
    }

    /**
     * Resets password with token verification. User must click the link in
     * email to get the token.
     *
     * @param request contains token and new password
     * @return operation result
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    /**
     * Validates a password reset token without resetting. Used by frontend to
     * check if token is valid before showing reset form.
     *
     * @param token the reset token
     * @return valid status
     */
    @GetMapping("/validate-reset-token")
    public ResponseEntity<?> validateResetToken(@RequestParam String token) {
        boolean isValid = authService.validateResetToken(token);
        if (isValid) {
            return ResponseEntity.ok(new MessageResponse("Token hợp lệ."));
        } else {
            return ResponseEntity.badRequest().body(new MessageResponse(
                    "Link đặt lại mật khẩu không hợp lệ hoặc đã hết hạn."));
        }
    }

    /**
     * Authenticates user with Google OAuth. Returns existing user info or
     * indicates registration is needed.
     *
     * @param request Google authentication data with ID token
     * @return authentication result with user info or registration flag
     */
    @PostMapping("/google")
    public ResponseEntity<GoogleAuthResponse> authenticateWithGoogle(@Valid @RequestBody GoogleAuthRequest request) {
        try {
            GoogleAuthResponse response = googleAuthService.handleGoogleCallback(request);

            if (response.getUserId() != null && !response.isNeedsRegistration()) {
                String jwt = jwtUtils.generateTokenFromUsername(response.getUsername());

                response.setToken(jwt);

                ResponseCookie jwtCookie = ResponseCookie.from("edu360_jwt", jwt)
                        .path("/")
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .maxAge(24L * 60 * 60)
                        .build();
                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                        .body(response);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            GoogleAuthResponse errorResponse = GoogleAuthResponse.builder()
                    .needsRegistration(false)
                    .message("Lỗi xác thực Google: " + e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Completes Google OAuth registration for new users. Creates account and
     * links with Google profile.
     *
     * @param request Google registration data with role selection
     * @return registration result with user info
     */
    @PostMapping("/google/register")
    public ResponseEntity<GoogleAuthResponse> completeGoogleRegistration(@Valid @RequestBody GoogleRegisterRequest request) {
        try {
            GoogleAuthResponse response = googleAuthService.registerWithGoogle(request);

            if (response.getUserId() != null) {
                String jwt = jwtUtils.generateTokenFromUsername(response.getUsername());

                response.setToken(jwt);

                ResponseCookie jwtCookie = ResponseCookie.from("edu360_jwt", jwt)
                        .path("/")
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .maxAge(24L * 60 * 60)
                        .build();
                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                        .body(response);
            }

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            GoogleAuthResponse errorResponse = GoogleAuthResponse.builder()
                    .needsRegistration(false)
                    .message("Lỗi đăng ký: " + e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Checks if a parent phone number already exists. Used during student
     * registration to link with existing parent.
     *
     * @param phone parent phone number to check
     * @return exists flag and parent info if found
     */
    @GetMapping("/check-parent-phone")
    public ResponseEntity<Map<String, Object>> checkParentPhone(@RequestParam String phone) {
        try {
            Map<String, Object> response = googleAuthService.checkParentPhone(phone);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("exists", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.ok(errorResponse);
        }
    }

    // ===================== DEV/TEST ENDPOINTS (XÓA TRƯỚC KHI PRODUCTION) =====================
    /**
     * [DEV ONLY] Reset tất cả rate limits và xóa OTP data để test. XÓA ENDPOINT
     * NÀY TRƯỚC KHI DEPLOY PRODUCTION!
     */
    @PostMapping("/dev/reset-rate-limits")
    public ResponseEntity<?> devResetRateLimits(@RequestParam(required = false) String email) {
        try {
            // 1. Clear in-memory rate limits
            rateLimiterService.clearAll();

            // 2. Clear OTP records từ database
            if (email != null && !email.isEmpty()) {
                emailVerificationService.clearOtpByEmail(email.toLowerCase().trim());
            } else {
                emailVerificationService.clearAllOtps();
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã reset tất cả rate limits" + (email != null ? " cho " + email : "")
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

}
