package fpt.capstone.edu360managementsystem.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.mail.MailException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fpt.capstone.edu360managementsystem.entity.EmailVerification;
import fpt.capstone.edu360managementsystem.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý xác thực email bằng OTP.
 *
 * - Gửi OTP 6 số đến email - OTP có hiệu lực 5 phút - Giới hạn 3 lần gửi OTP /
 * email / 5 phút - Giới hạn 5 lần nhập sai OTP
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailService emailService;

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_OTP_SENDS_PER_WINDOW = 3;
    private static final int OTP_SEND_WINDOW_MINUTES = 5;
    private static final int MAX_VERIFY_ATTEMPTS = 5;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Tạo và gửi OTP đến email.
     *
     * @param email Email cần xác thực
     * @return true nếu gửi thành công, false nếu bị rate limit hoặc lỗi
     */
    @Transactional
    public SendOtpResult sendOtp(String email) {
        // 1. Kiểm tra rate limit
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(OTP_SEND_WINDOW_MINUTES);
        long sendCount = emailVerificationRepository.countByEmailAndCreatedAtAfter(email, windowStart);

        if (sendCount >= MAX_OTP_SENDS_PER_WINDOW) {
            log.warn("Rate limit exceeded for email: {}", email);
            return SendOtpResult.rateLimited(OTP_SEND_WINDOW_MINUTES);
        }

        // 2. Vô hiệu hóa OTP cũ
        emailVerificationRepository.invalidateAllOtpsByEmail(email);

        // 3. Tạo OTP mới
        String otp = generateOtp();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryDate = now.plusMinutes(OTP_EXPIRY_MINUTES);

        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .otp(otp)
                .expiryDate(expiryDate)
                .used(false)
                .attempts(0)
                .createdAt(now)
                .build();

        emailVerificationRepository.save(verification);

        // 4. Gửi email chứa OTP
        try {
            sendOtpEmail(email, otp);
            log.info("OTP sent successfully to: {}", email);
            return SendOtpResult.ok(OTP_EXPIRY_MINUTES);
        } catch (MailException e) {
            log.error("Failed to send OTP email to: {}", email, e);
            return SendOtpResult.emailError("Không thể gửi email. Vui lòng kiểm tra lại địa chỉ email.");
        }
    }

    /**
     * Xác thực OTP.
     *
     * @param email Email đã gửi OTP
     * @param otp OTP người dùng nhập
     * @return Kết quả xác thực
     */
    @Transactional
    public VerifyOtpResult verifyOtp(String email, String otp) {
        LocalDateTime now = LocalDateTime.now();

        Optional<EmailVerification> verificationOpt
                = emailVerificationRepository.findValidOtpByEmail(email, now);

        if (verificationOpt.isEmpty()) {
            log.warn("No valid OTP found for email: {}", email);
            return VerifyOtpResult.expired();
        }

        EmailVerification verification = verificationOpt.get();

        // Kiểm tra số lần thử
        if (verification.isMaxAttemptsReached()) {
            log.warn("Max attempts reached for email: {}", email);
            return VerifyOtpResult.maxAttemptsReached();
        }

        // Kiểm tra OTP
        if (!verification.getOtp().equals(otp)) {
            verification.setAttempts(verification.getAttempts() + 1);
            emailVerificationRepository.save(verification);

            int remainingAttempts = MAX_VERIFY_ATTEMPTS - verification.getAttempts();
            log.warn("Invalid OTP for email: {}. Remaining attempts: {}", email, remainingAttempts);

            if (remainingAttempts <= 0) {
                return VerifyOtpResult.maxAttemptsReached();
            }
            return VerifyOtpResult.invalid(remainingAttempts);
        }

        // OTP hợp lệ - đánh dấu đã sử dụng
        verification.setUsed(true);
        emailVerificationRepository.save(verification);

        log.info("OTP verified successfully for email: {}", email);
        return VerifyOtpResult.ok();
    }

    /**
     * Kiểm tra email đã được xác thực chưa (trong vòng 30 phút)
     */
    public boolean isEmailVerified(String email) {
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);

        Optional<EmailVerification> verification
                = emailVerificationRepository.findTopByEmailOrderByCreatedAtDesc(email);

        return verification.isPresent()
                && verification.get().getUsed()
                && verification.get().getCreatedAt().isAfter(thirtyMinutesAgo);
    }

    /**
     * Tạo OTP 6 số ngẫu nhiên
     */
    private String generateOtp() {
        int otp = secureRandom.nextInt(900000) + 100000; // 100000 - 999999
        return String.valueOf(otp);
    }

    /**
     * Gửi email chứa OTP
     */
    private void sendOtpEmail(String email, String otp) {
        String subject = "Mã xác thực email - Edu360";
        String htmlContent = String.format("""
            <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="text-align: center; margin-bottom: 30px;">
                    <h1 style="color: #6366f1; margin: 0;">Edu360</h1>
                </div>
                
                <div style="background: linear-gradient(135deg, #6366f1 0%%, #8b5cf6 100%%); padding: 30px; border-radius: 16px; text-align: center;">
                    <h2 style="color: white; margin: 0 0 10px 0; font-size: 20px;">Mã xác thực của bạn</h2>
                    <p style="color: rgba(255,255,255,0.9); margin: 0 0 20px 0; font-size: 14px;">
                        Vui lòng nhập mã sau để xác thực email của bạn
                    </p>
                    <div style="background: white; border-radius: 12px; padding: 20px; display: inline-block;">
                        <span style="font-size: 36px; font-weight: bold; letter-spacing: 8px; color: #1f2937;">%s</span>
                    </div>
                </div>
                
                <div style="margin-top: 20px; padding: 20px; background: #fef3c7; border-radius: 12px;">
                    <p style="margin: 0; color: #92400e; font-size: 14px;">
                        <strong>⚠️ Lưu ý:</strong>
                    </p>
                    <ul style="margin: 10px 0 0 0; padding-left: 20px; color: #92400e; font-size: 14px;">
                        <li>Mã này có hiệu lực trong <strong>%d phút</strong></li>
                        <li>Không chia sẻ mã này với bất kỳ ai</li>
                        <li>Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email</li>
                    </ul>
                </div>
                
                <div style="text-align: center; margin-top: 30px; color: #6b7280; font-size: 12px;">
                    <p>© 2025 Edu360 - Hệ thống quản lý giáo dục</p>
                </div>
            </div>
            """, otp, OTP_EXPIRY_MINUTES);

        emailService.sendHtmlMessage(email, subject, htmlContent);
    }

    /**
     * Job cleanup OTP hết hạn (chạy mỗi giờ)
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    @Transactional
    public void cleanupExpiredOtps() {
        emailVerificationRepository.deleteExpiredOtps(LocalDateTime.now());
        log.info("Cleaned up expired OTPs");
    }

    // ================= Result Classes =================
    public record SendOtpResult(boolean success, String message, int waitMinutes) {

        public static SendOtpResult ok(int expiryMinutes) {
            return new SendOtpResult(true, "Mã OTP đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư.", expiryMinutes);
        }

        public static SendOtpResult rateLimited(int waitMinutes) {
            return new SendOtpResult(false,
                    String.format("Bạn đã yêu cầu quá nhiều mã OTP. Vui lòng thử lại sau %d phút.", waitMinutes),
                    waitMinutes);
        }

        public static SendOtpResult emailError(String message) {
            return new SendOtpResult(false, message, 0);
        }
    }

    public record VerifyOtpResult(boolean success, String message, int remainingAttempts) {

        public static VerifyOtpResult ok() {
            return new VerifyOtpResult(true, "Xác thực email thành công!", 0);
        }

        public static VerifyOtpResult invalid(int remainingAttempts) {
            return new VerifyOtpResult(false,
                    String.format("Mã OTP không đúng. Bạn còn %d lần thử.", remainingAttempts),
                    remainingAttempts);
        }

        public static VerifyOtpResult expired() {
            return new VerifyOtpResult(false, "Mã OTP đã hết hạn hoặc không tồn tại. Vui lòng yêu cầu mã mới.", 0);
        }

        public static VerifyOtpResult maxAttemptsReached() {
            return new VerifyOtpResult(false, "Bạn đã nhập sai quá nhiều lần. Vui lòng yêu cầu mã mới.", 0);
        }
    }
}
