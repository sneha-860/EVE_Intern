package com.eve.healthcare.controller;

import com.eve.healthcare.dto.CentreDtos.*;
import com.eve.healthcare.service.DiagnosticCentreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/centres")
@RequiredArgsConstructor
@Tag(name = "Diagnostic Centres & Tests", description = "Endpoints to manage centres and tests")
public class DiagnosticCentreController {

    private final DiagnosticCentreService centreService;

    @PostMapping
    @Operation(summary = "Create a new diagnostic centre")
    public ResponseEntity<CentreResponse> createCentre(@Valid @RequestBody CreateCentreRequest request) {
        return new ResponseEntity<>(centreService.createCentre(request), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all diagnostic centres")
    public ResponseEntity<List<CentreResponse>> getAllCentres() {
        return ResponseEntity.ok(centreService.getAllCentres());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get diagnostic centre by ID")
    public ResponseEntity<CentreResponse> getCentreById(@PathVariable Long id) {
        return ResponseEntity.ok(centreService.getCentreById(id));
    }

    @PostMapping("/{centreId}/tests")
    @Operation(summary = "Add a test to a diagnostic centre")
    public ResponseEntity<TestResponse> createTest(@PathVariable Long centreId, @Valid @RequestBody CreateTestRequest request) {
        return new ResponseEntity<>(centreService.createTest(centreId, request), HttpStatus.CREATED);
    }

    @GetMapping("/{centreId}/tests")
    @Operation(summary = "Get all tests offered by a centre")
    public ResponseEntity<List<TestResponse>> getTestsByCentreId(@PathVariable Long centreId) {
        return ResponseEntity.ok(centreService.getTestsByCentreId(centreId));
    }
}
