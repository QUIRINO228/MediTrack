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

import java.sql.Timestamp;
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

        Page<Object[]> resultPage;

        if (doctorIds == null || doctorIds.isEmpty()) {
            resultPage = patientRepository.findAllPatientsWithData(search, pageable);
        } else {
            resultPage = patientRepository.findPatientsWithAllData(search, doctorIds, doctorIds.size(), pageable);
        }

        if (resultPage.isEmpty()) {
            return PatientsListResponse.builder()
                    .data(Collections.emptyList())
                    .count(resultPage.getTotalElements())
                    .build();
        }

        Map<Long, PatientData> patientDataMap = new LinkedHashMap<>();

        for (Object[] row : resultPage.getContent()) {
            Long patientId = ((Number) row[0]).longValue();
            String patientFirstName = (String) row[1];
            String patientLastName = (String) row[2];

            PatientData patientData = patientDataMap.computeIfAbsent(
                    patientId, id -> PatientData.builder()
                            .firstName(patientFirstName)
                            .lastName(patientLastName)
                            .visits(new ArrayList<>())
                            .build()
            );

            if (row[3] != null) {
                // Convert Timestamp to ZonedDateTime
                Timestamp startTimestamp = (Timestamp) row[4];
                Timestamp endTimestamp = (Timestamp) row[5];
                ZonedDateTime startDateTime = startTimestamp.toInstant().atZone(ZoneId.systemDefault());
                ZonedDateTime endDateTime = endTimestamp.toInstant().atZone(ZoneId.systemDefault());

                String doctorFirstName = (String) row[7];
                String doctorLastName = (String) row[8];
                String timezone = (String) row[9];
                Integer patientCount = row[10] != null ? ((Number) row[10]).intValue() : 0;

                VisitData visitData = VisitData.builder()
                        .startDateTime(startDateTime)
                        .endDateTime(endDateTime)
                        .doctorFirstName(doctorFirstName)
                        .doctorLastName(doctorLastName)
                        .timezone(timezone)
                        .patientCount(patientCount)
                        .build();

                patientData.addVisit(visitData);
            }
        }

        List<PatientResponse> patientResponses = patientDataMap.values().stream()
                .map(this::convertToPatientResponse)
                .collect(Collectors.toList());

        return PatientsListResponse.builder()
                .data(patientResponses)
                .count(resultPage.getTotalElements())
                .build();
    }

    private PatientResponse convertToPatientResponse(PatientData patientData) {
        List<VisitResponse> visitResponses = patientData.getVisits().stream()
                .map(visitData -> {
                    DoctorResponse doctorResponse = DoctorResponse.builder()
                            .firstName(visitData.getDoctorFirstName())
                            .lastName(visitData.getDoctorLastName())
                            .totalPatients(visitData.getPatientCount())
                            .build();

                    return VisitResponse.builder()
                            .start(formatDate(visitData.getStartDateTime(), visitData.getTimezone()))
                            .end(formatDate(visitData.getEndDateTime(), visitData.getTimezone()))
                            .doctor(doctorResponse)
                            .build();
                })
                .collect(Collectors.toList());

        return PatientResponse.builder()
                .firstName(patientData.getFirstName())
                .lastName(patientData.getLastName())
                .lastVisits(visitResponses)
                .build();
    }

    private String formatDate(ZonedDateTime dateTime, String timezone) {
        return dateTime.withZoneSameInstant(ZoneId.of(timezone))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}