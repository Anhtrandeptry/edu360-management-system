package fpt.capstone.edu360managementsystem.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Payload webhook từ Casso.vn
 * Casso sẽ gửi webhook khi có biến động số dư tài khoản ngân hàng.
 * 
 * Đăng ký webhook tại: https://my.casso.vn
 * Docs: https://docs.casso.vn/api-reference/webhook
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CassoWebhookRequest {

    /**
     * Loại error (0 = success)
     */
    private Integer error;

    /**
     * Danh sách giao dịch
     */
    private List<CassoTransaction> data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CassoTransaction {
        /**
         * ID giao dịch của Casso
         */
        private Long id;

        /**
         * Thời gian giao dịch (format: yyyy-MM-dd HH:mm:ss)
         */
        @JsonProperty("when")
        private String when;

        /**
         * Số tiền giao dịch (dương = tiền vào, âm = tiền ra)
         */
        private Long amount;

        /**
         * Nội dung chuyển khoản
         */
        private String description;

        /**
         * Số tài khoản nhận
         */
        @JsonProperty("cusum_balance")
        private Long cusumBalance;

        /**
         * Mã tham chiếu giao dịch
         */
        private String tid;

        /**
         * Số tài khoản ngân hàng
         */
        @JsonProperty("bank_sub_acc_id")
        private String bankSubAccId;
    }
}
