package fpt.capstone.edu360managementsystem.dto.response;

import fpt.capstone.edu360managementsystem.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for payment list/details (admin).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long id;

    // Student info
    private Long studentId;
    private Long studentUserId;
    private String studentName;
    private String studentEmail;
    private String studentPhone;

    // Class info
    private Long classId;
    private String className;
    private String teacherName;
    private String subjectName;

    // Payment info
    private Long amount;
    private String content;
    private String orderCode;
    private PaymentStatus status;
    private String bankTransactionId;

    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
