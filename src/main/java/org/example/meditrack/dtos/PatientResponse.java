package org.example.meditrack.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class PatientResponse {
    private String firstName;
    private String lastName;
    private List<VisitResponse> lastVisits;
}
