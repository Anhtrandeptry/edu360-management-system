package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.entity.Payment;
import fpt.capstone.edu360managementsystem.enums.PaymentStatus;
import fpt.capstone.edu360managementsystem.repository.PaymentRepository;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentService.confirmPayment()
 * 
 * Test coverage:
 * - PENDING → PAID + auto-enroll + notification
 * - Already PAID → reject
 * - Enrollment fails → payment still confirmed
 * - Notification fails → payment still confirmed
 * - Payment not found → error
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService - confirmPayment()")
class PaymentServiceConfirmPaymentTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private EnrollmentService enrollmentService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PaymentService paymentService;

    @Captor
    private ArgumentCaptor<Payment> paymentCaptor;

    private Payment testPayment;

    @BeforeEach
    void setUp() {
        testPayment = TestDataBuilder.payment()
                .id(1L)
                .amount(1000000L)
                .status(PaymentStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("Should confirm payment, send notification and auto-enroll when status is PENDING")
    void confirmPayment_PendingPayment_ShouldConfirmAndEnroll() {
        // Given
        Long paymentId = 1L;
        when(paymentRepository.findById(paymentId))
                .thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        paymentService.confirmPayment(paymentId);

        // Then - verify payment was updated
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();

        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(savedPayment.getPaidAt()).isNotNull();
        assertThat(savedPayment.getPaidAt()).isBeforeOrEqualTo(LocalDateTime.now());

        // Verify notification was sent
        verify(notificationService).notifyPaymentSuccess(
                eq(testPayment.getStudent().getUser().getId()),
                eq(testPayment.getClazz().getName()),
                eq(testPayment.getAmount())
        );

        // Verify auto-enrollment was triggered
        verify(enrollmentService).enrollAfterPayment(
                eq(testPayment.getClazz().getId()),
                eq(testPayment.getStudent().getId())
        );
    }

    @Test
    @DisplayName("Should throw error when payment is already PAID")
    void confirmPayment_AlreadyPaid_ShouldThrowError() {
        // Given
        testPayment.setStatus(PaymentStatus.PAID);
        testPayment.setPaidAt(LocalDateTime.now().minusHours(1));

        Long paymentId = 1L;
        when(paymentRepository.findById(paymentId))
                .thenReturn(Optional.of(testPayment));

        // When & Then
        assertThatThrownBy(() -> paymentService.confirmPayment(paymentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Payment đã được xác nhận trước đó");

        // Verify no updates were made
        verify(paymentRepository, never()).save(any());
        verify(notificationService, never()).notifyPaymentSuccess(any(), any(), any());
        verify(enrollmentService, never()).enrollAfterPayment(any(), any());
    }

    @Test
    @DisplayName("Should throw error when payment not found")
    void confirmPayment_PaymentNotFound_ShouldThrowError() {
        // Given
        Long paymentId = 999L;
        when(paymentRepository.findById(paymentId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.confirmPayment(paymentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Payment not found");

        verify(paymentRepository, never()).save(any());
        verify(enrollmentService, never()).enrollAfterPayment(any(), any());
    }

    @Test
    @DisplayName("Should confirm payment even if notification fails")
    void confirmPayment_NotificationFails_ShouldStillConfirm() {
        // Given
        Long paymentId = 1L;
        when(paymentRepository.findById(paymentId))
                .thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Notification fails
        doThrow(new RuntimeException("Email service unavailable"))
                .when(notificationService).notifyPaymentSuccess(any(), any(), any());

        // When
        paymentService.confirmPayment(paymentId);

        // Then - payment should still be confirmed
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();

        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(savedPayment.getPaidAt()).isNotNull();

        // Enrollment should still be attempted
        verify(enrollmentService).enrollAfterPayment(
                eq(testPayment.getClazz().getId()),
                eq(testPayment.getStudent().getId())
        );
    }

    @Test
    @DisplayName("Should confirm payment even if enrollment fails")
    void confirmPayment_EnrollmentFails_ShouldStillConfirm() {
        // Given
        Long paymentId = 1L;
        when(paymentRepository.findById(paymentId))
                .thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Enrollment fails (e.g., class full)
        doThrow(new RuntimeException("Class is full"))
                .when(enrollmentService).enrollAfterPayment(any(), any());

        // When
        paymentService.confirmPayment(paymentId);

        // Then - payment should still be confirmed
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();

        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(savedPayment.getPaidAt()).isNotNull();

        // Notification should have been sent
        verify(notificationService).notifyPaymentSuccess(
                eq(testPayment.getStudent().getUser().getId()),
                eq(testPayment.getClazz().getName()),
                eq(testPayment.getAmount())
        );

        // Enrollment was attempted
        verify(enrollmentService).enrollAfterPayment(
                eq(testPayment.getClazz().getId()),
                eq(testPayment.getStudent().getId())
        );
    }

    @Test
    @DisplayName("Should handle FAILED status payment")
    void confirmPayment_FailedPayment_ShouldConfirm() {
        // Given - payment was previously marked as FAILED
        testPayment.setStatus(PaymentStatus.FAILED);

        Long paymentId = 1L;
        when(paymentRepository.findById(paymentId))
                .thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        paymentService.confirmPayment(paymentId);

        // Then - should allow manual confirmation
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();

        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(savedPayment.getPaidAt()).isNotNull();

        verify(notificationService).notifyPaymentSuccess(any(), any(), any());
        verify(enrollmentService).enrollAfterPayment(any(), any());
    }

    @Test
    @DisplayName("Should set paidAt to current time when confirming")
    void confirmPayment_ShouldSetPaidAtToCurrentTime() {
        // Given
        Long paymentId = 1L;
        LocalDateTime beforeConfirm = LocalDateTime.now();

        when(paymentRepository.findById(paymentId))
                .thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        paymentService.confirmPayment(paymentId);

        // Then
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();

        LocalDateTime afterConfirm = LocalDateTime.now();

        assertThat(savedPayment.getPaidAt())
                .isNotNull()
                .isAfterOrEqualTo(beforeConfirm)
                .isBeforeOrEqualTo(afterConfirm);
    }

    @Test
    @DisplayName("Should call services in correct order")
    void confirmPayment_ShouldCallServicesInOrder() {
        // Given
        Long paymentId = 1L;
        when(paymentRepository.findById(paymentId))
                .thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        paymentService.confirmPayment(paymentId);

        // Then - verify order of operations
        var inOrder = inOrder(paymentRepository, notificationService, enrollmentService);

        // 1. Save payment first
        inOrder.verify(paymentRepository).save(any(Payment.class));

        // 2. Send notification
        inOrder.verify(notificationService).notifyPaymentSuccess(any(), any(), any());

        // 3. Enroll student
        inOrder.verify(enrollmentService).enrollAfterPayment(any(), any());
    }

    @Test
    @DisplayName("Should handle both notification and enrollment failures gracefully")
    void confirmPayment_BothServicesFail_ShouldStillConfirm() {
        // Given
        Long paymentId = 1L;
        when(paymentRepository.findById(paymentId))
                .thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Both services fail
        doThrow(new RuntimeException("Notification failed"))
                .when(notificationService).notifyPaymentSuccess(any(), any(), any());
        doThrow(new RuntimeException("Enrollment failed"))
                .when(enrollmentService).enrollAfterPayment(any(), any());

        // When
        paymentService.confirmPayment(paymentId);

        // Then - payment should still be confirmed
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();

        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(savedPayment.getPaidAt()).isNotNull();

        // Both services were attempted
        verify(notificationService).notifyPaymentSuccess(any(), any(), any());
        verify(enrollmentService).enrollAfterPayment(any(), any());
    }
}
