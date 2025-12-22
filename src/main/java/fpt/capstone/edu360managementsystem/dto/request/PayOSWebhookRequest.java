package fpt.capstone.edu360managementsystem.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Payload webhook từ PayOS
 * PayOS sẽ gửi webhook khi có giao dịch thanh toán thành công.
 * 
 * Docs: https://payos.vn/docs/api/webhook
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayOSWebhookRequest {

    /**
     * Mã lỗi: "00" = thành công
     */
    private String code;

    /**
     * Mô tả lỗi
     */
    private String desc;

    /**
     * Trạng thái thành công
     */
    private Boolean success;

    /**
     * Dữ liệu giao dịch
     */
    private PayOSTransactionData data;

    /**
     * Chữ ký để verify (HMAC SHA256)
     */
    private String signature;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PayOSTransactionData {
        /**
         * Mã đơn hàng (orderCode khi tạo payment)
         */
        private Long orderCode;

        /**
         * Số tiền thanh toán
         */
        private Long amount;

        /**
         * Nội dung chuyển khoản
         */
        private String description;

        /**
         * Số tài khoản nhận tiền
         */
        private String accountNumber;

        /**
         * Mã giao dịch ngân hàng
         */
        private String reference;

        /**
         * Thời gian giao dịch
         */
        private String transactionDateTime;

        /**
         * Loại tiền tệ
         */
        private String currency;

        /**
         * ID link thanh toán
         */
        private String paymentLinkId;

        /**
         * Mã trạng thái giao dịch
         */
        private String code;

        /**
         * Mô tả trạng thái
         */
        private String desc;

        /**
         * Thông tin tài khoản chuyển tiền
         */
        private String counterAccountBankId;
        private String counterAccountBankName;
        private String counterAccountName;
        private String counterAccountNumber;
        private String virtualAccountName;
        private String virtualAccountNumber;
    }
}
