package org.example.meditrack.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class VisitData {
    private ZonedDateTime startDateTime;
    private ZonedDateTime endDateTime;
    private String doctorFirstName;
    private String doctorLastName;
    private String timezone;
    private Integer patientCount; 
}
