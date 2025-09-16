package org.example.meditrack.services;

import lombok.RequiredArgsConstructor;
import org.example.meditrack.dtos.*;
import org.example.meditrack.exceptions.BusinessException;
import org.example.meditrack.models.Doctor;
import org.example.meditrack.models.Patient;
import org.example.meditrack.models.Visit;
import org.example.meditrack.repositories.DoctorRepository;
import org.example.meditrack.repositories.PatientRepository;
import org.example.meditrack.repositories.VisitRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VisitService {

    private final VisitRepository visitRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    @Transactional
    public void createVisit(CreateVisitRequest request) {
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new BusinessException("Doctor not found"));

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new BusinessException("Patient not found"));

        ZoneId doctorTimezone = ZoneId.of(doctor.getTimezone());

        ZonedDateTime startDateTime = ZonedDateTime.parse(request.getStart())
                .withZoneSameInstant(doctorTimezone);
        ZonedDateTime endDateTime = ZonedDateTime.parse(request.getEnd())
                .withZoneSameInstant(doctorTimezone);

        if (startDateTime.isAfter(endDateTime) || startDateTime.equals(endDateTime)) {
            throw new BusinessException("Start time must be before end time");
        }

        long overlappingVisits = visitRepository.countOverlappingVisits(
                doctor.getId(), startDateTime, endDateTime);

        if (overlappingVisits > 0) {
            throw new BusinessException("Doctor already has a visit scheduled at this time");
        }

        Visit visit = Visit.builder()
                .startDateTime(startDateTime)
                .endDateTime(endDateTime)
                .patient(patient)
                .doctor(doctor)
                .build();
        visitRepository.save(visit);
    }

    @Transactional(readOnly = true)
    public PatientsListResponse getPatients(Integer page, Integer size, String search, List<Long> doctorIds) {
        int actualPage = page != null ? page : 0;
        int actualSize = size != null ? size : 20;

        Pageable pageable = PageRequest.of(actualPage, actualSize);

        Page<Patient> patientsPage = patientRepository.findPatientsWithFilters(search, doctorIds, pageable);
        long totalCount = patientRepository.countPatientsWithFilters(search, doctorIds);

        if (patientsPage.isEmpty()) {
            return new PatientsListResponse(Collections.emptyList(), totalCount);
        }

        List<Long> patientIds = patientsPage.getContent().stream()
                .map(Patient::getId)
                .collect(Collectors.toList());

        List<Visit> latestVisits = visitRepository.findLatestVisitsForPatients(patientIds);

        Set<Long> allDoctorIds = latestVisits.stream()
                .map(v -> v.getDoctor().getId())
                .collect(Collectors.toSet());

        Map<Long, Integer> doctorPatientCounts = new HashMap<>();
        if (!allDoctorIds.isEmpty()) {
            List<Object[]> counts = visitRepository.countPatientsByDoctors(new ArrayList<>(allDoctorIds));
            for (Object[] count : counts) {
                doctorPatientCounts.put((Long) count[0], ((Number) count[1]).intValue());
            }
        }

        Map<Long, List<Visit>> visitsByPatient = latestVisits.stream()
                .collect(Collectors.groupingBy(v -> v.getPatient().getId()));

        List<PatientResponse> patientResponses = patientsPage.getContent().stream()
                .map(patient -> {
                    List<Visit> patientVisits = visitsByPatient.getOrDefault(patient.getId(), Collections.emptyList());
                    List<VisitResponse> visitResponses = patientVisits.stream()
                            .map(visit -> {
                                Doctor visitDoctor = visit.getDoctor();
                                ZoneId doctorTimezone = ZoneId.of(visitDoctor.getTimezone());

                                String startFormatted = visit.getStartDateTime()
                                        .withZoneSameInstant(doctorTimezone)
                                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                                String endFormatted = visit.getEndDateTime()
                                        .withZoneSameInstant(doctorTimezone)
                                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

                                DoctorResponse doctorResponse = new DoctorResponse(
                                        visitDoctor.getFirstName(),
                                        visitDoctor.getLastName(),
                                        doctorPatientCounts.getOrDefault(visitDoctor.getId(), 0)
                                );

                                return new VisitResponse(startFormatted, endFormatted, doctorResponse);
                            })
                            .collect(Collectors.toList());

                    return new PatientResponse(patient.getFirstName(), patient.getLastName(), visitResponses);
                })
                .collect(Collectors.toList());

        return new PatientsListResponse(patientResponses, totalCount);
    }
}
