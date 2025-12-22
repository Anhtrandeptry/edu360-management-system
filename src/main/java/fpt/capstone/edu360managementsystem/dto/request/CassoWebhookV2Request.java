package fpt.capstone.edu360managementsystem.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO nhận webhook V2 từ Casso khi có giao dịch ngân hàng.
 * Casso V2 gửi data là object (không phải array như V1).
 * 
 * @see <a href="https://docs.casso.vn">Casso API Documentation</a>
 */
@Data
public class CassoWebhookV2Request {

    /**
     * Mã lỗi (0 = thành công)
     */
    private Integer error;

    /**
     * Thông tin giao dịch (object, không phải array)
     */
    private CassoTransactionV2 data;

    @Data
    public static class CassoTransactionV2 {

        /**
         * ID giao dịch từ Casso
         */
        private Long id;

        /**
         * Mã tham chiếu giao dịch
         */
        private String reference;

        /**
         * Nội dung chuyển khoản
         */
        private String description;

        /**
         * Số tiền giao dịch (VND)
         */
        private Long amount;

        /**
         * Số dư sau giao dịch
         */
        private Long runningBalance;

        /**
         * Thời gian giao dịch (format: "yyyy-MM-dd HH:mm:ss")
         */
        private String transactionDateTime;

        /**
         * Số tài khoản nhận
         */
        private String accountNumber;

        /**
         * Tên ngân hàng
         */
        private String bankName;

        /**
         * Viết tắt ngân hàng
         */
        private String bankAbbreviation;

        /**
         * Số tài khoản ảo
         */
        private String virtualAccountNumber;

        /**
         * Tên tài khoản ảo
         */
        private String virtualAccountName;

        /**
         * Tên người chuyển
         */
        private String counterAccountName;

        /**
         * Số tài khoản người chuyển
         */
        private String counterAccountNumber;

        /**
         * Mã ngân hàng người chuyển
         */
        private String counterAccountBankId;

        /**
         * Tên ngân hàng người chuyển
         */
        private String counterAccountBankName;
    }
}
