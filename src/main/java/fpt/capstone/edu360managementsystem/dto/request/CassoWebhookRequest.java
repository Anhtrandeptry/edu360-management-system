package fpt.capstone.edu360managementsystem.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * DTO nhận webhook từ Casso khi có giao dịch ngân hàng.
 * Casso gửi thông tin giao dịch tiền vào tài khoản.
 * 
 * @see <a href="https://docs.casso.vn">Casso API Documentation</a>
 */
@Data
public class CassoWebhookRequest {

    /**
     * Mã lỗi (0 = thành công)
     */
    private Integer error;

    /**
     * Danh sách giao dịch
     */
    private List<CassoTransaction> data;

    @Data
    public static class CassoTransaction {

        /**
         * ID giao dịch từ Casso
         */
        private Long id;

        /**
         * Thời gian giao dịch (format: "yyyy-MM-dd HH:mm:ss")
         */
        private String when;

        /**
         * Số tiền giao dịch (VND)
         */
        private Long amount;

        /**
         * Nội dung chuyển khoản
         */
        private String description;

        /**
         * Số dư sau giao dịch
         */
        @JsonProperty("cusum_balance")
        private Long cusumBalance;

        /**
         * Mã giao dịch ngân hàng
         */
        private String tid;

        /**
         * ID tài khoản ngân hàng phụ
         */
        @JsonProperty("bank_sub_acc_id")
        private String bankSubAccId;

        /**
         * Tên ngân hàng đối tác (người chuyển)
         */
        @JsonProperty("corresponsive_name")
        private String corresponsiveName;

        /**
         * Số tài khoản đối tác (người chuyển)
         */
        @JsonProperty("corresponsive_account")
        private String corresponsiveAccount;

        /**
         * Tên ngân hàng đối tác
         */
        @JsonProperty("corresponsive_bank_id")
        private String corresponsiveBankId;
    }
}
