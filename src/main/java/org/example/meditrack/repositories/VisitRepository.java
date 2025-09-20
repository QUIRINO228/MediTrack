package org.example.meditrack.repositories;

import org.example.meditrack.models.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;

@Repository
public interface VisitRepository extends JpaRepository<Visit, Long> {

    @Query("SELECT COUNT(v) FROM Visit v WHERE v.doctor.id = :doctorId " +
            "AND ((v.startDateTime BETWEEN :start AND :end) OR " +
            "(v.endDateTime BETWEEN :start AND :end) OR " +
            "(v.startDateTime <= :start AND v.endDateTime >= :end))")
    long countOverlappingVisits(@Param("doctorId") Long doctorId,
                                @Param("start") ZonedDateTime start,
                                @Param("end") ZonedDateTime end);
}