package com.eve.healthcare.controller;

import com.eve.healthcare.dto.PaymentDtos.*;
import com.eve.healthcare.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Service & Webhook", description = "Endpoints for simulated payment processing and webhook integration")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Simulate payment processing for a booking")
    public ResponseEntity<PaymentResponse> processSimulatedPayment(@AuthenticationPrincipal UserDetails userDetails,
                                                                  @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.processSimulatedPayment(userDetails.getUsername(), request));
    }

    @PostMapping({"/webhook", "/webhook/"})
    @Operation(summary = "Idempotent payment webhook endpoint")
    public ResponseEntity<WebhookResponse> handleWebhook(@Valid @RequestBody WebhookPayload payload) {
        return ResponseEntity.ok(paymentService.handleWebhook(payload));
    }
}
