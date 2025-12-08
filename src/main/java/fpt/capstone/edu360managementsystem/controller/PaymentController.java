package fpt.capstone.edu360managementsystem.controller;

import fpt.capstone.edu360managementsystem.dto.request.CassoWebhookRequest;
import fpt.capstone.edu360managementsystem.dto.request.VietQrCallbackRequest;
import fpt.capstone.edu360managementsystem.dto.response.MessageResponse;
import fpt.capstone.edu360managementsystem.dto.response.PaymentCreateResponse;
import fpt.capstone.edu360managementsystem.dto.response.PaymentResponse;
import fpt.capstone.edu360managementsystem.enums.PaymentStatus;
import fpt.capstone.edu360managementsystem.service.PaymentService;
import fpt.capstone.edu360managementsystem.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    // Endpoint student get QR như trước
    @PostMapping("/class/{classId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<PaymentCreateResponse> createPayment(
            @PathVariable Long classId,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        PaymentCreateResponse resp = paymentService.createPaymentForClass(classId, user.getId());
        return ResponseEntity.ok(resp);
    }

    /**
     * Callback từ VietQR / bank:
     * Khi test bằng Postman bạn chỉ cần gửi JSON giống VietQrCallbackRequest.
     */
    @PostMapping("/vietqr/callback")
    public ResponseEntity<?> vietQrCallback(@RequestBody VietQrCallbackRequest body) {
        try {
            paymentService.handleVietQrCallback(body);
            return ResponseEntity.ok(new MessageResponse("Payment verified and student enrolled"));
        } catch (Exception ex) {
            // Có thể log chi tiết hơn
            return ResponseEntity.badRequest().body(new MessageResponse(ex.getMessage()));
        }
    }

    /**
     * Webhook từ Casso.vn - Tự động nhận biến động số dư ngân hàng.
     * 
     * Cách setup:
     * 1. Đăng ký tài khoản tại https://my.casso.vn
     * 2. Liên kết tài khoản ngân hàng (qua QR hoặc API)
     * 3. Vào Settings > Webhook > Thêm webhook URL: https://your-domain.com/api/payments/casso/webhook
     * 4. Casso sẽ tự động gửi POST khi có tiền vào tài khoản
     */
    @PostMapping("/casso/webhook")
    public ResponseEntity<?> cassoWebhook(@RequestBody CassoWebhookRequest body) {
        try {
            paymentService.handleCassoWebhook(body);
            return ResponseEntity.ok(new MessageResponse("OK"));
        } catch (Exception ex) {
            System.err.println("Casso webhook error: " + ex.getMessage());
            // Trả OK để Casso không retry liên tục
            return ResponseEntity.ok(new MessageResponse("Processed with error: " + ex.getMessage()));
        }
    }

    // ===================== ADMIN ENDPOINTS =====================

    /**
     * Admin: Lấy danh sách payment với filter và phân trang.
     * GET /api/payments?status=PENDING&studentName=...&classId=1&from=...&to=...&page=0&size=20
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<PaymentResponse>> listPayments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) String studentName,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<PaymentResponse> result = paymentService.getPayments(status, studentName, classId, from, to, page, size);
        return ResponseEntity.ok(result);
    }

    /**
     * Admin: Lấy chi tiết 1 payment.
     * GET /api/payments/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long id) {
        PaymentResponse resp = paymentService.getPaymentById(id);
        return ResponseEntity.ok(resp);
    }

    /**
     * Admin: Thống kê tổng quan payments.
     * GET /api/payments/stats
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = paymentService.getPaymentStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Admin: Xác nhận thanh toán thủ công (sau khi đối soát).
     * POST /api/payments/{id}/confirm
     */
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> confirmPayment(@PathVariable Long id) {
        paymentService.confirmPayment(id);
        return ResponseEntity.ok(new MessageResponse("Đã xác nhận thanh toán thành công"));
    }

    // ===================== STUDENT ENDPOINTS =====================

    /**
     * Student: Lấy lịch sử thanh toán của chính mình.
     * GET /api/payments/my-history?page=0&size=10
     */
    @GetMapping("/my-history")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Page<PaymentResponse>> getMyPaymentHistory(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<PaymentResponse> result = paymentService.getStudentPaymentHistory(user.getId(), page, size);
        return ResponseEntity.ok(result);
    }
}
