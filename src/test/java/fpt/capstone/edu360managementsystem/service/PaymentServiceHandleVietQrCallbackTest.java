package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.request.VietQrCallbackRequest;
import fpt.capstone.edu360managementsystem.entity.Clazz;
import fpt.capstone.edu360managementsystem.entity.Payment;
import fpt.capstone.edu360managementsystem.entity.Student;
import fpt.capstone.edu360managementsystem.enums.PaymentStatus;
import fpt.capstone.edu360managementsystem.repository.ClassSessionRepository;
import fpt.capstone.edu360managementsystem.repository.ClazzRepository;
import fpt.capstone.edu360managementsystem.repository.PaymentRepository;
import fpt.capstone.edu360managementsystem.repository.StudentRepository;
import fpt.capstone.edu360managementsystem.testbuilder.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentService.handleVietQrCallback()
 * 
 * KHÔNG THAY ĐỔI CODE GỐC - Sử dụng helper methods để tạo request objects
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService - handleVietQrCallback()")
class PaymentServiceHandleVietQrCallbackTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ClazzRepository clazzRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ClassSessionRepository classSessionRepository;

    @Mock
    private EnrollmentService enrollmentService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PaymentService paymentService;

    @Captor
    private ArgumentCaptor<Payment> paymentCaptor;

    private static final String VALID_ACCOUNT_NUMBER = "1234567890";
    private static final String ORDER_CODE = "PAY-1-1-123456";
    private static final Long VALID_AMOUNT = 1000000L;

    private Payment testPayment;
    private Clazz testClass;
    private Student testStudent;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "accountNumber", VALID_ACCOUNT_NUMBER);

        testStudent = TestDataBuilder.student()
                .id(1L)
                .user(TestDataBuilder.user().id(2L).fullName("Nguyen Van A").build())
                .build();

        testClass = TestDataBuilder.clazz()
                .id(1L)
                .name("Math 101")
                .pricePerSession(100000L)
                .build();

        testPayment = TestDataBuilder.payment()
                .id(1L)
                .clazz(testClass)
                .student(testStudent)
                .amount(VALID_AMOUNT)
                .orderCode(ORDER_CODE)
                .content("Nguyen Van A thanh toan hoc phi #" + ORDER_CODE)
                .status(PaymentStatus.PENDING)
                .build();
    }

    // ==================== HELPER METHODS ====================
    
    /**
     * Helper method to create VietQrCallbackRequest without modifying source code
     */
    private VietQrCallbackRequest createVietQrRequest(
            String accountNumber, Long amount, String content, String transactionId) {
        VietQrCallbackRequest request = new VietQrCallbackRequest();
        request.setAccountNumber(accountNumber);
        request.setAmount(amount);
        request.setContent(content);
        request.setTransactionId(transactionId);
        return request;
    }

    // ==================== TESTS ====================

    @Test
    @DisplayName("Should mark payment as PAID and auto-enroll when callback is valid")
    void handleVietQrCallback_ValidPayment_ShouldMarkPaidAndEnroll() {
        // Given
        VietQrCallbackRequest request = createVietQrRequest(
                VALID_ACCOUNT_NUMBER,
                VALID_AMOUNT,
                "Nguyen Van A thanh toan hoc phi #" + ORDER_CODE,
                "TXN123456"
        );

        when(paymentRepository.findByOrderCode(ORDER_CODE))
                .thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        paymentService.handleVietQrCallback(request);

        // Then
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();

        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(savedPayment.getBankTransactionId()).isEqualTo("TXN123456");
        assertThat(savedPayment.getPaidAt()).isNotNull();
        assertThat(savedPayment.getPaidAt()).isBeforeOrEqualTo(LocalDateTime.now());

        verify(enrollmentService).enrollAfterPayment(
                eq(testClass.getId()),
                eq(testStudent.getId())
        );
    }

    @Test
    @DisplayName("Should reject when account number is invalid")
    void handleVietQrCallback_InvalidAccountNumber_ShouldReject() {
        // Given
        VietQrCallbackRequest request = createVietQrRequest(
                "9999999999",
                VALID_AMOUNT,
                "Nguyen Van A thanh toan hoc phi #" + ORDER_CODE,
                "TXN123456"
        );

        // When & Then
        assertThatThrownBy(() -> paymentService.handleVietQrCallback(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Sai tài khoản nhận tiền");

        verify(paymentRepository, never()).save(any());
        verify(enrollmentService, never()).enrollAfterPayment(any(), any());
    }

    @Test
    @DisplayName("Should mark as FAILED when amount does not match")
    void handleVietQrCallback_AmountMismatch_ShouldMarkFailed() {
        // Given
        Long wrongAmount = 500000L;
        VietQrCallbackRequest request = createVietQrRequest(
                VALID_ACCOUNT_NUMBER,
                wrongAmount,
                "Nguyen Van A thanh toan hoc phi #" + ORDER_CODE,
                "TXN123456"
        );

        when(paymentRepository.findByOrderCode(ORDER_CODE))
                .thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When & Then
        assertThatThrownBy(() -> paymentService.handleVietQrCallback(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Số tiền thanh toán không khớp");

        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();

        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(savedPayment.getBankTransactionId()).isEqualTo("TXN123456");

        verify(enrollmentService, never()).enrollAfterPayment(any(), any());
    }

    @Test
    @DisplayName("Should throw error when orderCode is not found in content")
    void handleVietQrCallback_MissingOrderCode_ShouldThrowError() {
        // Given
        VietQrCallbackRequest request = createVietQrRequest(
                VALID_ACCOUNT_NUMBER,
                VALID_AMOUNT,
                "Nguyen Van A thanh toan hoc phi",
                "TXN123456"
        );

        // When & Then
        assertThatThrownBy(() -> paymentService.handleVietQrCallback(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Không tìm thấy mã order trong nội dung chuyển khoản");

        verify(paymentRepository, never()).save(any());
        verify(enrollmentService, never()).enrollAfterPayment(any(), any());
    }

    @Test
    @DisplayName("Should throw error when payment not found by orderCode")
    void handleVietQrCallback_PaymentNotFound_ShouldThrowError() {
        // Given
        VietQrCallbackRequest request = createVietQrRequest(
                VALID_ACCOUNT_NUMBER,
                VALID_AMOUNT,
                "Nguyen Van A thanh toan hoc phi #PAY-999-999-999",
                "TXN123456"
        );

        when(paymentRepository.findByOrderCode("PAY-999-999-999"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.handleVietQrCallback(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Payment không tồn tại cho orderCode");

        verify(paymentRepository, never()).save(any());
        verify(enrollmentService, never()).enrollAfterPayment(any(), any());
    }

    @Test
    @DisplayName("Should be idempotent when payment is already PAID")
    void handleVietQrCallback_AlreadyPaid_ShouldBeIdempotent() {
        // Given
        testPayment.setStatus(PaymentStatus.PAID);
        testPayment.setPaidAt(LocalDateTime.now().minusHours(1));
        testPayment.setBankTransactionId("OLD_TXN");

        VietQrCallbackRequest request = createVietQrRequest(
                VALID_ACCOUNT_NUMBER,
                VALID_AMOUNT,
                "Nguyen Van A thanh toan hoc phi #" + ORDER_CODE,
                "NEW_TXN"
        );

        when(paymentRepository.findByOrderCode(ORDER_CODE))
                .thenReturn(Optional.of(testPayment));

        // When
        paymentService.handleVietQrCallback(request);

        // Then
        verify(paymentRepository).save(any(Payment.class));
        verify(enrollmentService).enrollAfterPayment(any(), any());
    }

    @Test
    @DisplayName("Should mark payment as PAID even if enrollment fails")
    void handleVietQrCallback_EnrollmentFails_ShouldStillMarkPaid() {
        // Given
        VietQrCallbackRequest request = createVietQrRequest(
                VALID_ACCOUNT_NUMBER,
                VALID_AMOUNT,
                "Nguyen Van A thanh toan hoc phi #" + ORDER_CODE,
                "TXN123456"
        );

        when(paymentRepository.findByOrderCode(ORDER_CODE))
                .thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        doThrow(new RuntimeException("Class is full"))
                .when(enrollmentService).enrollAfterPayment(any(), any());

        // When
        paymentService.handleVietQrCallback(request);

        // Then
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();

        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(savedPayment.getBankTransactionId()).isEqualTo("TXN123456");

        verify(enrollmentService).enrollAfterPayment(
                eq(testClass.getId()),
                eq(testStudent.getId())
        );
    }

    @Test
    @DisplayName("Should reject when amount is null or zero")
    void handleVietQrCallback_InvalidAmount_ShouldReject() {
        // Given - null amount
        VietQrCallbackRequest request1 = createVietQrRequest(
                VALID_ACCOUNT_NUMBER,
                null,
                "Nguyen Van A thanh toan hoc phi #" + ORDER_CODE,
                "TXN123456"
        );

        // When & Then
        assertThatThrownBy(() -> paymentService.handleVietQrCallback(request1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Số tiền không hợp lệ");

        // Given - zero amount
        VietQrCallbackRequest request2 = createVietQrRequest(
                VALID_ACCOUNT_NUMBER,
                0L,
                "Nguyen Van A thanh toan hoc phi #" + ORDER_CODE,
                "TXN123456"
        );

        // When & Then
        assertThatThrownBy(() -> paymentService.handleVietQrCallback(request2))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Số tiền không hợp lệ");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should extract orderCode correctly from various content formats")
    void handleVietQrCallback_VariousContentFormats_ShouldExtractOrderCode() {
        // Given - orderCode at the end
        VietQrCallbackRequest request1 = createVietQrRequest(
                VALID_ACCOUNT_NUMBER,
                VALID_AMOUNT,
                "Thanh toan #" + ORDER_CODE,
                "TXN1"
        );

        when(paymentRepository.findByOrderCode(ORDER_CODE))
                .thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        paymentService.handleVietQrCallback(request1);

        // Then
        verify(paymentRepository).findByOrderCode(ORDER_CODE);
        verify(paymentRepository).save(any(Payment.class));

        // Given - orderCode in the middle
        reset(paymentRepository, enrollmentService);
        VietQrCallbackRequest request2 = createVietQrRequest(
                VALID_ACCOUNT_NUMBER,
                VALID_AMOUNT,
                "Thanh toan #" + ORDER_CODE + " cho lop hoc",
                "TXN2"
        );

        // The extractOrderCode method extracts everything after # including trailing text
        String extractedOrderCode = ORDER_CODE + " cho lop hoc";
        when(paymentRepository.findByOrderCode(extractedOrderCode))
                .thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        paymentService.handleVietQrCallback(request2);

        // Then
        verify(paymentRepository).findByOrderCode(extractedOrderCode);
    }
}
