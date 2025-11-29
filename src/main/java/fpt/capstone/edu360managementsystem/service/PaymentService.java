package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.request.VietQrCallbackRequest;
import fpt.capstone.edu360managementsystem.dto.response.PaymentCreateResponse;
import fpt.capstone.edu360managementsystem.entity.Clazz;
import fpt.capstone.edu360managementsystem.entity.Payment;
import fpt.capstone.edu360managementsystem.entity.Student;
import fpt.capstone.edu360managementsystem.enums.PaymentStatus;
import fpt.capstone.edu360managementsystem.repository.ClassSessionRepository;
import fpt.capstone.edu360managementsystem.repository.ClazzRepository;
import fpt.capstone.edu360managementsystem.repository.PaymentRepository;
import fpt.capstone.edu360managementsystem.repository.StudentRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
public class PaymentService {

    @Value("${payment.vietqr.bankCode}")
    private String bankCode;

    @Value("${payment.vietqr.accountNumber}")
    private String accountNumber;

    @Value("${payment.vietqr.accountName}")
    private String accountName;

    @Value("${payment.vietqr.templateId}")
    private String templateId;

    @Value("${payment.vietqr.baseUrl}")
    private String baseUrl;

    @Autowired
    private ClazzRepository clazzRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ClassSessionRepository classSessionRepository;

    @Autowired
    private EnrollmentService enrollmentService;

    /**
     * Tạo Payment cho 1 lớp (mỗi học sinh - mỗi lớp 1 payment).
     * Sử dụng VietQR để generate link QR có sẵn amount + content.
     */
    @Transactional
    public PaymentCreateResponse createPaymentForClass(Long classId, Long userId) {
        Clazz clazz = clazzRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        Student student = studentRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Student profile not found"));

        long sessionsCount = classSessionRepository.countByClazz_Id(classId);
        if (sessionsCount <= 0) {
            throw new RuntimeException("Class has no sessions to calculate tuition");
        }

        if (clazz.getPricePerSession() == null || clazz.getPricePerSession() <= 0) {
            throw new RuntimeException("Class pricePerSession is not configured");
        }

        long amount = clazz.getPricePerSession() * sessionsCount;

        // Tạo/hoặc lấy payment cũ
        Payment payment = paymentRepository.findByClazz_IdAndStudent_Id(classId, student.getId())
                .orElse(null);

        if (payment == null) {
            String orderCode = generateOrderCode(clazz.getId(), student.getId());

            // Nội dung: Tên + " thanh toan hoc phi " + mã order
            String content = student.getUser().getFullName()
                    + " thanh toan hoc phi "
                    + "#" + orderCode;

            payment = Payment.builder()
                    .clazz(clazz)
                    .student(student)
                    .amount(amount)
                    .content(content)
                    .status(PaymentStatus.PENDING)
                    .orderCode(orderCode)
                    .createdAt(LocalDateTime.now())
                    .build();
        } else {
            // update thông tin nếu cần
            payment.setAmount(amount);
            payment.setStatus(PaymentStatus.PENDING);
            // KHÔNG đổi orderCode để nội dung cũ vẫn dùng được
        }

        payment = paymentRepository.save(payment);

        // Build QR với content đã có trong payment
        String qrUrl = buildVietQrUrl(payment.getAmount(), payment.getContent());

        return PaymentCreateResponse.builder()
                .paymentId(payment.getId())
                .classId(clazz.getId())
                .studentId(student.getId())
                .amount(payment.getAmount())
                .content(payment.getContent())
                .qrImageUrl(qrUrl)
                .build();
    }

    private String generateOrderCode(Long classId, Long studentId) {
        return "PAY-" + classId + "-" + studentId + "-" + System.currentTimeMillis();
    }

    /**
     * Admin xác nhận đã thanh toán (sau khi check sao kê).
     * Có thể mở rộng: auto enroll vào lớp.
     */
    @Transactional
    public void confirmPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // TODO (optional): tự động enroll học sinh vào lớp sau khi thanh toán
        // Ví dụ:
        // enrollmentService.selfEnroll(payment.getClazz().getId(), payment.getStudent().getUser().getId());
    }



    /**
     * Build URL VietQR để FE show <img src="...">
     */
    private String buildVietQrUrl(long amount, String content) {
        String encodedContent = URLEncoder.encode(content, StandardCharsets.UTF_8);
        String encodedAccountName = URLEncoder.encode(accountName, StandardCharsets.UTF_8);

        // Ví dụ: https://api.vietqr.io/image/{bankCode}-{accountNumber}-{template}.png?amount=...&addInfo=...&accountName=...
        return String.format(
                "%s/%s-%s-%s.png?amount=%d&addInfo=%s&accountName=%s",
                baseUrl,
                bankCode,
                accountNumber,
                templateId,
                amount,
                encodedContent,
                encodedAccountName
        );
    }

    @Transactional
    public void handleVietQrCallback(VietQrCallbackRequest req) {
        // 1) Đúng tài khoản nhận tiền?
        if (req.getAccountNumber() == null ||
                !req.getAccountNumber().equals(accountNumber)) {
            throw new RuntimeException("Sai tài khoản nhận tiền");
        }

        if (req.getAmount() == null || req.getAmount() <= 0) {
            throw new RuntimeException("Số tiền không hợp lệ");
        }

        // 2) Lấy orderCode từ content (sau dấu #)
        String orderCode = extractOrderCode(req.getContent());
        if (orderCode == null) {
            throw new RuntimeException("Không tìm thấy mã order trong nội dung chuyển khoản");
        }

        Payment payment = paymentRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Payment không tồn tại cho orderCode: " + orderCode));

        // 3) Check số tiền
        if (!req.getAmount().equals(payment.getAmount())) {
            // Sai số tiền => mark FAILED, không enroll
            payment.setStatus(PaymentStatus.FAILED);
            payment.setBankTransactionId(req.getTransactionId());
            paymentRepository.save(payment);
            throw new RuntimeException("Số tiền thanh toán không khớp. Yêu cầu: "
                    + payment.getAmount() + ", thực tế: " + req.getAmount());
        }

        // 4) Đúng tiền => mark PAID + set paidAt + lưu mã giao dịch
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setBankTransactionId(req.getTransactionId());
        paymentRepository.save(payment);

        // 5) TỰ ĐỘNG ENROLL
        Long classId = payment.getClazz().getId();
        Long userId = payment.getStudent().getUser().getId();
        enrollmentService.selfEnroll(classId, userId);
    }

    private String extractOrderCode(String content) {
        if (content == null) return null;
        int idx = content.lastIndexOf("#PAY-");
        if (idx == -1) return null;
        return content.substring(idx + 1).trim(); // cắt phần "#PAY-..."
    }
}
