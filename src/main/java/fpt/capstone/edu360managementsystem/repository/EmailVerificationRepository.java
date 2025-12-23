package fpt.capstone.edu360managementsystem.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import fpt.capstone.edu360managementsystem.entity.EmailVerification;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    /**
     * Tìm OTP chưa sử dụng và còn hiệu lực cho email
     */
    @Query("SELECT e FROM EmailVerification e WHERE e.email = :email AND e.used = false AND e.expiryDate > :now ORDER BY e.createdAt DESC")
    Optional<EmailVerification> findValidOtpByEmail(String email, LocalDateTime now);

    /**
     * Tìm OTP mới nhất của email (bất kể trạng thái)
     */
    Optional<EmailVerification> findTopByEmailOrderByCreatedAtDesc(String email);

    /**
     * Đếm số OTP đã gửi trong khoảng thời gian (rate limiting)
     */
    @Query("SELECT COUNT(e) FROM EmailVerification e WHERE e.email = :email AND e.createdAt > :since")
    long countByEmailAndCreatedAtAfter(String email, LocalDateTime since);

    /**
     * Xóa các OTP hết hạn (cleanup job)
     */
    @Modifying
    @Query("DELETE FROM EmailVerification e WHERE e.expiryDate < :now")
    void deleteExpiredOtps(LocalDateTime now);

    /**
     * Vô hiệu hóa tất cả OTP cũ của email (khi tạo OTP mới)
     */
    @Modifying
    @Query("UPDATE EmailVerification e SET e.used = true WHERE e.email = :email AND e.used = false")
    void invalidateAllOtpsByEmail(String email);

    /**
     * Xóa tất cả OTP của email (dùng cho testing/reset)
     */
    @Modifying
    @Query("DELETE FROM EmailVerification e WHERE e.email = :email")
    void deleteAllByEmail(String email);

    /**
     * Xóa tất cả OTP (dùng cho testing/reset)
     */
    @Modifying
    @Query("DELETE FROM EmailVerification e")
    void deleteAllOtps();
}
