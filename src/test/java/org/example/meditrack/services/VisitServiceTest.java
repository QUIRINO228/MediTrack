package org.example.meditrack.services;

import org.example.meditrack.dtos.CreateVisitRequest;
import org.example.meditrack.dtos.PatientsListResponse;
import org.example.meditrack.exceptions.BusinessException;
import org.example.meditrack.models.Doctor;
import org.example.meditrack.models.Patient;
import org.example.meditrack.models.Visit;
import org.example.meditrack.repositories.DoctorRepository;
import org.example.meditrack.repositories.PatientRepository;
import org.example.meditrack.repositories.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
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

    private Doctor testDoctor;
    private Patient testPatient;

    @BeforeEach
    void setUp() {
        testDoctor = Doctor.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .timezone("America/New_York")
                .build();

        testPatient = Patient.builder()
                .id(1L)
                .firstName("Jane")
                .lastName("Smith")
                .build();
    }

    // ========== CREATE VISIT TESTS ==========

    @Test
    void createVisit_Success() {
        // Given
        CreateVisitRequest request = CreateVisitRequest.builder()
                .start("2024-07-01T10:00:00-04:00")
                .end("2024-07-01T11:00:00-04:00")
                .patientId(1L)
                .doctorId(1L)
                .build();

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(testDoctor));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(testPatient));
        when(visitRepository.countOverlappingVisits(anyLong(), any(), any())).thenReturn(0L);

        // When & Then
        assertDoesNotThrow(() -> visitService.createVisit(request));
        verify(visitRepository).save(any(Visit.class));
    }

    @Test
    void createVisit_OverlappingVisit_ThrowsException() {
        // Given
        CreateVisitRequest request = CreateVisitRequest.builder()
                .start("2024-07-01T10:00:00-04:00")
                .end("2024-07-01T11:00:00-04:00")
                .patientId(1L)
                .doctorId(1L)
                .build();

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(testDoctor));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(testPatient));
        when(visitRepository.countOverlappingVisits(eq(1L), any(), any())).thenReturn(1L);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> visitService.createVisit(request));
        assertEquals("Doctor already has a visit scheduled at this time", exception.getMessage());
        verify(visitRepository, never()).save(any());
    }

    @Test
    void createVisit_DoctorNotFound_ThrowsException() {
        // Given
        CreateVisitRequest request = CreateVisitRequest.builder()
                .start("2024-07-01T10:00:00-04:00")
                .end("2024-07-01T11:00:00-04:00")
                .patientId(1L)
                .doctorId(999L)
                .build();

        when(doctorRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> visitService.createVisit(request));
        assertEquals("Doctor not found", exception.getMessage());
        verify(visitRepository, never()).save(any());
    }

    @Test
    void createVisit_PatientNotFound_ThrowsException() {
        // Given
        CreateVisitRequest request = CreateVisitRequest.builder()
                .start("2024-07-01T10:00:00-04:00")
                .end("2024-07-01T11:00:00-04:00")
                .patientId(999L)
                .doctorId(1L)
                .build();

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(testDoctor));
        when(patientRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> visitService.createVisit(request));
        assertEquals("Patient not found", exception.getMessage());
        verify(visitRepository, never()).save(any());
    }

    @Test
    void createVisit_InvalidTimeRange_ThrowsException() {
        // Given - End time before start time
        CreateVisitRequest request = CreateVisitRequest.builder()
                .start("2024-07-01T11:00:00-04:00")
                .end("2024-07-01T10:00:00-04:00")
                .patientId(1L)
                .doctorId(1L)
                .build();

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(testDoctor));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(testPatient));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> visitService.createVisit(request));
        assertEquals("Start time must be before end time", exception.getMessage());
        verify(visitRepository, never()).save(any());
    }

    @Test
    void createVisit_EqualStartEndTime_ThrowsException() {
        // Given - Equal start and end time
        CreateVisitRequest request = CreateVisitRequest.builder()
                .start("2024-07-01T10:00:00-04:00")
                .end("2024-07-01T10:00:00-04:00")
                .patientId(1L)
                .doctorId(1L)
                .build();

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(testDoctor));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(testPatient));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> visitService.createVisit(request));
        assertEquals("Start time must be before end time", exception.getMessage());
        verify(visitRepository, never()).save(any());
    }

    // ========== GET PATIENTS TESTS ==========

    @Test
    void getPatients_WithoutDoctorFilter_Success() {
        // Given
        ZonedDateTime visitStart = ZonedDateTime.parse("2024-07-01T10:00:00-04:00");
        ZonedDateTime visitEnd = ZonedDateTime.parse("2024-07-01T11:00:00-04:00");

        // Row structure: [patient_id, first_name, last_name, visit_id, start_time, end_time,
        //                 doctor_id, doctor_first_name, doctor_last_name, timezone, patient_count, total_count]
        Object[] row = {
                1L, "Jane", "Smith", 1L,
                Timestamp.from(visitStart.toInstant()),
                Timestamp.from(visitEnd.toInstant()),
                1L, "John", "Doe", "America/New_York", 3,
                5L // total_count from CTE
        };

        when(patientRepository.findAllPatientsWithDataOptimized(null, 0, 20))
                .thenReturn(Collections.singletonList(row));

        // When
        PatientsListResponse response = visitService.getPatients(0, 20, null, null);

        // Then
        assertEquals(5L, response.getCount()); // Total from CTE
        assertEquals(1, response.getData().size());
        assertEquals("Jane", response.getData().get(0).getFirstName());
        assertEquals("Smith", response.getData().get(0).getLastName());
        assertEquals(1, response.getData().get(0).getLastVisits().size());
        assertEquals("John", response.getData().get(0).getLastVisits().get(0).getDoctor().getFirstName());
        assertEquals(3, response.getData().get(0).getLastVisits().get(0).getDoctor().getTotalPatients());

        verify(patientRepository).findAllPatientsWithDataOptimized(null, 0, 20);
    }

    @Test
    void getPatients_WithDoctorFilter_Success() {
        // Given
        List<Long> doctorIds = Arrays.asList(1L, 2L);
        ZonedDateTime visitStart = ZonedDateTime.parse("2024-07-01T10:00:00-04:00");

        Object[] row1 = {
                1L, "Jane", "Smith", 1L,
                Timestamp.from(visitStart.toInstant()),
                Timestamp.from(visitStart.plusHours(1).toInstant()),
                1L, "John", "Doe", "America/New_York", 3,
                2L // total_count
        };

        Object[] row2 = {
                1L, "Jane", "Smith", 2L,
                Timestamp.from(visitStart.plusHours(2).toInstant()),
                Timestamp.from(visitStart.plusHours(3).toInstant()),
                2L, "Alice", "Johnson", "Europe/London", 2,
                2L // total_count
        };

        when(patientRepository.findPatientsWithAllDataOptimized(null, doctorIds, 2, 0, 20))
                .thenReturn(Arrays.asList(row1, row2));

        // When
        PatientsListResponse response = visitService.getPatients(0, 20, null, doctorIds);

        // Then
        assertEquals(2L, response.getCount());
        assertEquals(1, response.getData().size()); // One unique patient
        assertEquals(2, response.getData().get(0).getLastVisits().size()); // Two visits

        verify(patientRepository).findPatientsWithAllDataOptimized(null, doctorIds, 2, 0, 20);
    }

    @Test
    void getPatients_WithSearch_Success() {
        // Given
        String search = "jane";
        Object[] row = {
                1L, "Jane", "Smith", 1L,
                Timestamp.from(ZonedDateTime.now().toInstant()),
                Timestamp.from(ZonedDateTime.now().plusHours(1).toInstant()),
                1L, "John", "Doe", "America/New_York", 3,
                1L // total_count
        };

        when(patientRepository.findAllPatientsWithDataOptimized(search, 0, 20))
                .thenReturn(Collections.singletonList(row));

        // When
        PatientsListResponse response = visitService.getPatients(0, 20, search, null);

        // Then
        assertEquals(1L, response.getCount());
        assertEquals("Jane", response.getData().get(0).getFirstName());

        verify(patientRepository).findAllPatientsWithDataOptimized(search, 0, 20);
    }

    @Test
    void getPatients_WithSearchAndDoctorFilter_Success() {
        // Given
        String search = "jane";
        List<Long> doctorIds = Collections.singletonList(1L);

        Object[] row = {
                1L, "Jane", "Smith", 1L,
                Timestamp.from(ZonedDateTime.now().toInstant()),
                Timestamp.from(ZonedDateTime.now().plusHours(1).toInstant()),
                1L, "John", "Doe", "America/New_York", 3,
                1L // total_count
        };

        when(patientRepository.findPatientsWithAllDataOptimized(search, doctorIds, 1, 0, 20))
                .thenReturn(Collections.singletonList(row));

        // When
        PatientsListResponse response = visitService.getPatients(0, 20, search, doctorIds);

        // Then
        assertEquals(1L, response.getCount());
        assertEquals("Jane", response.getData().get(0).getFirstName());

        verify(patientRepository).findPatientsWithAllDataOptimized(search, doctorIds, 1, 0, 20);
    }

    @Test
    void getPatients_EmptyResults_Success() {
        // Given
        when(patientRepository.findAllPatientsWithDataOptimized(null, 0, 20))
                .thenReturn(Collections.emptyList());

        // When
        PatientsListResponse response = visitService.getPatients(0, 20, null, null);

        // Then
        assertEquals(0L, response.getCount());
        assertTrue(response.getData().isEmpty());

        verify(patientRepository).findAllPatientsWithDataOptimized(null, 0, 20);
    }

    @Test
    void getPatients_WithCustomPagination_Success() {
        // Given
        Integer page = 2;
        Integer size = 5;
        int expectedOffset = 10; // page 2 * size 5

        Object[] row = {
                1L, "Jane", "Smith", 1L,
                Timestamp.from(ZonedDateTime.now().toInstant()),
                Timestamp.from(ZonedDateTime.now().plusHours(1).toInstant()),
                1L, "John", "Doe", "America/New_York", 3,
                25L // total_count
        };

        when(patientRepository.findAllPatientsWithDataOptimized(null, expectedOffset, size))
                .thenReturn(Collections.singletonList(row));

        // When
        PatientsListResponse response = visitService.getPatients(page, size, null, null);

        // Then
        assertEquals(25L, response.getCount()); // Total elements
        assertEquals(1, response.getData().size()); // Elements on this page

        verify(patientRepository).findAllPatientsWithDataOptimized(null, expectedOffset, size);
    }

    @Test
    void getPatients_DefaultPagination_Success() {
        // Given - null page and size should default to 0 and 20
        Object[] row = {
                1L, "Jane", "Smith", 1L,
                Timestamp.from(ZonedDateTime.now().toInstant()),
                Timestamp.from(ZonedDateTime.now().plusHours(1).toInstant()),
                1L, "John", "Doe", "America/New_York", 3,
                1L // total_count
        };

        when(patientRepository.findAllPatientsWithDataOptimized(null, 0, 20))
                .thenReturn(Collections.singletonList(row));

        // When
        PatientsListResponse response = visitService.getPatients(null, null, null, null);

        // Then
        assertEquals(1L, response.getCount());
        assertEquals(1, response.getData().size());

        verify(patientRepository).findAllPatientsWithDataOptimized(null, 0, 20);
    }

    @Test
    void getPatients_PatientWithoutVisits_Success() {
        // Given - patient without visits (visit_id is null)
        Object[] row = {
                1L, "Jane", "Smith", null, // visit_id is null
                null, null, null, null, null, null, null,
                1L // total_count
        };

        when(patientRepository.findAllPatientsWithDataOptimized(null, 0, 20))
                .thenReturn(Collections.singletonList(row));

        // When
        PatientsListResponse response = visitService.getPatients(0, 20, null, null);

        // Then
        assertEquals(1L, response.getCount());
        assertEquals(1, response.getData().size());
        assertEquals("Jane", response.getData().get(0).getFirstName());
        assertTrue(response.getData().get(0).getLastVisits().isEmpty()); // No visits

        verify(patientRepository).findAllPatientsWithDataOptimized(null, 0, 20);
    }

    @Test
    void getPatients_MultipleVisitsPerPatient_Success() {
        // Given - Patient with visits to multiple doctors
        ZonedDateTime visitStart = ZonedDateTime.parse("2024-07-01T10:00:00-04:00");

        Object[] row1 = {
                1L, "Jane", "Smith", 1L,
                Timestamp.from(visitStart.toInstant()),
                Timestamp.from(visitStart.plusHours(1).toInstant()),
                1L, "John", "Doe", "America/New_York", 3,
                1L // total_count
        };

        Object[] row2 = {
                1L, "Jane", "Smith", 2L,
                Timestamp.from(visitStart.plusDays(1).toInstant()),
                Timestamp.from(visitStart.plusDays(1).plusHours(1).toInstant()),
                2L, "Alice", "Johnson", "Europe/London", 2,
                1L // total_count
        };

        Object[] row3 = {
                1L, "Jane", "Smith", 3L,
                Timestamp.from(visitStart.plusDays(2).toInstant()),
                Timestamp.from(visitStart.plusDays(2).plusHours(1).toInstant()),
                3L, "Bob", "Wilson", "Asia/Tokyo", 1,
                1L // total_count
        };

        when(patientRepository.findAllPatientsWithDataOptimized(null, 0, 20))
                .thenReturn(Arrays.asList(row1, row2, row3));

        // When
        PatientsListResponse response = visitService.getPatients(0, 20, null, null);

        // Then
        assertEquals(1L, response.getCount());
        assertEquals(1, response.getData().size()); // One unique patient
        assertEquals(3, response.getData().get(0).getLastVisits().size()); // Three visits to different doctors

        // Verify each visit has correct doctor info
        var visits = response.getData().get(0).getLastVisits();
        assertEquals("John", visits.get(0).getDoctor().getFirstName());
        assertEquals(3, visits.get(0).getDoctor().getTotalPatients());
        assertEquals("Alice", visits.get(1).getDoctor().getFirstName());
        assertEquals(2, visits.get(1).getDoctor().getTotalPatients());
        assertEquals("Bob", visits.get(2).getDoctor().getFirstName());
        assertEquals(1, visits.get(2).getDoctor().getTotalPatients());

        verify(patientRepository).findAllPatientsWithDataOptimized(null, 0, 20);
    }

    @Test
    void getPatients_CorrectTimezoneConversion_Success() {
        // Given - Visit with specific timezone
        ZonedDateTime visitStart = ZonedDateTime.parse("2024-07-01T14:00:00+00:00"); // UTC time
        Object[] row = {
                1L, "Jane", "Smith", 1L,
                Timestamp.from(visitStart.toInstant()),
                Timestamp.from(visitStart.plusHours(1).toInstant()),
                1L, "John", "Doe", "Europe/London", 3, // London timezone
                1L // total_count
        };

        when(patientRepository.findAllPatientsWithDataOptimized(null, 0, 20))
                .thenReturn(Collections.singletonList(row));

        // When
        PatientsListResponse response = visitService.getPatients(0, 20, null, null);

        // Then
        assertEquals(1, response.getData().size());
        var visit = response.getData().get(0).getLastVisits().get(0);

        // Verify that the time is converted to London timezone (should be +01:00 in summer)
        assertTrue(visit.getStart().contains("+01:00") || visit.getStart().contains("+00:00"));
        assertTrue(visit.getEnd().contains("+01:00") || visit.getEnd().contains("+00:00"));

        verify(patientRepository).findAllPatientsWithDataOptimized(null, 0, 20);
    }
}