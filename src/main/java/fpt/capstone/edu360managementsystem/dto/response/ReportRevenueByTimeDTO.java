package fpt.capstone.edu360managementsystem.dto.response;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRevenueByTimeDTO {

    private LocalDate date;
    private String label;          // "01/12", "Tuần 1", "Tháng 1"...
    private Long revenue;
    private Integer paymentCount;  // Số giao dịch
}
