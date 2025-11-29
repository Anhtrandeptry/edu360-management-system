package fpt.capstone.edu360managementsystem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreateResponse {

    private Long paymentId;
    private Long classId;
    private Long studentId;

    private Long amount;      // Số tiền cần thanh toán (VND)
    private String content;   // Nội dung chuyển khoản

    private String qrImageUrl; // URL ảnh QR VietQR
}
