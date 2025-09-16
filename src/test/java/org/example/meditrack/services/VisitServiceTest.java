package org.example.meditrack.services;

import lombok.val;
import org.example.meditrack.dtos.CreateVisitRequest;
import org.example.meditrack.models.Doctor;
import org.example.meditrack.models.Patient;
import org.example.meditrack.models.Visit;
import org.example.meditrack.repositories.DoctorRepository;
import org.example.meditrack.exceptions.BusinessException;
import org.example.meditrack.repositories.PatientRepository;
import org.example.meditrack.repositories.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VisitServiceTest {

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @InjectMocks
    private VisitService visitService;

    // Test data generation
    private List<Doctor> testDoctors;
    private List<Patient> testPatients;
    private List<Visit> testVisits;

    @BeforeEach
    void setUp() {
        // Generate test doctors
        testDoctors = new ArrayList<>();
        testDoctors.add(Doctor.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .timezone("America/New_York")
                .build());
        testDoctors.add(Doctor.builder()
                .id(2L)
                .firstName("Alice")
                .lastName("Smith")
                .timezone("Europe/London")
                .build());
        testDoctors.add(Doctor.builder()
                .id(3L)
                .firstName("Bob")
                .lastName("Johnson")
                .timezone("Asia/Tokyo")
                .build());

        // Generate test patients
        testPatients = new ArrayList<>();
        testPatients.add(Patient.builder()
                .id(1L)
                .firstName("Jane")
                .lastName("Doe")
                .build());
        testPatients.add(Patient.builder()
                .id(2L)
                .firstName("Michael")
                .lastName("Brown")
                .build());
        testPatients.add(Patient.builder()
                .id(3L)
                .firstName("Emily")
                .lastName("Davis")
                .build());
        testPatients.add(Patient.builder()
                .id(4L)
                .firstName("David")
                .lastName("Wilson")
                .build());

        // Generate test visits with cross-relations:
        // - Doctor 1 has patients 1,2,3
        // - Doctor 2 has patients 1,3,4
        // - Doctor 3 has patients 2,4
        // - Patient 1 visits doctors 1,2
        // - Patient 2 visits doctors 1,3
        // - Patient 3 visits doctors 1,2
        // - Patient 4 visits doctors 2,3
        testVisits = new ArrayList<>();
        // Visits for Doctor 1
        testVisits.add(createVisit(1L, testDoctors.get(0), testPatients.get(0), "2024-06-01T10:00:00-04:00", "2024-06-01T11:00:00-04:00"));
        testVisits.add(createVisit(2L, testDoctors.get(0), testPatients.get(1), "2024-06-02T10:00:00-04:00", "2024-06-02T11:00:00-04:00"));
        testVisits.add(createVisit(3L, testDoctors.get(0), testPatients.get(2), "2024-06-03T10:00:00-04:00", "2024-06-03T11:00:00-04:00"));
        // Visits for Doctor 2
        testVisits.add(createVisit(4L, testDoctors.get(1), testPatients.get(0), "2024-06-01T15:00:00+01:00", "2024-06-01T16:00:00+01:00"));
        testVisits.add(createVisit(5L, testDoctors.get(1), testPatients.get(2), "2024-06-02T15:00:00+01:00", "2024-06-02T16:00:00+01:00"));
        testVisits.add(createVisit(6L, testDoctors.get(1), testPatients.get(3), "2024-06-03T15:00:00+01:00", "2024-06-03T16:00:00+01:00"));
        // Visits for Doctor 3
        testVisits.add(createVisit(7L, testDoctors.get(2), testPatients.get(1), "2024-06-01T18:00:00+09:00", "2024-06-01T19:00:00+09:00"));
        testVisits.add(createVisit(8L, testDoctors.get(2), testPatients.get(3), "2024-06-02T18:00:00+09:00", "2024-06-02T19:00:00+09:00"));

        // Note: For simplicity, we assume the latest visits are the ones with higher IDs for each patient-doctor pair.
    }

    private Visit createVisit(Long id, Doctor doctor, Patient patient, String start, String end) {
        ZonedDateTime startDateTime = ZonedDateTime.parse(start);
        ZonedDateTime endDateTime = ZonedDateTime.parse(end);
        return Visit.builder()
                .id(id)
                .startDateTime(startDateTime)
                .endDateTime(endDateTime)
                .doctor(doctor)
                .patient(patient)
                .build();
    }

    @Test
    void createVisit_Success_WithMultipleDoctorsAndPatients() {
        // Test creating a new visit without overlap
        CreateVisitRequest request = CreateVisitRequest.builder()
                .start("2024-07-01T10:00:00-04:00")
                .end("2024-07-01T11:00:00-04:00")
                .patientId(1L)
                .doctorId(1L)
                .build();

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(testDoctors.get(0)));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(testPatients.get(0)));
        when(visitRepository.countOverlappingVisits(anyLong(), any(), any())).thenReturn(0L);

        assertDoesNotThrow(() -> visitService.createVisit(request));
        verify(visitRepository).save(any(Visit.class));
    }

    @Test
    void createVisit_Overlapping_WithExistingVisits() {
        // Test overlapping with existing visit for Doctor 1
        CreateVisitRequest overlappingRequest = CreateVisitRequest.builder()
                .start("2024-06-01T10:30:00-04:00")
                .end("2024-06-01T11:30:00-04:00")
                .patientId(2L)
                .doctorId(1L)
                .build();

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(testDoctors.get(0)));
        when(patientRepository.findById(2L)).thenReturn(Optional.of(testPatients.get(1)));
        when(visitRepository.countOverlappingVisits(eq(1L), any(), any())).thenReturn(1L); // Fixed: Use eq(1L)

        val exception = assertThrows(BusinessException.class,
                () -> visitService.createVisit(overlappingRequest));
        assertEquals("Doctor already has a visit scheduled at this time", exception.getMessage());
        verify(visitRepository, never()).save(any());
    }

    @Test
    void getPatients_Success_WithMultipleRelations() {
        // Given: All patients, no filters
        Page<Patient> patientsPage = new PageImpl<>(testPatients);
        when(patientRepository.findPatientsWithFilters(null, null, PageRequest.of(0, 20)))
                .thenReturn(patientsPage);
        when(patientRepository.countPatientsWithFilters(null, null))
                .thenReturn(4L);
        when(visitRepository.findLatestVisitsForPatients(anyList()))
                .thenReturn(testVisits);
        // Simulate countPatientsByDoctors: Doctor1:3 patients, Doctor2:3, Doctor3:2
        when(visitRepository.countPatientsByDoctors(anyList()))
                .thenReturn(Arrays.asList(
                        new Object[]{1L, 3L},
                        new Object[]{2L, 3L},
                        new Object[]{3L, 2L}
                ));

        // When
        val response = visitService.getPatients(0, 20, null, null);

        // Then
        assertNotNull(response);
        assertEquals(4, response.getCount());
        assertEquals(4, response.getData().size());

        // Verify patient 1 (Jane Doe) has visits with Doctor1 and Doctor2
        val patient1 = response.getData().stream()
                .filter(p -> p.getFirstName().equals("Jane") && p.getLastName().equals("Doe"))
                .findFirst().orElse(null);
        assertNotNull(patient1);
        assertEquals(2, patient1.getLastVisits().size()); // One latest per doctor

        // Verify doctor patient counts
        val visitForDoctor1 = patient1.getLastVisits().stream()
                .filter(v -> v.getDoctor().getFirstName().equals("John"))
                .findFirst().orElse(null);
        assertNotNull(visitForDoctor1);
        assertEquals(3, visitForDoctor1.getDoctor().getTotalPatients());

        // Similar checks for other patients...
        // Patient 2 (Michael Brown) visits Doctor1 and Doctor3
        val patient2 = response.getData().stream()
                .filter(p -> p.getFirstName().equals("Michael") && p.getLastName().equals("Brown"))
                .findFirst().orElse(null);
        assertNotNull(patient2);
        assertEquals(2, patient2.getLastVisits().size());

        // Patient 3 (Emily Davis) visits Doctor1 and Doctor2
        val patient3 = response.getData().stream()
                .filter(p -> p.getFirstName().equals("Emily") && p.getLastName().equals("Davis"))
                .findFirst().orElse(null);
        assertNotNull(patient3);
        assertEquals(2, patient3.getLastVisits().size());

        // Patient 4 (David Wilson) visits Doctor2 and Doctor3
        val patient4 = response.getData().stream()
                .filter(p -> p.getFirstName().equals("David") && p.getLastName().equals("Wilson"))
                .findFirst().orElse(null);
        assertNotNull(patient4);
        assertEquals(2, patient4.getLastVisits().size());
    }

    @Test
    void getPatients_WithDoctorFilter() {
        // Given: Filter by doctorIds 1 and 2
        List<Long> doctorIds = Arrays.asList(1L, 2L);
        // Patients associated with these doctors: 1,2,3,4 (all, but verify query)
        Page<Patient> patientsPage = new PageImpl<>(testPatients.subList(0, 3)); // Assume first 3
        when(patientRepository.findPatientsWithFilters(null, doctorIds, PageRequest.of(0, 20)))
                .thenReturn(patientsPage);
        when(patientRepository.countPatientsWithFilters(null, doctorIds))
                .thenReturn(3L);
        when(visitRepository.findLatestVisitsForPatients(anyList()))
                .thenReturn(testVisits.subList(0, 6)); // Visits related to doctors 1 and 2
        when(visitRepository.countPatientsByDoctors(anyList()))
                .thenReturn(Arrays.asList(
                        new Object[]{1L, 3L},
                        new Object[]{2L, 3L}
                ));

        // When
        val response = visitService.getPatients(0, 20, null, doctorIds);

        // Then
        assertEquals(3, response.getCount());
        assertEquals(3, response.getData().size());
        // Further assertions on data...
    }

    @Test
    void getPatients_WithSearchTerm() {
        // Given: Search for "doe" (should match Jane Doe)
        String search = "doe";
        Page<Patient> patientsPage = new PageImpl<>(Collections.singletonList(testPatients.get(0)));
        when(patientRepository.findPatientsWithFilters(search, null, PageRequest.of(0, 20)))
                .thenReturn(patientsPage);
        when(patientRepository.countPatientsWithFilters(search, null))
                .thenReturn(1L);
        when(visitRepository.findLatestVisitsForPatients(anyList()))
                .thenReturn(Arrays.asList(testVisits.get(0), testVisits.get(3))); // Visits for patient 1
        when(visitRepository.countPatientsByDoctors(anyList()))
                .thenReturn(Arrays.asList(
                        new Object[]{1L, 3L},
                        new Object[]{2L, 3L}
                ));

        // When
        val response = visitService.getPatients(0, 20, search, null);

        // Then
        assertEquals(1, response.getCount());
        assertEquals(1, response.getData().size());
        val patient = response.getData().get(0);
        assertEquals("Jane", patient.getFirstName());
        assertEquals(2, patient.getLastVisits().size());
    }
}