package org.example.meditrack.repositories;

import org.example.meditrack.models.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface VisitRepository extends JpaRepository<Visit, Long> {

    @Query("SELECT COUNT(v) FROM Visit v WHERE v.doctor.id = :doctorId " +
            "AND ((v.startDateTime BETWEEN :start AND :end) OR " +
            "(v.endDateTime BETWEEN :start AND :end) OR " +
            "(v.startDateTime <= :start AND v.endDateTime >= :end))")
    long countOverlappingVisits(@Param("doctorId") Long doctorId,
                                @Param("start") ZonedDateTime start,
                                @Param("end") ZonedDateTime end);

    @Query("SELECT v FROM Visit v " +
            "WHERE v.patient.id IN :patientIds " +
            "AND v.id IN (" +
            "  SELECT MAX(v2.id) FROM Visit v2 " +
            "  WHERE v2.patient.id = v.patient.id AND v2.doctor.id = v.doctor.id" +
            ")")
    List<Visit> findLatestVisitsForPatients(@Param("patientIds") List<Long> patientIds);

    @Query("SELECT v.doctor.id, COUNT(DISTINCT v.patient.id) " +
            "FROM Visit v " +
            "WHERE v.doctor.id IN :doctorIds " +
            "GROUP BY v.doctor.id")
    List<Object[]> countPatientsByDoctors(@Param("doctorIds") List<Long> doctorIds);
}
