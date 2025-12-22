package fpt.capstone.edu360managementsystem.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fpt.capstone.edu360managementsystem.dto.request.CassoWebhookRequest;
import fpt.capstone.edu360managementsystem.dto.request.CassoWebhookV2Request;
import fpt.capstone.edu360managementsystem.dto.request.PayOSWebhookRequest;
import fpt.capstone.edu360managementsystem.dto.request.VietQrCallbackRequest;
import fpt.capstone.edu360managementsystem.dto.response.PaymentCreateResponse;
import fpt.capstone.edu360managementsystem.dto.response.PaymentResponse;
import fpt.capstone.edu360managementsystem.entity.Clazz;
import fpt.capstone.edu360managementsystem.entity.Payment;
import fpt.capstone.edu360managementsystem.entity.Student;
import fpt.capstone.edu360managementsystem.entity.Teacher;
import fpt.capstone.edu360managementsystem.entity.User;
import fpt.capstone.edu360managementsystem.enums.PaymentStatus;
import fpt.capstone.edu360managementsystem.repository.ClassSessionRepository;
import fpt.capstone.edu360managementsystem.repository.ClazzRepository;
import fpt.capstone.edu360managementsystem.repository.PaymentRepository;
import fpt.capstone.edu360managementsystem.repository.StudentRepository;
import fpt.capstone.edu360managementsystem.repository.UserRepository;
import jakarta.transaction.Transactional;

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

    @Value("${casso.webhookSecretToken:}")
    private String cassoWebhookSecretToken;

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

    @Autowired
    private UserRepository userRepository;

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
        
        // Ngân hàng có thể loại bỏ ký tự đặc biệt như #, -
        // Nên cần tìm theo nhiều pattern
        
        // Pattern 1: Tìm #PAY- (format gốc)
        int idx = content.lastIndexOf("#PAY-");
        if (idx != -1) {
            return content.substring(idx + 1).trim(); // cắt phần "#PAY-..."
        }
        
        // Pattern 2: Tìm PAY- (không có #)
        idx = content.lastIndexOf("PAY-");
        if (idx != -1) {
            return content.substring(idx).trim();
        }
        
        // Pattern 3: Tìm PAY (ngân hàng loại bỏ cả # và -)
        // Ví dụ: "thanh toan hoc phi PAY51234567890"
        idx = content.toUpperCase().lastIndexOf("PAY");
        if (idx != -1) {
            String extracted = content.substring(idx).trim();
            // Loại bỏ các ký tự không phải số và chữ sau PAY
            // Chuyển format PAY51234567890 -> PAY-5-1-234567890 để match với database
            // Hoặc giữ nguyên để so sánh linh hoạt hơn
            System.out.println("extractOrderCode: Found PAY pattern, extracted: " + extracted);
            return extracted;
        }
        
        return null;
    }
    
    /**
     * So sánh orderCode linh hoạt, bỏ qua ký tự đặc biệt
     * Ví dụ: "PAY-5-1-1234567890" matches "PAY51234567890"
     */
    private boolean orderCodeMatches(String dbOrderCode, String webhookOrderCode) {
        if (dbOrderCode == null || webhookOrderCode == null) {
            return false;
        }
        // Loại bỏ tất cả ký tự không phải chữ và số
        String normalizedDb = dbOrderCode.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        String normalizedWebhook = webhookOrderCode.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        
        return normalizedDb.equals(normalizedWebhook);
    }
    
    /**
     * Tìm payment theo orderCode một cách linh hoạt
     * Vì ngân hàng có thể loại bỏ ký tự đặc biệt (#, -) khỏi nội dung chuyển khoản
     * Ví dụ: DB lưu "PAY-5-9-1234567890", webhook nhận "PAY591234567890"
     * 
     * Cũng hỗ trợ match theo classId + studentId nếu timestamp khác
     * (user có thể chuyển tiền với nội dung cũ sau khi mở QR mới)
     */
    private Payment findPaymentByOrderCodeFlexible(String webhookOrderCode) {
        System.out.println("findPaymentByOrderCodeFlexible: Looking for " + webhookOrderCode);
        
        // 1. Thử tìm chính xác trước
        var exactMatch = paymentRepository.findByOrderCode(webhookOrderCode);
        if (exactMatch.isPresent()) {
            System.out.println("findPaymentByOrderCodeFlexible: Exact match found");
            return exactMatch.get();
        }
        
        // 2. Normalize webhook orderCode
        String normalizedWebhook = webhookOrderCode.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        System.out.println("findPaymentByOrderCodeFlexible: Normalized webhook = " + normalizedWebhook);
        
        // 3. Tìm trong tất cả payment PENDING và so sánh linh hoạt
        var pendingPayments = paymentRepository.findByStatus(PaymentStatus.PENDING);
        System.out.println("findPaymentByOrderCodeFlexible: Found " + pendingPayments.size() + " PENDING payments");
        
        for (Payment p : pendingPayments) {
            if (p.getOrderCode() != null) {
                String normalizedDb = p.getOrderCode().replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
                System.out.println("findPaymentByOrderCodeFlexible: Comparing DB=" + normalizedDb + " vs Webhook=" + normalizedWebhook);
                
                // So sánh chính xác
                if (normalizedDb.equals(normalizedWebhook)) {
                    System.out.println("findPaymentByOrderCodeFlexible: Full match found - paymentId=" + p.getId());
                    return p;
                }
                
                // So sánh theo prefix (PAY + classId + studentId) - bỏ qua timestamp
                // Format: PAY-{classId}-{studentId}-{timestamp}
                // Webhook có thể là: PAY{classId}{studentId}{timestamp} (không có dấu -)
                String dbPrefix = extractOrderCodePrefix(p.getOrderCode());
                String webhookPrefix = extractOrderCodePrefix(webhookOrderCode);
                
                if (dbPrefix != null && webhookPrefix != null && dbPrefix.equals(webhookPrefix)) {
                    System.out.println("findPaymentByOrderCodeFlexible: Prefix match found - paymentId=" + p.getId() 
                            + ", dbPrefix=" + dbPrefix + ", webhookPrefix=" + webhookPrefix);
                    return p;
                }
            }
        }
        
        System.out.println("findPaymentByOrderCodeFlexible: No match found");
        return null;
    }
    
    /**
     * Extract prefix từ orderCode (PAY + classId + studentId), bỏ timestamp
     * Input: PAY-56-17-1234567890 hoặc PAY5617xxxxxx
     * Output: PAY5617
     */
    private String extractOrderCodePrefix(String orderCode) {
        if (orderCode == null) return null;
        
        // Loại bỏ ký tự đặc biệt
        String normalized = orderCode.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        
        // Format: PAY{classId}{studentId}{timestamp}
        // Cần extract PAY + 2-3 số (classId) + 1-3 số (studentId)
        // Đơn giản: lấy 8-10 ký tự đầu (PAY + classId + studentId thường < 10 chars)
        if (normalized.startsWith("PAY") && normalized.length() >= 6) {
            // Tìm vị trí kết thúc của studentId
            // Giả sử classId và studentId đều < 1000, tổng cộng tối đa 6 digits
            // Lấy tối đa 10 ký tự đầu (PAY + 7 digits) để so sánh
            int prefixLen = Math.min(10, normalized.length());
            return normalized.substring(0, prefixLen);
        }
        return normalized;
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

    /**
     * Xử lý webhook từ Casso
     * Casso gửi webhook khi có giao dịch tiền vào tài khoản ngân hàng.
     * 
     * Luồng:
     * 1. Verify Secure Token từ header
     * 2. Duyệt qua danh sách giao dịch
     * 3. Tìm payment theo orderCode trong description
     * 4. Verify số tiền và cập nhật trạng thái PAID
     * 5. Tự động enroll student vào lớp
     * 
     * @param req Webhook payload từ Casso
     * @param authHeader Authorization header chứa Secure Token
     */
    @Transactional
    public void handleCassoWebhook(CassoWebhookRequest req, String authHeader) {
        System.out.println("📥 Casso webhook received");

        // 1. Verify Secure Token
        if (!verifyCassoSecureToken(authHeader)) {
            throw new RuntimeException("Invalid Casso Secure Token");
        }

        // 2. Kiểm tra error code
        if (req.getError() != null && req.getError() != 0) {
            System.err.println("Casso webhook: Error code = " + req.getError());
            return;
        }

        // 3. Kiểm tra có data không
        if (req.getData() == null || req.getData().isEmpty()) {
            System.out.println("Casso webhook: No transactions in data");
            return;
        }

        // 4. Xử lý từng giao dịch
        for (CassoWebhookRequest.CassoTransaction tx : req.getData()) {
            try {
                processCassoTransaction(tx);
            } catch (Exception e) {
                // Log lỗi nhưng tiếp tục xử lý các giao dịch khác
                System.err.println("Casso: Error processing transaction " + tx.getTid() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Verify Casso Secure Token (cho webhook V1 - legacy)
     */
    private boolean verifyCassoSecureToken(String authHeader) {
        if (cassoWebhookSecretToken == null || cassoWebhookSecretToken.isEmpty()) {
            System.out.println("Casso V1: Secret token not configured, skipping verification");
            return true;
        }

        if (authHeader == null || authHeader.isEmpty()) {
            System.err.println("Casso V1: No token received, rejecting request");
            return false;
        }

        // Casso gửi header dạng: "Apikey <token>" hoặc chỉ "<token>"
        String token = authHeader;
        if (authHeader.toLowerCase().startsWith("apikey ")) {
            token = authHeader.substring(7).trim();
        }

        boolean isValid = cassoWebhookSecretToken.equals(token);
        if (!isValid) {
            System.err.println("Casso V1: Invalid Secure Token");
        }
        return isValid;
    }

    /**
     * Verify Casso Signature (HMAC-SHA256)
     * Format: X-Casso-Signature: t=timestamp,v1=signature
     */
    private boolean verifyCassoSignature(String signatureHeader, String rawBody) {
        if (cassoWebhookSecretToken == null || cassoWebhookSecretToken.isEmpty()) {
            System.out.println("Casso: Secret token not configured, skipping verification");
            return true;
        }

        if (signatureHeader == null || signatureHeader.isEmpty()) {
            System.err.println("Casso: No signature received, rejecting request");
            return false;
        }

        try {
            // Parse signature header: t=timestamp,v1=signature
            String timestamp = null;
            String signature = null;
            
            String[] parts = signatureHeader.split(",");
            for (String part : parts) {
                if (part.startsWith("t=")) {
                    timestamp = part.substring(2);
                } else if (part.startsWith("v1=")) {
                    signature = part.substring(3);
                }
            }

            if (timestamp == null || signature == null) {
                System.err.println("Casso: Invalid signature format: " + signatureHeader);
                return false;
            }

            // Tạo payload = timestamp.body
            String payload = timestamp + "." + rawBody;
            
            // Tính HMAC-SHA256
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(
                    cassoWebhookSecretToken.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            // Convert to hex
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            String computedSignature = hexString.toString();

            boolean isValid = computedSignature.equals(signature);
            if (isValid) {
                System.out.println("✅ Casso: Signature verified successfully");
            } else {
                System.err.println("Casso: Signature mismatch!");
                System.err.println("  - Received: " + signature);
                System.err.println("  - Computed: " + computedSignature);
            }
            return isValid;
        } catch (Exception e) {
            System.err.println("Casso: Error verifying signature: " + e.getMessage());
            return false;
        }
    }

    /**
     * Xử lý một giao dịch từ Casso
     */
    private void processCassoTransaction(CassoWebhookRequest.CassoTransaction tx) {
        System.out.println("Casso: Processing transaction - tid=" + tx.getTid() 
                + ", amount=" + tx.getAmount() 
                + ", desc=" + tx.getDescription());

        // 1. Tìm orderCode trong description
        String orderCode = extractOrderCode(tx.getDescription());
        if (orderCode == null) {
            System.out.println("Casso: No orderCode found in description: " + tx.getDescription());
            return;
        }

        // 2. Tìm payment theo orderCode
        var paymentOpt = paymentRepository.findByOrderCode(orderCode);
        if (paymentOpt.isEmpty()) {
            System.out.println("Casso: Payment not found for orderCode: " + orderCode);
            return;
        }

        Payment payment = paymentOpt.get();

        // 3. Kiểm tra đã xử lý chưa
        if (payment.getStatus() == PaymentStatus.PAID) {
            System.out.println("Casso: Payment already PAID for orderCode: " + orderCode);
            return;
        }

        // 4. Verify số tiền
        if (tx.getAmount() == null || !tx.getAmount().equals(payment.getAmount())) {
            System.err.println("Casso: Amount mismatch for " + orderCode 
                    + ". Expected: " + payment.getAmount() 
                    + ", Got: " + tx.getAmount());
            payment.setStatus(PaymentStatus.FAILED);
            payment.setBankTransactionId(tx.getTid());
            paymentRepository.save(payment);
            return;
        }

        // 5. Mark PAID
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setBankTransactionId(tx.getTid());
        paymentRepository.save(payment);

        System.out.println("✅ Casso: Payment confirmed for orderCode: " + orderCode);

        // 6. Gửi thông báo thanh toán thành công
        try {
            notificationService.notifyPaymentSuccess(
                    payment.getStudent().getUser().getId(),
                    payment.getClazz().getName(),
                    payment.getAmount()
            );
            System.out.println("Casso: Notification sent to student: " + payment.getStudent().getUser().getId());
        } catch (Exception e) {
            System.err.println("Casso: Failed to send notification: " + e.getMessage());
        }

        // 7. Tự động enroll student vào lớp
        try {
            enrollmentService.enrollAfterPayment(
                    payment.getClazz().getId(),
                    payment.getStudent().getId()
            );
            System.out.println("✅ Casso: Student auto-enrolled for class: " + payment.getClazz().getId());
        } catch (Exception e) {
            System.err.println("Casso: Auto-enroll failed: " + e.getMessage());
        }
    }

    /**
     * Xử lý webhook V2 từ Casso (data là object thay vì array)
     */
    @Transactional
    public void handleCassoWebhookV2(CassoWebhookV2Request req, String signatureHeader, String rawBody) {
        System.out.println("📥 Casso webhook V2 processing...");
        System.out.println("📥 Casso V2 - signature: [" + signatureHeader + "]");

        // 1. Verify Signature (HMAC-SHA256)
        if (!verifyCassoSignature(signatureHeader, rawBody)) {
            throw new RuntimeException("Invalid Casso Signature");
        }

        // 2. Kiểm tra error code
        if (req.getError() != null && req.getError() != 0) {
            System.err.println("Casso V2 webhook: Error code = " + req.getError());
            return;
        }

        // 3. Kiểm tra có data không
        if (req.getData() == null) {
            System.out.println("Casso V2 webhook: No transaction data");
            return;
        }

        // 4. Xử lý giao dịch
        processCassoTransactionV2(req.getData());
    }

    /**
     * Xử lý một giao dịch từ Casso V2
     */
    private void processCassoTransactionV2(CassoWebhookV2Request.CassoTransactionV2 tx) {
        System.out.println("Casso V2: Processing transaction - ref=" + tx.getReference() 
                + ", amount=" + tx.getAmount() 
                + ", desc=" + tx.getDescription());

        // 1. Tìm orderCode trong description
        String orderCode = extractOrderCode(tx.getDescription());
        if (orderCode == null) {
            System.out.println("Casso V2: No orderCode found in description: " + tx.getDescription());
            return;
        }
        System.out.println("Casso V2: Extracted orderCode: " + orderCode);

        // 2. Tìm payment theo orderCode (so sánh linh hoạt vì ngân hàng loại bỏ ký tự đặc biệt)
        Payment payment = findPaymentByOrderCodeFlexible(orderCode);
        if (payment == null) {
            System.out.println("Casso V2: Payment not found for orderCode: " + orderCode);
            return;
        }
        System.out.println("Casso V2: Found payment ID=" + payment.getId() + ", dbOrderCode=" + payment.getOrderCode());

        // 3. Kiểm tra đã xử lý chưa
        if (payment.getStatus() == PaymentStatus.PAID) {
            System.out.println("Casso V2: Payment already PAID for orderCode: " + orderCode);
            return;
        }

        // 4. Verify số tiền
        if (tx.getAmount() == null || !tx.getAmount().equals(payment.getAmount())) {
            System.err.println("Casso V2: Amount mismatch for " + orderCode 
                    + ". Expected: " + payment.getAmount() 
                    + ", Got: " + tx.getAmount());
            payment.setStatus(PaymentStatus.FAILED);
            payment.setBankTransactionId(tx.getReference());
            paymentRepository.save(payment);
            return;
        }

        // 5. Mark PAID
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setBankTransactionId(tx.getReference());
        paymentRepository.save(payment);

        System.out.println("✅ Casso V2: Payment confirmed for orderCode: " + orderCode);

        // 6. Gửi thông báo thanh toán thành công
        try {
            notificationService.notifyPaymentSuccess(
                    payment.getStudent().getUser().getId(),
                    payment.getClazz().getName(),
                    payment.getAmount()
            );
            System.out.println("Casso V2: Notification sent to student: " + payment.getStudent().getUser().getId());
        } catch (Exception e) {
            System.err.println("Casso V2: Failed to send notification: " + e.getMessage());
        }

        // 7. Tự động enroll student vào lớp
        try {
            enrollmentService.enrollAfterPayment(
                    payment.getClazz().getId(),
                    payment.getStudent().getId()
            );
            System.out.println("✅ Casso V2: Student auto-enrolled for class: " + payment.getClazz().getId());
        } catch (Exception e) {
            System.err.println("Casso V2: Auto-enroll failed: " + e.getMessage());
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

    /**
     * Check payment status for polling from frontend.
     * Allows student to check their own payment status.
     *
     * @param paymentId the payment ID
     * @param userId the authenticated user ID
     * @return payment status information map
     */
    public Map<String, Object> checkPaymentStatus(Long paymentId, Long userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        // Verify that the payment belongs to this user (for STUDENT role)
        if (!payment.getStudent().getUser().getId().equals(userId)) {
            // Check if user is admin - admins can check any payment
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            boolean isAdmin = user.getRoles().stream()
                    .anyMatch(role -> role.getName().name().equals("ROLE_ADMIN"));
            if (!isAdmin) {
                throw new RuntimeException("You can only check your own payment status");
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("paymentId", payment.getId());
        result.put("status", payment.getStatus().name());
        result.put("isPaid", payment.getStatus() == PaymentStatus.PAID);
        result.put("className", payment.getClazz().getName());
        result.put("amount", payment.getAmount());
        
        if (payment.getPaidAt() != null) {
            result.put("paidAt", payment.getPaidAt().toString());
        }
        
        if (payment.getBankTransactionId() != null) {
            result.put("bankTransactionId", payment.getBankTransactionId());
        }

        return result;
    }
}
