package fpt.capstone.edu360managementsystem.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity lưu trữ OTP xác thực email khi đăng ký. OTP có thời hạn 5 phút và chỉ
 * được sử dụng 1 lần.
 *
 * @author 360edu
 * @version 1.0
 */
@Entity
@Table(name = "email_verifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 6)
    private String otp;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Column(name = "used", nullable = false)
    @Builder.Default
    private Boolean used = false;

    @Column(name = "attempts", nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Kiểm tra OTP còn hiệu lực không
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    /**
     * Kiểm tra số lần nhập sai đã vượt quá giới hạn chưa (5 lần)
     */
    public boolean isMaxAttemptsReached() {
        return attempts >= 5;
    }
}
