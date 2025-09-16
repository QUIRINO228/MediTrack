package org.example.meditrack.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class VisitResponse {
    private String start;
    private String end;
    private DoctorResponse doctor;
}
