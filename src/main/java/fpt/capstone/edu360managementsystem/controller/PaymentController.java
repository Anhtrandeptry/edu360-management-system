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

/**
 * REST controller for payment management.
 * Provides endpoints for payment creation, webhooks, and payment history.
 *
 * @author 360edu
 * @version 1.0
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    /**
     * Creates a payment and generates QR code for class enrollment.
     *
     * @param classId the class ID to pay for
     * @param user    the authenticated student
     * @return payment response with QR code
     */
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
     * Handles VietQR payment callback.
     *
     * @param body the callback request data
     * @return success or error message
     */
    @PostMapping("/vietqr/callback")
    public ResponseEntity<?> vietQrCallback(@RequestBody VietQrCallbackRequest body) {
        try {
            paymentService.handleVietQrCallback(body);
            return ResponseEntity.ok(new MessageResponse("Payment verified and student enrolled"));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(new MessageResponse(ex.getMessage()));
        }
    }

    /**
     * Handles Casso payment webhook.
     *
     * @param body the webhook request data
     * @return acknowledgement message
     */
    @PostMapping("/casso/webhook")
    public ResponseEntity<?> cassoWebhook(@RequestBody CassoWebhookRequest body) {
        try {
            paymentService.handleCassoWebhook(body);
            return ResponseEntity.ok(new MessageResponse("OK"));
        } catch (Exception ex) {
            System.err.println("Casso webhook error: " + ex.getMessage());
            return ResponseEntity.ok(new MessageResponse("Processed with error: " + ex.getMessage()));
        }
    }

    /**
     * Lists all payments with filters and pagination.
     *
     * @param status  optional status filter
     * @param search  optional search (student name or class name)
     * @param classId optional class filter
     * @param from    optional start date filter
     * @param to      optional end date filter
     * @param page    page number
     * @param size    page size
     * @return paginated payment list
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<PaymentResponse>> listPayments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<PaymentResponse> result = paymentService.getPayments(status, search, classId, from, to, page, size);
        return ResponseEntity.ok(result);
    }

    /**
     * Retrieves payment details by ID.
     *
     * @param id the payment ID
     * @return payment details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long id) {
        PaymentResponse resp = paymentService.getPaymentById(id);
        return ResponseEntity.ok(resp);
    }

    /**
     * Retrieves payment statistics.
     *
     * @return payment statistics
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = paymentService.getPaymentStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Manually confirms a payment.
     *
     * @param id the payment ID
     * @return success message
     */
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> confirmPayment(@PathVariable Long id) {
        paymentService.confirmPayment(id);
        return ResponseEntity.ok(new MessageResponse("Đã xác nhận thanh toán thành công"));
    }

    /**
     * Retrieves payment history for the authenticated student.
     *
     * @param user the authenticated student
     * @param page page number
     * @param size page size
     * @return paginated payment history
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
