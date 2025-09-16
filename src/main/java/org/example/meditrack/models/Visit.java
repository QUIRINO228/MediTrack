package org.example.meditrack.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Table(name = "visits", indexes = {
        @Index(name = "idx_visit_doctor_time", columnList = "doctor_id, startDateTime"),
        @Index(name = "idx_visit_patient", columnList = "patient_id"),
        @Index(name = "idx_visit_start_time", columnList = "startDateTime")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Visit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private ZonedDateTime startDateTime;

    @NotNull
    @Column(nullable = false)
    private ZonedDateTime endDateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;
}
