package fpt.capstone.edu360managementsystem.entity;

import fpt.capstone.edu360managementsystem.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"class_id", "student_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Lớp học
    @ManyToOne(optional = false)
    @JoinColumn(name = "class_id")
    private Clazz clazz;

    // Học sinh
    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id")
    private Student student;

    // Số tiền cần thanh toán (VND)
    @Column(nullable = false)
    private Long amount;

    // Nội dung chuyển khoản: "Tên học sinh - thanh toán học phí"
    @Column(nullable = false, length = 255)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    // Mã giao dịch nội bộ (dùng đối soát, nếu sau này cần)
    @Column(nullable = false, unique = true, length = 100)
    private String orderCode;

    // Mã giao dịch phía ngân hàng (optional)
    private String bankTransactionId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime paidAt;
}
