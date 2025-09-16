package org.example.meditrack.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.meditrack.dtos.CreateVisitRequest;
import org.example.meditrack.dtos.PatientsListResponse;
import org.example.meditrack.services.VisitService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VisitController {

    private final VisitService visitService;

    @PostMapping("/visits")
    public ResponseEntity<Void> createVisit(@Valid @RequestBody CreateVisitRequest request) {
        visitService.createVisit(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/patients")
    public ResponseEntity<PatientsListResponse> getPatients(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String doctorIds) {

        List<Long> doctorIdList = null;
        if (doctorIds != null && !doctorIds.trim().isEmpty()) {
            doctorIdList = Arrays.stream(doctorIds.split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        }

        PatientsListResponse response = visitService.getPatients(page, size, search, doctorIdList);
        return ResponseEntity.ok(response);
    }
}