package fpt.capstone.edu360managementsystem.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import fpt.capstone.edu360managementsystem.entity.Payment;
import fpt.capstone.edu360managementsystem.enums.PaymentStatus;
import fpt.capstone.edu360managementsystem.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled job để tự động hủy các payment PENDING đã hết hạn.
 * 
 * Logic:
 * - Chạy mỗi 5 phút
 * - Tìm các payment PENDING được tạo cách đây hơn 15 phút
 * - Cập nhật status thành CANCELLED
 * - Điều này giúp giải phóng slot cho người khác đăng ký
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiredPaymentScheduler {

    private final PaymentRepository paymentRepository;

    // Thời gian hết hạn payment (phút)
    private static final int PAYMENT_EXPIRY_MINUTES = 15;

    /**
     * Chạy mỗi 5 phút để kiểm tra và hủy payment hết hạn
     * Cron: giây phút giờ ngày tháng thứ
     */
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void cancelExpiredPayments() {
        log.info(" [ExpiredPaymentScheduler] Checking for expired PENDING payments...");

        LocalDateTime expiryThreshold = LocalDateTime.now().minusMinutes(PAYMENT_EXPIRY_MINUTES);
        
        List<Payment> expiredPayments = paymentRepository.findExpiredPendingPayments(expiryThreshold);

        if (expiredPayments.isEmpty()) {
            log.info(" [ExpiredPaymentScheduler] No expired payments found.");
            return;
        }

        log.info(" [ExpiredPaymentScheduler] Found {} expired payments. Cancelling...", expiredPayments.size());

        for (Payment payment : expiredPayments) {
            payment.setStatus(PaymentStatus.CANCELLED);
            paymentRepository.save(payment);
            
            log.info(" [ExpiredPaymentScheduler] Cancelled payment ID: {}, Student: {}, Class: {}", 
                    payment.getId(),
                    payment.getStudent().getUser().getFullName(),
                    payment.getClazz().getName());
        }

        log.info(" [ExpiredPaymentScheduler] Cancelled {} expired payments.", expiredPayments.size());
    }
}
