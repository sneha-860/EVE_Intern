package com.eve.healthcare.service;

import com.eve.healthcare.dto.CentreDtos.*;
import com.eve.healthcare.entity.DiagnosticCentre;
import com.eve.healthcare.entity.DiagnosticTest;
import com.eve.healthcare.exception.CustomExceptions.ResourceNotFoundException;
import com.eve.healthcare.repository.DiagnosticCentreRepository;
import com.eve.healthcare.repository.DiagnosticTestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiagnosticCentreService {

    private final DiagnosticCentreRepository centreRepository;
    private final DiagnosticTestRepository testRepository;

    @Transactional
    public CentreResponse createCentre(CreateCentreRequest request) {
        DiagnosticCentre centre = DiagnosticCentre.builder()
                .name(request.getName())
                .location(request.getLocation())
                .build();
        centre = centreRepository.save(centre);
        return mapToCentreResponse(centre);
    }

    public List<CentreResponse> getAllCentres() {
        return centreRepository.findAll().stream()
                .map(this::mapToCentreResponse)
                .collect(Collectors.toList());
    }

    public CentreResponse getCentreById(Long id) {
        DiagnosticCentre centre = centreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Diagnostic centre not found with id: " + id));
        return mapToCentreResponse(centre);
    }

    @Transactional
    public TestResponse createTest(Long centreId, CreateTestRequest request) {
        DiagnosticCentre centre = centreRepository.findById(centreId)
                .orElseThrow(() -> new ResourceNotFoundException("Diagnostic centre not found with id: " + centreId));

        DiagnosticTest test = DiagnosticTest.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .centre(centre)
                .build();

        test = testRepository.save(test);
        return mapToTestResponse(test);
    }

    public List<TestResponse> getTestsByCentreId(Long centreId) {
        if (!centreRepository.existsById(centreId)) {
            throw new ResourceNotFoundException("Diagnostic centre not found with id: " + centreId);
        }
        return testRepository.findByCentreId(centreId).stream()
                .map(this::mapToTestResponse)
                .collect(Collectors.toList());
    }

    private CentreResponse mapToCentreResponse(DiagnosticCentre centre) {
        List<TestResponse> testResponses = centre.getTests() == null ? List.of() :
                centre.getTests().stream().map(this::mapToTestResponse).collect(Collectors.toList());

        return CentreResponse.builder()
                .id(centre.getId())
                .name(centre.getName())
                .location(centre.getLocation())
                .tests(testResponses)
                .build();
    }

    private TestResponse mapToTestResponse(DiagnosticTest test) {
        return TestResponse.builder()
                .id(test.getId())
                .name(test.getName())
                .description(test.getDescription())
                .price(test.getPrice())
                .centreId(test.getCentre().getId())
                .build();
    }
}
