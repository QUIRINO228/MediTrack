package org.example.meditrack.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class CreateVisitRequest {

    @NotBlank(message = "Start time is required")
    private String start;

    @NotBlank(message = "End time is required")
    private String end;

    @NotNull(message = "Patient ID is required")
    @JsonProperty("patientId")
    private Long patientId;

    @NotNull(message = "Doctor ID is required")
    @JsonProperty("doctorId")
    private Long doctorId;
}
