package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.request.CassoWebhookRequest;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentService.handleCassoWebhook()
 * 
 * KHÔNG THAY ĐỔI CODE GỐC - Sử dụng helper methods để tạo request objects
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService - handleCassoWebhook()")
class PaymentServiceHandleCassoWebhookTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private EnrollmentService enrollmentService;

    @InjectMocks
    private PaymentService paymentService;

    @Captor
    private ArgumentCaptor<Payment> paymentCaptor;

    private Payment payment1;
    private Payment payment2;
    private Payment payment3;

    @BeforeEach
    void setUp() {
        payment1 = TestDataBuilder.payment()
                .id(1L)
                .orderCode("PAY-1-1-111")
                .amount(1000000L)
                .status(PaymentStatus.PENDING)
                .build();

        payment2 = TestDataBuilder.payment()
                .id(2L)
                .orderCode("PAY-1-2-222")
                .amount(2000000L)
                .status(PaymentStatus.PENDING)
                .build();

        payment3 = TestDataBuilder.payment()
                .id(3L)
                .orderCode("PAY-1-3-333")
                .amount(3000000L)
                .status(PaymentStatus.PENDING)
                .build();
    }

    // ==================== HELPER METHODS ====================
    
    /**
     * Helper method to create CassoTransaction without modifying source code
     */
    private CassoWebhookRequest.CassoTransaction createTransaction(
            String tid, Long amount, String description) {
        CassoWebhookRequest.CassoTransaction tx = new CassoWebhookRequest.CassoTransaction();
        tx.setTid(tid);
        tx.setAmount(amount);
        tx.setDescription(description);
        return tx;
    }

    /**
     * Helper method to create CassoWebhookRequest without modifying source code
     */
    private CassoWebhookRequest createCassoRequest(
            Integer error, List<CassoWebhookRequest.CassoTransaction> data) {
        CassoWebhookRequest request = new CassoWebhookRequest();
        request.setError(error);
        request.setData(data);
        return request;
    }

    // ==================== TESTS ====================

    @Test
    @DisplayName("Should process all valid transactions successfully")
    void handleCassoWebhook_AllValidTransactions_ShouldProcessAll() {
        // Given
        CassoWebhookRequest.CassoTransaction tx1 = createTransaction(
                "TXN001", 1000000L, "Nguyen Van A #PAY-1-1-111"
        );
        CassoWebhookRequest.CassoTransaction tx2 = createTransaction(
                "TXN002", 2000000L, "Tran Thi B #PAY-1-2-222"
        );

        CassoWebhookRequest request = createCassoRequest(0, Arrays.asList(tx1, tx2));

        when(paymentRepository.findByOrderCode("PAY-1-1-111"))
                .thenReturn(Optional.of(payment1));
        when(paymentRepository.findByOrderCode("PAY-1-2-222"))
                .thenReturn(Optional.of(payment2));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        paymentService.handleCassoWebhook(request);

        // Then
        verify(paymentRepository, times(2)).save(paymentCaptor.capture());
        List<Payment> savedPayments = paymentCaptor.getAllValues();

        assertThat(savedPayments).hasSize(2);
        assertThat(savedPayments.get(0).getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(savedPayments.get(0).getBankTransactionId()).isEqualTo("TXN001");
        assertThat(savedPayments.get(1).getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(savedPayments.get(1).getBankTransactionId()).isEqualTo("TXN002");

        verify(enrollmentService, times(2)).enrollAfterPayment(any(), any());
    }

    @Test
    @DisplayName("Should handle mixed valid and invalid transactions")
    void handleCassoWebhook_MixedTransactions_ShouldProcessValidOnes() {
        // Given
        CassoWebhookRequest.CassoTransaction validTx = createTransaction(
                "TXN001", 1000000L, "Valid payment #PAY-1-1-111"
        );
        CassoWebhookRequest.CassoTransaction invalidTx = createTransaction(
                "TXN002", 999999L, "Invalid amount #PAY-1-2-222"
        );
        CassoWebhookRequest.CassoTransaction noOrderCodeTx = createTransaction(
                "TXN003", 3000000L, "No order code here"
        );

        CassoWebhookRequest request = createCassoRequest(
                0, Arrays.asList(validTx, invalidTx, noOrderCodeTx)
        );

        when(paymentRepository.findByOrderCode("PAY-1-1-111"))
                .thenReturn(Optional.of(payment1));
        when(paymentRepository.findByOrderCode("PAY-1-2-222"))
                .thenReturn(Optional.of(payment2));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        paymentService.handleCassoWebhook(request);

        // Then
        verify(paymentRepository, times(2)).save(paymentCaptor.capture());
        List<Payment> savedPayments = paymentCaptor.getAllValues();

        assertThat(savedPayments.get(0).getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(savedPayments.get(0).getBankTransactionId()).isEqualTo("TXN001");

        assertThat(savedPayments.get(1).getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(savedPayments.get(1).getBankTransactionId()).isEqualTo("TXN002");

        verify(enrollmentService, times(1)).enrollAfterPayment(
                eq(payment1.getClazz().getId()),
                eq(payment1.getStudent().getId())
        );
    }

    @Test
    @DisplayName("Should skip already processed payments")
    void handleCassoWebhook_AlreadyPaid_ShouldSkip() {
        // Given
        payment1.setStatus(PaymentStatus.PAID);

        CassoWebhookRequest.CassoTransaction tx = createTransaction(
                "TXN001", 1000000L, "Payment #PAY-1-1-111"
        );

        CassoWebhookRequest request = createCassoRequest(0, Collections.singletonList(tx));

        when(paymentRepository.findByOrderCode("PAY-1-1-111"))
                .thenReturn(Optional.of(payment1));

        // When
        paymentService.handleCassoWebhook(request);

        // Then
        verify(paymentRepository, never()).save(any());
        verify(enrollmentService, never()).enrollAfterPayment(any(), any());
    }

    @Test
    @DisplayName("Should handle empty transaction list gracefully")
    void handleCassoWebhook_EmptyData_ShouldNotFail() {
        // Given
        CassoWebhookRequest request = createCassoRequest(0, Collections.emptyList());

        // When
        paymentService.handleCassoWebhook(request);

        // Then
        verify(paymentRepository, never()).findByOrderCode(any());
        verify(paymentRepository, never()).save(any());
        verify(enrollmentService, never()).enrollAfterPayment(any(), any());
    }

    @Test
    @DisplayName("Should handle null data gracefully")
    void handleCassoWebhook_NullData_ShouldNotFail() {
        // Given
        CassoWebhookRequest request = createCassoRequest(0, null);

        // When
        paymentService.handleCassoWebhook(request);

        // Then
        verify(paymentRepository, never()).findByOrderCode(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw error when webhook has error code")
    void handleCassoWebhook_ErrorCode_ShouldThrowError() {
        // Given
        CassoWebhookRequest request = createCassoRequest(1, Collections.emptyList());

        // When & Then
        assertThatThrownBy(() -> paymentService.handleCassoWebhook(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Casso webhook error");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should skip transactions with negative or zero amount")
    void handleCassoWebhook_InvalidAmount_ShouldSkip() {
        // Given
        CassoWebhookRequest.CassoTransaction zeroTx = createTransaction(
                "TXN001", 0L, "Zero amount #PAY-1-1-111"
        );
        CassoWebhookRequest.CassoTransaction negativeTx = createTransaction(
                "TXN002", -1000000L, "Negative amount #PAY-1-2-222"
        );

        CassoWebhookRequest request = createCassoRequest(0, Arrays.asList(zeroTx, negativeTx));

        // When
        paymentService.handleCassoWebhook(request);

        // Then
        verify(paymentRepository, never()).findByOrderCode(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should mark as FAILED when amount does not match")
    void handleCassoWebhook_AmountMismatch_ShouldMarkFailed() {
        // Given
        CassoWebhookRequest.CassoTransaction tx = createTransaction(
                "TXN001", 500000L, "Wrong amount #PAY-1-1-111"
        );

        CassoWebhookRequest request = createCassoRequest(0, Collections.singletonList(tx));

        when(paymentRepository.findByOrderCode("PAY-1-1-111"))
                .thenReturn(Optional.of(payment1));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        paymentService.handleCassoWebhook(request);

        // Then
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();

        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(savedPayment.getBankTransactionId()).isEqualTo("TXN001");

        verify(enrollmentService, never()).enrollAfterPayment(any(), any());
    }

    @Test
    @DisplayName("Should continue processing when enrollment fails")
    void handleCassoWebhook_EnrollmentFails_ShouldContinueProcessing() {
        // Given
        CassoWebhookRequest.CassoTransaction tx1 = createTransaction(
                "TXN001", 1000000L, "Payment 1 #PAY-1-1-111"
        );
        CassoWebhookRequest.CassoTransaction tx2 = createTransaction(
                "TXN002", 2000000L, "Payment 2 #PAY-1-2-222"
        );

        CassoWebhookRequest request = createCassoRequest(0, Arrays.asList(tx1, tx2));

        when(paymentRepository.findByOrderCode("PAY-1-1-111"))
                .thenReturn(Optional.of(payment1));
        when(paymentRepository.findByOrderCode("PAY-1-2-222"))
                .thenReturn(Optional.of(payment2));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        doThrow(new RuntimeException("Class is full"))
                .when(enrollmentService).enrollAfterPayment(
                        eq(payment1.getClazz().getId()),
                        eq(payment1.getStudent().getId())
                );

        // When
        paymentService.handleCassoWebhook(request);

        // Then
        verify(paymentRepository, times(2)).save(paymentCaptor.capture());
        List<Payment> savedPayments = paymentCaptor.getAllValues();

        assertThat(savedPayments).allMatch(p -> p.getStatus() == PaymentStatus.PAID);

        verify(enrollmentService, times(2)).enrollAfterPayment(any(), any());
    }

    @Test
    @DisplayName("Should skip when payment not found by orderCode")
    void handleCassoWebhook_PaymentNotFound_ShouldSkip() {
        // Given
        CassoWebhookRequest.CassoTransaction tx = createTransaction(
                "TXN001", 1000000L, "Unknown payment #PAY-999-999-999"
        );

        CassoWebhookRequest request = createCassoRequest(0, Collections.singletonList(tx));

        when(paymentRepository.findByOrderCode("PAY-999-999-999"))
                .thenReturn(Optional.empty());

        // When
        paymentService.handleCassoWebhook(request);

        // Then
        verify(paymentRepository, never()).save(any());
        verify(enrollmentService, never()).enrollAfterPayment(any(), any());
    }

    @Test
    @DisplayName("Should handle transactions without orderCode gracefully")
    void handleCassoWebhook_NoOrderCode_ShouldSkip() {
        // Given
        CassoWebhookRequest.CassoTransaction tx1 = createTransaction(
                "TXN001", 1000000L, "No order code here"
        );
        CassoWebhookRequest.CassoTransaction tx2 = createTransaction(
                "TXN002", 2000000L, "Just a regular transfer"
        );

        CassoWebhookRequest request = createCassoRequest(0, Arrays.asList(tx1, tx2));

        // When
        paymentService.handleCassoWebhook(request);

        // Then
        verify(paymentRepository, never()).findByOrderCode(any());
        verify(paymentRepository, never()).save(any());
    }
}
