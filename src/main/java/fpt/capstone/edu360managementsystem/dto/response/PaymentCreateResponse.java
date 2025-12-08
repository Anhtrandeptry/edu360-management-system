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

    private Long amount;
    private String content;

    private String qrImageUrl;
}
