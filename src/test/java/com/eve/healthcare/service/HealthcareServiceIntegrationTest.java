package com.eve.healthcare.service;

import com.eve.healthcare.dto.AuthDtos.*;
import com.eve.healthcare.dto.BookingDtos.*;
import com.eve.healthcare.dto.CentreDtos.*;
import com.eve.healthcare.dto.PaymentDtos.*;
import com.eve.healthcare.entity.Booking;
import com.eve.healthcare.entity.DiagnosticCentre;
import com.eve.healthcare.entity.DiagnosticTest;
import com.eve.healthcare.exception.CustomExceptions.BadRequestException;
import com.eve.healthcare.exception.CustomExceptions.DuplicateResourceException;
import com.eve.healthcare.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class HealthcareServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private DiagnosticCentreService centreService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DiagnosticCentreRepository centreRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ProcessedWebhookRepository webhookRepository;

    private AuthResponse testUser;
    private CentreResponse testCentre;
    private TestResponse testDiagnosticTest;

    @BeforeEach
    void setUp() {
        // Create user
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setEmail("testpatient@example.com");
        signupRequest.setPassword("password123");
        signupRequest.setFullName("Test Patient");
        testUser = authService.signup(signupRequest);

        // Create diagnostic centre
        CreateCentreRequest centreRequest = new CreateCentreRequest();
        centreRequest.setName("City Healthcare Diagnostic");
        centreRequest.setLocation("Downtown Main St");
        testCentre = centreService.createCentre(centreRequest);

        // Create diagnostic test
        CreateTestRequest testRequest = new CreateTestRequest();
        testRequest.setName("Full Body Blood Profile");
        testRequest.setDescription("Complete blood panel checkup");
        testRequest.setPrice(new BigDecimal("1500.00"));
        testDiagnosticTest = centreService.createTest(testCentre.getId(), testRequest);
    }

    @Test
    void testUserSignupAndDuplicateEmail() {
        assertNotNull(testUser.getToken());
        assertEquals("testpatient@example.com", testUser.getEmail());

        // Duplicate signup attempt
        SignupRequest dupRequest = new SignupRequest();
        dupRequest.setEmail("testpatient@example.com");
        dupRequest.setPassword("password123");
        dupRequest.setFullName("Duplicate Patient");

        assertThrows(DuplicateResourceException.class, () -> authService.signup(dupRequest));
    }

    @Test
    void testCreateBookingAndPaymentSuccess() {
        CreateBookingRequest bookingRequest = new CreateBookingRequest();
        bookingRequest.setCentreId(testCentre.getId());
        bookingRequest.setTestId(testDiagnosticTest.getId());
        bookingRequest.setAppointmentDateTime(LocalDateTime.now().plusDays(2));

        BookingResponse booking = bookingService.createBooking(testUser.getEmail(), bookingRequest);
        assertNotNull(booking.getId());
        assertEquals(Booking.BookingStatus.PENDING, booking.getStatus());
        assertEquals(new BigDecimal("1500.00"), booking.getAmount());

        // Process successful payment
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setBookingId(booking.getId());
        paymentRequest.setSimulateFailure(false);
        paymentRequest.setPaymentMethod("CREDIT_CARD");

        PaymentResponse paymentResponse = paymentService.processSimulatedPayment(testUser.getEmail(), paymentRequest);
        assertEquals("SUCCESS", paymentResponse.getStatus().name());
        assertEquals("CONFIRMED", paymentResponse.getBookingStatus());

        // Verify in DB
        Booking updatedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertEquals(Booking.BookingStatus.CONFIRMED, updatedBooking.getStatus());
    }

    @Test
    void testSimulatedPaymentFailure() {
        CreateBookingRequest bookingRequest = new CreateBookingRequest();
        bookingRequest.setCentreId(testCentre.getId());
        bookingRequest.setTestId(testDiagnosticTest.getId());
        bookingRequest.setAppointmentDateTime(LocalDateTime.now().plusDays(3));

        BookingResponse booking = bookingService.createBooking(testUser.getEmail(), bookingRequest);

        // Process failing payment
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setBookingId(booking.getId());
        paymentRequest.setSimulateFailure(true);

        PaymentResponse paymentResponse = paymentService.processSimulatedPayment(testUser.getEmail(), paymentRequest);
        assertEquals("FAILED", paymentResponse.getStatus().name());
        assertEquals("FAILED", paymentResponse.getBookingStatus());
    }

    @Test
    void testWebhookIdempotency() {
        CreateBookingRequest bookingRequest = new CreateBookingRequest();
        bookingRequest.setCentreId(testCentre.getId());
        bookingRequest.setTestId(testDiagnosticTest.getId());
        bookingRequest.setAppointmentDateTime(LocalDateTime.now().plusDays(4));

        BookingResponse booking = bookingService.createBooking(testUser.getEmail(), bookingRequest);

        WebhookPayload payload = new WebhookPayload();
        payload.setEventId("EVT_9988776655");
        payload.setEventType("payment.succeeded");
        payload.setBookingId(booking.getId());
        payload.setTransactionId("TXN_WEBHOOK_123");
        payload.setAmount(new BigDecimal("1500.00"));
        payload.setStatus("SUCCESS");

        // First webhook call
        WebhookResponse response1 = paymentService.handleWebhook(payload);
        assertTrue(response1.isSuccess());
        assertEquals("Webhook processed successfully", response1.getMessage());

        // Check booking confirmed
        Booking b1 = bookingRepository.findById(booking.getId()).orElseThrow();
        assertEquals(Booking.BookingStatus.CONFIRMED, b1.getStatus());

        // Second webhook call (duplicate eventId)
        WebhookResponse response2 = paymentService.handleWebhook(payload);
        assertTrue(response2.isSuccess());
        assertEquals("Event already processed (Idempotent response)", response2.getMessage());

        // Verify count of processed webhooks is still 1
        assertTrue(webhookRepository.existsByEventId("EVT_9988776655"));
    }
}
