package com.eve.healthcare.service;

import com.eve.healthcare.dto.BookingDtos.*;
import com.eve.healthcare.entity.Booking;
import com.eve.healthcare.entity.DiagnosticCentre;
import com.eve.healthcare.entity.DiagnosticTest;
import com.eve.healthcare.entity.User;
import com.eve.healthcare.exception.CustomExceptions.*;
import com.eve.healthcare.repository.BookingRepository;
import com.eve.healthcare.repository.DiagnosticCentreRepository;
import com.eve.healthcare.repository.DiagnosticTestRepository;
import com.eve.healthcare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final DiagnosticCentreRepository centreRepository;
    private final DiagnosticTestRepository testRepository;

    @Transactional
    public BookingResponse createBooking(String userEmail, CreateBookingRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        DiagnosticCentre centre = centreRepository.findById(request.getCentreId())
                .orElseThrow(() -> new ResourceNotFoundException("Diagnostic centre not found with id: " + request.getCentreId()));

        DiagnosticTest test = testRepository.findById(request.getTestId())
                .orElseThrow(() -> new ResourceNotFoundException("Diagnostic test not found with id: " + request.getTestId()));

        if (!test.getCentre().getId().equals(centre.getId())) {
            throw new BadRequestException("Diagnostic test ID " + test.getId() + " does not belong to Diagnostic Centre ID " + centre.getId());
        }

        Booking booking = Booking.builder()
                .user(user)
                .centre(centre)
                .test(test)
                .appointmentDateTime(request.getAppointmentDateTime())
                .amount(test.getPrice())
                .status(Booking.BookingStatus.PENDING)
                .build();

        booking = bookingRepository.save(booking);
        return mapToBookingResponse(booking);
    }

    public List<BookingResponse> getUserBookings(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        return bookingRepository.findByUserId(user.getId()).stream()
                .map(this::mapToBookingResponse)
                .collect(Collectors.toList());
    }

    public BookingResponse getBookingById(String userEmail, Long bookingId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (!booking.getUser().getId().equals(user.getId()) && user.getRole() != User.Role.ADMIN) {
            throw new UnauthorizedAccessException("You are not authorized to view this booking");
        }

        return mapToBookingResponse(booking);
    }

    @Transactional
    public BookingResponse cancelBooking(String userEmail, Long bookingId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (!booking.getUser().getId().equals(user.getId()) && user.getRole() != User.Role.ADMIN) {
            throw new UnauthorizedAccessException("You are not authorized to cancel this booking");
        }

        if (booking.getStatus() == Booking.BookingStatus.CONFIRMED) {
            throw new BadRequestException("Cannot cancel a confirmed booking directly. Please contact support.");
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        booking = bookingRepository.save(booking);
        return mapToBookingResponse(booking);
    }

    public BookingResponse mapToBookingResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .userId(booking.getUser().getId())
                .userName(booking.getUser().getFullName())
                .centreId(booking.getCentre().getId())
                .centreName(booking.getCentre().getName())
                .testId(booking.getTest().getId())
                .testName(booking.getTest().getName())
                .appointmentDateTime(booking.getAppointmentDateTime())
                .amount(booking.getAmount())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
