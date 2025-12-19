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


    private Integer error;


    private List<CassoTransaction> data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CassoTransaction {

        private Long id;


        @JsonProperty("when")
        private String when;


        private Long amount;


        private String description;


        @JsonProperty("cusum_balance")
        private Long cusumBalance;


        private String tid;


        @JsonProperty("bank_sub_acc_id")
        private String bankSubAccId;
    }
}
