package fpt.capstone.edu360managementsystem.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreateResponse {

    // Expose to frontend - needed for UI display
    private Long classId;
    private Long amount;
    private String qrImageUrl;
    private String content;  // Nội dung chuyển khoản - cần hiển thị cho user
    
    // These fields are for internal use only, not exposed to API response
    @JsonIgnore
    private Long paymentId;
    
    @JsonIgnore
    private Long studentId;
}
