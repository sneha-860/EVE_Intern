package com.eve.healthcare.controller;

import com.eve.healthcare.dto.BookingDtos.*;
import com.eve.healthcare.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking System", description = "Endpoints for booking diagnostic tests")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "Book a diagnostic test")
    public ResponseEntity<BookingResponse> createBooking(@AuthenticationPrincipal UserDetails userDetails,
                                                        @Valid @RequestBody CreateBookingRequest request) {
        return new ResponseEntity<>(bookingService.createBooking(userDetails.getUsername(), request), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get user's diagnostic test bookings")
    public ResponseEntity<List<BookingResponse>> getUserBookings(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(bookingService.getUserBookings(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking details by ID")
    public ResponseEntity<BookingResponse> getBookingById(@AuthenticationPrincipal UserDetails userDetails,
                                                          @PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(userDetails.getUsername(), id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a booking")
    public ResponseEntity<BookingResponse> cancelBooking(@AuthenticationPrincipal UserDetails userDetails,
                                                         @PathVariable Long id) {
        return ResponseEntity.ok(bookingService.cancelBooking(userDetails.getUsername(), id));
    }
}
