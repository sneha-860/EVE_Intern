package com.eve.healthcare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

public class CentreDtos {

    @Data
    public static class CreateCentreRequest {
        @NotBlank(message = "Centre name is required")
        private String name;

        @NotBlank(message = "Location is required")
        private String location;
    }

    @Data
    public static class CreateTestRequest {
        @NotBlank(message = "Test name is required")
        private String name;

        private String description;

        @NotNull(message = "Price is required")
        @Positive(message = "Price must be greater than zero")
        private BigDecimal price;
    }

    @Data
    @Builder
    public static class TestResponse {
        private Long id;
        private String name;
        private String description;
        private BigDecimal price;
        private Long centreId;
    }

    @Data
    @Builder
    public static class CentreResponse {
        private Long id;
        private String name;
        private String location;
        private List<TestResponse> tests;
    }
}
