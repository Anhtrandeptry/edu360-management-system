package fpt.capstone.edu360managementsystem.dto.request;

import lombok.Data;

/**
 * Payload callback từ VietQR / bank.
 * Thực tế bạn sẽ map theo spec chính thức,
 * ở đây là dạng generic để test Postman.
 */
@Data
public class VietQrCallbackRequest {

    private String accountNumber;
    private Long amount;
    private String content;
    private String transactionId;
}
