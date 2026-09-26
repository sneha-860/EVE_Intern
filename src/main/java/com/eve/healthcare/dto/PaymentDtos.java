package com.eve.healthcare.dto;

import com.eve.healthcare.entity.Payment.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentDtos {

    @Data
    public static class PaymentRequest {
        @NotNull(message = "Booking ID is required")
        private Long bookingId;

        private Boolean simulateFailure;
        private String paymentMethod;
    }

    @Data
    public static class WebhookPayload {
        @NotBlank(message = "Event ID is required")
        private String eventId;

        @NotBlank(message = "Event type is required")
        private String eventType; // e.g. "payment.success", "payment.failed"

        @NotNull(message = "Booking ID is required")
        private Long bookingId;

        private String transactionId;
        private BigDecimal amount;
        private String status; // "SUCCESS" or "FAILED"
    }

    @Data
    @Builder
    public static class PaymentResponse {
        private Long paymentId;
        private String transactionId;
        private Long bookingId;
        private BigDecimal amount;
        private PaymentStatus status;
        private String bookingStatus;
        private String message;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    public static class WebhookResponse {
        private boolean success;
        private String message;
        private String eventId;
    }
}
