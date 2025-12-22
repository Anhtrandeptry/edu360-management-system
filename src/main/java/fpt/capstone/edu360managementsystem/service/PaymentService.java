package fpt.capstone.edu360managementsystem.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fpt.capstone.edu360managementsystem.dto.request.PayOSWebhookRequest;
import fpt.capstone.edu360managementsystem.dto.request.VietQrCallbackRequest;
import fpt.capstone.edu360managementsystem.dto.response.PaymentCreateResponse;
import fpt.capstone.edu360managementsystem.dto.response.PaymentResponse;
import fpt.capstone.edu360managementsystem.entity.Clazz;
import fpt.capstone.edu360managementsystem.entity.Payment;
import fpt.capstone.edu360managementsystem.entity.Student;
import fpt.capstone.edu360managementsystem.entity.Teacher;
import fpt.capstone.edu360managementsystem.enums.PaymentStatus;
import fpt.capstone.edu360managementsystem.repository.ClassSessionRepository;
import fpt.capstone.edu360managementsystem.repository.ClazzRepository;
import fpt.capstone.edu360managementsystem.repository.PaymentRepository;
import fpt.capstone.edu360managementsystem.repository.StudentRepository;
import jakarta.transaction.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

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

    @Value("${payos.checksumKey:}")
    private String payosChecksumKey;

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

    @Autowired
    private NotificationService notificationService;

    /**
     * Tạo Payment cho 1 lớp (mỗi học sinh - mỗi lớp 1 payment). Sử dụng VietQR
     * để generate link QR có sẵn amount + content.
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

        // Gửi thông báo cho tất cả Admin mỗi khi student mở QR thanh toán
        // (Kể cả payment cũ - để admin biết student đang chờ thanh toán)
        try {
            System.out.println("📢 Sending notification to admins for payment: " + payment.getId());
            notificationService.notifyAdminsNewPaymentPending(
                    student.getUser().getFullName(),
                    clazz.getName(),
                    payment.getAmount(),
                    payment.getId()
            );
            System.out.println("✅ Notification sent successfully!");
        } catch (Exception e) {
            // Không throw lỗi nếu gửi notification thất bại
            System.err.println("❌ Failed to notify admins about new payment: " + e.getMessage());
            e.printStackTrace();
        }

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
     * Admin xác nhận đã thanh toán (sau khi check sao kê). Tự động enroll học
     * sinh vào lớp sau khi xác nhận.
     */
    @Transactional
    public void confirmPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        // Chỉ xác nhận nếu đang PENDING
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new RuntimeException("Payment đã được xác nhận trước đó");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        //Gửi thông báo thanh toán thành công
        try {
            notificationService.notifyPaymentSuccess(
                    payment.getStudent().getUser().getId(),
                    payment.getClazz().getName(),
                    payment.getAmount()
            );
        } catch (Exception e) {
            System.err.println("Failed to send payment notification: " + e.getMessage());
        }

        //Tự động enroll học sinh vào lớp sau khi thanh toán được xác nhận
        try {
            enrollmentService.enrollAfterPayment(
                    payment.getClazz().getId(),
                    payment.getStudent().getId()
            );
        } catch (Exception e) {
            // Log lỗi nhưng không rollback payment confirmation
            // Vì payment đã thành công, chỉ là auto-enroll bị lỗi (ví dụ: lớp đầy)
            System.err.println("Auto-enroll failed after payment confirmation: " + e.getMessage());
        }
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
        if (req.getAccountNumber() == null
                || !req.getAccountNumber().equals(accountNumber)) {
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

        // 5) TỰ ĐỘNG ENROLL sau khi thanh toán thành công
        try {
            enrollmentService.enrollAfterPayment(
                    payment.getClazz().getId(),
                    payment.getStudent().getId()
            );
        } catch (Exception e) {
            // Log lỗi nhưng không rollback payment
            System.err.println("Auto-enroll failed after VietQR callback: " + e.getMessage());
        }
    }

    private String extractOrderCode(String content) {
        if (content == null) {
            return null;
        }
        int idx = content.lastIndexOf("#PAY-");
        if (idx == -1) {
            return null;
        }
        return content.substring(idx + 1).trim(); // cắt phần "#PAY-..."
    }

    /**
     * Xử lý webhook từ PayOS
     * PayOS sẽ gửi webhook khi có giao dịch thanh toán thành công.
     * 
     * Luồng:
     * 1. Nhận webhook từ PayOS
     * 2. Verify signature với Checksum Key
     * 3. Tìm payment theo orderCode trong description
     * 4. Verify số tiền và cập nhật trạng thái PAID
     * 5. Tự động enroll student vào lớp
     */
    @Transactional
    public void handlePayOSWebhook(PayOSWebhookRequest req) {
        // Kiểm tra response thành công
        if (req.getCode() == null || !"00".equals(req.getCode())) {
            System.err.println("PayOS webhook: Transaction not successful, code: " + req.getCode());
            return;
        }

        if (req.getData() == null) {
            System.err.println("PayOS webhook: No transaction data");
            return;
        }

        PayOSWebhookRequest.PayOSTransactionData tx = req.getData();

        // Tìm orderCode trong description (nội dung chuyển khoản)
        String orderCode = extractOrderCode(tx.getDescription());
        if (orderCode == null) {
            System.out.println("PayOS: No orderCode found in description: " + tx.getDescription());
            return;
        }

        // Tìm payment theo orderCode
        var paymentOpt = paymentRepository.findByOrderCode(orderCode);
        if (paymentOpt.isEmpty()) {
            System.out.println("PayOS: Payment not found for orderCode: " + orderCode);
            return;
        }

        Payment payment = paymentOpt.get();

        // Đã xử lý rồi thì bỏ qua
        if (payment.getStatus() == PaymentStatus.PAID) {
            System.out.println("PayOS: Payment already processed for orderCode: " + orderCode);
            return;
        }

        // Verify số tiền
        if (tx.getAmount() != null && !tx.getAmount().equals(payment.getAmount())) {
            System.err.println("PayOS: Amount mismatch for " + orderCode + 
                    ". Expected: " + payment.getAmount() + ", Got: " + tx.getAmount());
            payment.setStatus(PaymentStatus.FAILED);
            payment.setBankTransactionId(tx.getReference());
            paymentRepository.save(payment);
            return;
        }

        // Đúng tiền => Mark PAID
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setBankTransactionId(tx.getReference());
        paymentRepository.save(payment);

        System.out.println("PayOS: Payment confirmed for orderCode: " + orderCode);

        // Gửi thông báo thanh toán thành công
        try {
            notificationService.notifyPaymentSuccess(
                    payment.getStudent().getUser().getId(),
                    payment.getClazz().getName(),
                    payment.getAmount()
            );
            System.out.println("PayOS: Payment notification sent to student: " + payment.getStudent().getUser().getId());
        } catch (Exception e) {
            System.err.println("PayOS: Failed to send payment notification: " + e.getMessage());
        }

        // Tự động enroll student
        try {
            enrollmentService.enrollAfterPayment(
                    payment.getClazz().getId(),
                    payment.getStudent().getId()
            );
            System.out.println("PayOS: Student auto-enrolled for class: " + payment.getClazz().getId());
        } catch (Exception e) {
            System.err.println("PayOS: Auto-enroll failed: " + e.getMessage());
        }
    }

    /**
     * Verify PayOS webhook signature
     */
    public boolean verifyPayOSSignature(String data, String signature) {
        if (payosChecksumKey == null || payosChecksumKey.isEmpty()) {
            System.out.println("PayOS: Checksum key not configured, skipping signature verification");
            return true;
        }
        
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(payosChecksumKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString().equals(signature);
        } catch (Exception e) {
            System.err.println("PayOS: Signature verification failed: " + e.getMessage());
            return false;
        }
    }

    // ===================== ADMIN METHODS =====================
    /**
     * Admin: Lấy danh sách payment với filter và phân trang.
     */
    public Page<PaymentResponse> getPayments(
            PaymentStatus status,
            String search,
            Long classId,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        // Validate sort field - chỉ cho phép createdAt hoặc paidAt
        if (sortBy == null || (!sortBy.equals("createdAt") && !sortBy.equals("paidAt"))) {
            sortBy = "createdAt";
        }

        // Tạo Sort object
        org.springframework.data.domain.Sort sort = sortDir != null && sortDir.equalsIgnoreCase("asc")
                ? org.springframework.data.domain.Sort.by(sortBy).ascending()
                : org.springframework.data.domain.Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Payment> payments = paymentRepository.findAllWithFilters(status, search, classId, from, to, pageable);
        return payments.map(this::mapToResponse);
    }

    /**
     * Admin: Lấy chi tiết 1 payment.
     */
    public PaymentResponse getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
        return mapToResponse(payment);
    }

    /**
     * Admin: Thống kê tổng quan.
     */
    public Map<String, Object> getPaymentStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPaidAmount", paymentRepository.sumPaidAmount());
        stats.put("pendingCount", paymentRepository.countByStatus(PaymentStatus.PENDING));
        stats.put("paidCount", paymentRepository.countByStatus(PaymentStatus.PAID));
        stats.put("failedCount", paymentRepository.countByStatus(PaymentStatus.FAILED));
        stats.put("totalCount", paymentRepository.count());
        return stats;
    }

    /**
     * Student: Lấy lịch sử thanh toán của chính mình.
     */
    public Page<PaymentResponse> getStudentPaymentHistory(Long userId, int page, int size) {
        Student student = studentRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Student profile not found"));

        Pageable pageable = PageRequest.of(page, size, org.springframework.data.domain.Sort.by("createdAt").descending());
        Page<Payment> payments = paymentRepository.findByStudent_Id(student.getId(), pageable);
        return payments.map(this::mapToResponse);
    }

    /**
     * Map Payment entity -> PaymentResponse DTO
     */
    private PaymentResponse mapToResponse(Payment p) {
        Student student = p.getStudent();
        Clazz clazz = p.getClazz();
        Teacher teacher = clazz.getTeacher();

        String teacherName = teacher != null && teacher.getUser() != null
                ? teacher.getUser().getFullName()
                : "";
        String subjectName = clazz.getSubject() != null
                ? clazz.getSubject().getName()
                : "";

        return PaymentResponse.builder()
                .id(p.getId())
                .studentId(student.getId())
                .studentUserId(student.getUser().getId())
                .studentName(student.getUser().getFullName())
                .studentEmail(student.getUser().getEmail())
                .studentPhone(student.getUser().getPhoneNumber())
                .classId(clazz.getId())
                .className(clazz.getName())
                .teacherName(teacherName)
                .subjectName(subjectName)
                .amount(p.getAmount())
                .content(p.getContent())
                .orderCode(p.getOrderCode())
                .status(p.getStatus())
                .bankTransactionId(p.getBankTransactionId())
                .createdAt(p.getCreatedAt())
                .paidAt(p.getPaidAt())
                .build();
    }
}
