package fpt.capstone.edu360managementsystem.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import fpt.capstone.edu360managementsystem.dto.request.CassoWebhookRequest;
import fpt.capstone.edu360managementsystem.dto.request.CassoWebhookV2Request;
import fpt.capstone.edu360managementsystem.dto.request.PayOSWebhookRequest;
import fpt.capstone.edu360managementsystem.dto.request.VietQrCallbackRequest;
import fpt.capstone.edu360managementsystem.dto.response.MessageResponse;
import fpt.capstone.edu360managementsystem.dto.response.PaymentCreateResponse;
import fpt.capstone.edu360managementsystem.dto.response.PaymentResponse;
import fpt.capstone.edu360managementsystem.enums.PaymentStatus;
import fpt.capstone.edu360managementsystem.service.PaymentService;
import fpt.capstone.edu360managementsystem.service.UserDetailsImpl;

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
        System.out.println(" [PaymentController] createPayment called - classId=" + classId + ", userId=" + user.getId());
        PaymentCreateResponse resp = paymentService.createPaymentForClass(classId, user.getId());
        System.out.println(" [PaymentController] Payment created - paymentId=" + resp.getPaymentId());
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
     * Handles PayOS payment webhook.
     * PayOS sẽ gửi webhook mỗi khi có giao dịch thanh toán thành công.
     * Luồng tự động: PayOS webhook -> verify -> tìm payment -> mark PAID -> enroll student
     *
     * @param body the webhook request data from PayOS
     * @return acknowledgement message
     */
    @PostMapping("/payos/webhook")
    public ResponseEntity<?> payosWebhook(@RequestBody PayOSWebhookRequest body) {
        try {
            System.out.println("PayOS webhook received: code=" + body.getCode() + ", success=" + body.getSuccess());
            
            paymentService.handlePayOSWebhook(body);
            return ResponseEntity.ok(new MessageResponse("OK"));
        } catch (Exception ex) {
            System.err.println("PayOS webhook error: " + ex.getMessage());
            return ResponseEntity.ok(new MessageResponse("Processed with error: " + ex.getMessage()));
        }
    }

    /**
     * Handles Casso payment webhook (V1 - data as array).
     * Casso gửi webhook khi có giao dịch tiền vào tài khoản ngân hàng đã liên kết.
     * Luồng tự động: Casso webhook -> verify token -> tìm payment -> mark PAID -> enroll student
     *
     * @param body the webhook request data from Casso
     * @param authorization the Authorization header containing Secure Token
     * @return acknowledgement message
     */
    @PostMapping("/casso/webhook")
    public ResponseEntity<?> cassoWebhook(
            @RequestBody CassoWebhookRequest body,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "Secure-Token", required = false) String secureToken
    ) {
        try {
            System.out.println("📥 Casso webhook V1 received");
            
            // Casso có thể gửi token qua Authorization header hoặc Secure-Token header
            String authHeader = authorization != null ? authorization : secureToken;
            
            paymentService.handleCassoWebhook(body, authHeader);
            return ResponseEntity.ok(new MessageResponse("OK"));
        } catch (Exception ex) {
            System.err.println("Casso webhook error: " + ex.getMessage());
            // Trả về 200 OK để Casso không retry liên tục
            return ResponseEntity.ok(new MessageResponse("Processed with error: " + ex.getMessage()));
        }
    }

    /**
     * Handles Casso payment webhook V2 (data as object).
     * Casso V2 gửi data là object thay vì array.
     *
     * @param body the webhook request data from Casso V2
     * @param authorization the Authorization header containing Secure Token
     * @return acknowledgement message
     */
    @PostMapping("/casso/webhook/v2")
    public ResponseEntity<?> cassoWebhookV2(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Casso-Signature", required = false) String cassoSignature
    ) {
        try {
            System.out.println("📥 Casso webhook V2 received");
            System.out.println("📥 X-Casso-Signature: " + cassoSignature);
            
            // Parse JSON body
            ObjectMapper mapper = new ObjectMapper();
            CassoWebhookV2Request body = mapper.readValue(rawBody, CassoWebhookV2Request.class);
            
            paymentService.handleCassoWebhookV2(body, cassoSignature, rawBody);
            return ResponseEntity.ok(new MessageResponse("OK"));
        } catch (Exception ex) {
            System.err.println("Casso webhook V2 error: " + ex.getMessage());
            // Trả về 200 OK để Casso không retry liên tục
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
     * @param sortBy  sort field (createdAt or paidAt)
     * @param sortDir sort direction (asc or desc)
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
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Page<PaymentResponse> result = paymentService.getPayments(status, search, classId, from, to, page, size, sortBy, sortDir);
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

    /**
     * Checks payment status by ID.
     * Allows authenticated students to check their own payment status for polling.
     *
     * @param id the payment ID
     * @param user the authenticated student
     * @return payment status information
     */
    @GetMapping("/{id}/status")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> checkPaymentStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        Map<String, Object> statusInfo = paymentService.checkPaymentStatus(id, user.getId());
        return ResponseEntity.ok(statusInfo);
    }
}
