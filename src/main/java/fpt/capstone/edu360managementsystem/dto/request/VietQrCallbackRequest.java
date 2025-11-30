package fpt.capstone.edu360managementsystem.dto.request;

import lombok.Data;

/**
 * Payload callback từ VietQR / bank.
 * Thực tế bạn sẽ map theo spec chính thức,
 * ở đây là dạng generic để test Postman.
 */
@Data
public class VietQrCallbackRequest {

    private String accountNumber;  // tài khoản nhận tiền (của trung tâm)
    private Long amount;           // số tiền chuyển (VND)
    private String content;        // nội dung chuyển khoản
    private String transactionId;  // mã giao dịch của ngân hàng (optional)
}
