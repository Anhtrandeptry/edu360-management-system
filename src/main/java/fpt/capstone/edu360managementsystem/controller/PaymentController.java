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


    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long id) {
        PaymentResponse resp = paymentService.getPaymentById(id);
        return ResponseEntity.ok(resp);
    }


    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = paymentService.getPaymentStats();
        return ResponseEntity.ok(stats);
    }


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
