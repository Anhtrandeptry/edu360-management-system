package fpt.capstone.edu360managementsystem.controller;

import fpt.capstone.edu360managementsystem.dto.request.VietQrCallbackRequest;
import fpt.capstone.edu360managementsystem.dto.response.MessageResponse;
import fpt.capstone.edu360managementsystem.dto.response.PaymentCreateResponse;
import fpt.capstone.edu360managementsystem.service.PaymentService;
import fpt.capstone.edu360managementsystem.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
}
