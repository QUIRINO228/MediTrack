package org.example.meditrack.repositories;

import org.example.meditrack.models.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    @Query("SELECT DISTINCT p FROM Patient p " +
            "LEFT JOIN p.visits v " +
            "LEFT JOIN v.doctor d " +
            "WHERE (:search IS NULL OR LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:doctorIds IS NULL OR d.id IN :doctorIds)")
    Page<Patient> findPatientsWithFilters(
            @Param("search") String search,
            @Param("doctorIds") List<Long> doctorIds,
            Pageable pageable);

    @Query("SELECT COUNT(DISTINCT p.id) FROM Patient p " +
            "LEFT JOIN p.visits v " +
            "LEFT JOIN v.doctor d " +
            "WHERE (:search IS NULL OR LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:doctorIds IS NULL OR d.id IN :doctorIds)")
    long countPatientsWithFilters(
            @Param("search") String search,
            @Param("doctorIds") List<Long> doctorIds);
}

