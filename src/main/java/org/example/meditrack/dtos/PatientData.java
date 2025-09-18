package org.example.meditrack.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PatientData {
    private String firstName;
    private String lastName;
    private List<VisitData> visits;

    public void addVisit(VisitData visitData) {
        this.visits.add(visitData);
    }
}