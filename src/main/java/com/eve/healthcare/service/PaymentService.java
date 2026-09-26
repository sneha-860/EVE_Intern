package com.eve.healthcare.service;

import com.eve.healthcare.dto.PaymentDtos.*;
import com.eve.healthcare.entity.Booking;
import com.eve.healthcare.entity.Payment;
import com.eve.healthcare.entity.Payment.PaymentStatus;
import com.eve.healthcare.entity.ProcessedWebhook;
import com.eve.healthcare.entity.User;
import com.eve.healthcare.exception.CustomExceptions.*;
import com.eve.healthcare.repository.BookingRepository;
import com.eve.healthcare.repository.PaymentRepository;
import com.eve.healthcare.repository.ProcessedWebhookRepository;
import com.eve.healthcare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final ProcessedWebhookRepository webhookRepository;

    @Transactional
    public PaymentResponse processSimulatedPayment(String userEmail, PaymentRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + request.getBookingId()));

        if (!booking.getUser().getId().equals(user.getId()) && user.getRole() != User.Role.ADMIN) {
            throw new UnauthorizedAccessException("You are not authorized to pay for this booking");
        }

        if (booking.getStatus() == Booking.BookingStatus.CONFIRMED) {
            throw new BadRequestException("Booking is already paid and confirmed");
        }

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new BadRequestException("Cannot pay for a cancelled booking");
        }

        boolean isFailure = Boolean.TRUE.equals(request.getSimulateFailure());
        PaymentStatus paymentStatus = isFailure ? PaymentStatus.FAILED : PaymentStatus.SUCCESS;
        Booking.BookingStatus newBookingStatus = isFailure ? Booking.BookingStatus.FAILED : Booking.BookingStatus.CONFIRMED;

        String txnId = "TXN_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        Payment payment = Payment.builder()
                .transactionId(txnId)
                .booking(booking)
                .amount(booking.getAmount())
                .status(paymentStatus)
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "SIMULATED_CARD")
                .build();

        payment = paymentRepository.save(payment);

        booking.setStatus(newBookingStatus);
        bookingRepository.save(booking);

        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .transactionId(payment.getTransactionId())
                .bookingId(booking.getId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .bookingStatus(booking.getStatus().name())
                .message(isFailure ? "Payment failed. Booking status set to FAILED." : "Payment succeeded! Booking confirmed.")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Transactional
    public WebhookResponse handleWebhook(WebhookPayload payload) {
        // Idempotency check: if eventId has already been processed, return success without duplicate operations
        if (webhookRepository.existsByEventId(payload.getEventId())) {
            return WebhookResponse.builder()
                    .success(true)
                    .message("Event already processed (Idempotent response)")
                    .eventId(payload.getEventId())
                    .build();
        }

        Booking booking = bookingRepository.findById(payload.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + payload.getBookingId()));

        PaymentStatus paymentStatus = "FAILED".equalsIgnoreCase(payload.getStatus()) ? PaymentStatus.FAILED : PaymentStatus.SUCCESS;
        Booking.BookingStatus newBookingStatus = paymentStatus == PaymentStatus.SUCCESS ? Booking.BookingStatus.CONFIRMED : Booking.BookingStatus.FAILED;

        String txnId = payload.getTransactionId() != null ? payload.getTransactionId() : "WH_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        // Check if payment with transaction ID exists to prevent duplicate payment insertion
        if (paymentRepository.findByTransactionId(txnId).isEmpty()) {
            Payment payment = Payment.builder()
                    .transactionId(txnId)
                    .booking(booking)
                    .amount(payload.getAmount() != null ? payload.getAmount() : booking.getAmount())
                    .status(paymentStatus)
                    .paymentMethod("WEBHOOK_EVENT")
                    .build();
            paymentRepository.save(payment);
        }

        // Only update booking status if not already terminal or confirmed
        if (booking.getStatus() != Booking.BookingStatus.CONFIRMED) {
            booking.setStatus(newBookingStatus);
            bookingRepository.save(booking);
        }

        // Record processed webhook event ID
        ProcessedWebhook processedWebhook = ProcessedWebhook.builder()
                .eventId(payload.getEventId())
                .eventType(payload.getEventType())
                .build();
        webhookRepository.save(processedWebhook);

        return WebhookResponse.builder()
                .success(true)
                .message("Webhook processed successfully")
                .eventId(payload.getEventId())
                .build();
    }
}
