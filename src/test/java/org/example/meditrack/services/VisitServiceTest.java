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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

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
        // Given
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
    void getPatients_WithoutDoctorFilter_Success() {
        // Given
        ZonedDateTime visitStart = ZonedDateTime.parse("2024-07-01T10:00:00-04:00");
        ZonedDateTime visitEnd = ZonedDateTime.parse("2024-07-01T11:00:00-04:00");

        Object[] row = {
                1L, // patient_id
                "Jane", // patient_first_name
                "Smith", // patient_last_name
                1L, // visit_id
                Timestamp.from(visitStart.toInstant()), // start_date_time
                Timestamp.from(visitEnd.toInstant()), // end_date_time
                1L, // doctor_id
                "John", // doctor_first_name
                "Doe", // doctor_last_name
                "America/New_York", // timezone
                3 // patient_count
        };

        Page<Object[]> resultPage = new PageImpl<>(Collections.singletonList(row), PageRequest.of(0, 20), 1L);
        when(patientRepository.findAllPatientsWithData(null, PageRequest.of(0, 20)))
                .thenReturn(resultPage);

        // When
        PatientsListResponse response = visitService.getPatients(0, 20, null, null);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getCount());
        assertEquals(1, response.getData().size());

        assertEquals("Jane", response.getData().get(0).getFirstName());
        assertEquals("Smith", response.getData().get(0).getLastName());
        assertEquals(1, response.getData().get(0).getLastVisits().size());

        assertEquals("John", response.getData().get(0).getLastVisits().get(0).getDoctor().getFirstName());
        assertEquals(3, response.getData().get(0).getLastVisits().get(0).getDoctor().getTotalPatients());

        verify(patientRepository).findAllPatientsWithData(null, PageRequest.of(0, 20));
    }

    @Test
    void getPatients_WithDoctorFilter_Success() {
        // Given
        List<Long> doctorIds = Arrays.asList(1L, 2L);
        ZonedDateTime visitStart = ZonedDateTime.parse("2024-07-01T10:00:00-04:00");
        ZonedDateTime visitEnd = ZonedDateTime.parse("2024-07-01T11:00:00-04:00");

        Object[] row1 = {
                1L, "Jane", "Smith", 1L,
                Timestamp.from(visitStart.toInstant()),
                Timestamp.from(visitEnd.toInstant()),
                1L, "John", "Doe", "America/New_York", 3
        };

        Object[] row2 = {
                1L, "Jane", "Smith", 2L,
                Timestamp.from(visitStart.plusHours(2).toInstant()),
                Timestamp.from(visitEnd.plusHours(2).toInstant()),
                2L, "Alice", "Johnson", "Europe/London", 2
        };

        Page<Object[]> resultPage = new PageImpl<>(Arrays.asList(row1, row2),
                PageRequest.of(0, 20), 2);

        when(patientRepository.findPatientsWithAllData(null, doctorIds, 2, PageRequest.of(0, 20)))
                .thenReturn(resultPage);

        // When
        PatientsListResponse response = visitService.getPatients(0, 20, null, doctorIds);

        // Then
        assertNotNull(response);
        assertEquals(2, response.getCount());
        assertEquals(1, response.getData().size()); // One unique patient
        assertEquals(2, response.getData().get(0).getLastVisits().size()); // Two visits

        verify(patientRepository).findPatientsWithAllData(null, doctorIds, 2, PageRequest.of(0, 20));
    }

    @Test
    void getPatients_WithSearch_Success() {
        // Given
        String search = "jane";
        ZonedDateTime visitStart = ZonedDateTime.parse("2024-07-01T10:00:00-04:00");
        ZonedDateTime visitEnd = ZonedDateTime.parse("2024-07-01T11:00:00-04:00");

        Object[] row = {
                1L, "Jane", "Smith", 1L,
                Timestamp.from(visitStart.toInstant()),
                Timestamp.from(visitEnd.toInstant()),
                1L, "John", "Doe", "America/New_York", 3
        };

        Page<Object[]> resultPage = new PageImpl<>(Collections.singletonList(row), PageRequest.of(0, 20), 1);
        when(patientRepository.findAllPatientsWithData(search, PageRequest.of(0, 20)))
                .thenReturn(resultPage);

        // When
        PatientsListResponse response = visitService.getPatients(0, 20, search, null);

        // Then
        assertEquals(1, response.getCount());
        assertEquals("Jane", response.getData().get(0).getFirstName());

        verify(patientRepository).findAllPatientsWithData(search, PageRequest.of(0, 20));
    }

    @Test
    void getPatients_EmptyResults_Success() {
        // Given
        Page<Object[]> emptyPage = new PageImpl<>(Collections.emptyList(),
                PageRequest.of(0, 20), 0);
        when(patientRepository.findAllPatientsWithData(null, PageRequest.of(0, 20)))
                .thenReturn(emptyPage);

        // When
        PatientsListResponse response = visitService.getPatients(0, 20, null, null);

        // Then
        assertEquals(0, response.getCount());
        assertTrue(response.getData().isEmpty());

        verify(patientRepository).findAllPatientsWithData(null, PageRequest.of(0, 20));
    }

    @Test
    void getPatients_WithPagination_Success() {
        // Given
        Integer page = 1;
        Integer size = 10;

        Object[] row = {
                1L, "Jane", "Smith", 1L,
                Timestamp.from(ZonedDateTime.now().toInstant()),
                Timestamp.from(ZonedDateTime.now().plusHours(1).toInstant()),
                1L, "John", "Doe", "America/New_York", 3
        };

        Page<Object[]> resultPage = new PageImpl<>(Collections.singletonList(row), PageRequest.of(1, 10), 25);
        when(patientRepository.findAllPatientsWithData(null, PageRequest.of(1, 10)))
                .thenReturn(resultPage);

        // When
        PatientsListResponse response = visitService.getPatients(page, size, null, null);

        // Then
        assertEquals(25, response.getCount()); // Total elements
        assertEquals(1, response.getData().size()); // Elements on this page

        verify(patientRepository).findAllPatientsWithData(null, PageRequest.of(1, 10));
    }

    @Test
    void getPatients_DefaultPagination_Success() {
        // Given - no page/size parameters
        Object[] row = {
                1L, "Jane", "Smith", 1L,
                Timestamp.from(ZonedDateTime.now().toInstant()),
                Timestamp.from(ZonedDateTime.now().plusHours(1).toInstant()),
                1L, "John", "Doe", "America/New_York", 3
        };

        Page<Object[]> resultPage = new PageImpl<>(Collections.singletonList(row), PageRequest.of(0, 20), 1);
        when(patientRepository.findAllPatientsWithData(null, PageRequest.of(0, 20)))
                .thenReturn(resultPage);

        // When
        PatientsListResponse response = visitService.getPatients(null, null, null, null);

        // Then
        assertEquals(1, response.getCount());
        assertEquals(1, response.getData().size());

        verify(patientRepository).findAllPatientsWithData(null, PageRequest.of(0, 20));
    }

    @Test
    void getPatients_PatientWithoutVisits_Success() {
        // Given - patient without visits (visit_id is null)
        Object[] row = {
                1L, "Jane", "Smith", null, // visit_id is null
                null, null, null, null, null, null, null
        };

        Page<Object[]> resultPage = new PageImpl<>(Collections.singletonList(row), PageRequest.of(0, 20), 1);
        when(patientRepository.findAllPatientsWithData(null, PageRequest.of(0, 20)))
                .thenReturn(resultPage);

        // When
        PatientsListResponse response = visitService.getPatients(0, 20, null, null);

        // Then
        assertEquals(1, response.getCount());
        assertEquals(1, response.getData().size());
        assertEquals("Jane", response.getData().get(0).getFirstName());
        assertTrue(response.getData().get(0).getLastVisits().isEmpty()); // No visits

        verify(patientRepository).findAllPatientsWithData(null, PageRequest.of(0, 20));
    }
}