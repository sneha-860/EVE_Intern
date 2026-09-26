package com.eve.healthcare.dto;

import com.eve.healthcare.entity.Booking.BookingStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BookingDtos {

    @Data
    public static class CreateBookingRequest {
        @NotNull(message = "Centre ID is required")
        private Long centreId;

        @NotNull(message = "Test ID is required")
        private Long testId;

        @NotNull(message = "Appointment date/time is required")
        @Future(message = "Appointment date/time must be in the future")
        private LocalDateTime appointmentDateTime;
    }

    @Data
    @Builder
    public static class BookingResponse {
        private Long id;
        private Long userId;
        private String userName;
        private Long centreId;
        private String centreName;
        private Long testId;
        private String testName;
        private LocalDateTime appointmentDateTime;
        private BigDecimal amount;
        private BookingStatus status;
        private LocalDateTime createdAt;
    }
}
