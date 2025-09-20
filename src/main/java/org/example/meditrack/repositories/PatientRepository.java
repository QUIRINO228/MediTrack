package org.example.meditrack.repositories;

import org.example.meditrack.models.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    @Query(value = """
        WITH base_patients AS (
            SELECT DISTINCT p.id as patient_id, 
                   p.first_name as patient_first_name, 
                   p.last_name as patient_last_name
            FROM patients p
            LEFT JOIN (
                SELECT DISTINCT v1.patient_id, v1.doctor_id
                FROM visits v1
            ) v ON p.id = v.patient_id
            LEFT JOIN doctors d ON v.doctor_id = d.id
            WHERE (:search IS NULL OR LOWER(CONCAT(p.first_name, ' ', p.last_name)) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (COALESCE(:doctorIdsSize, 0) = 0 OR d.id IN :doctorIds OR v.doctor_id IS NULL)
        ),
        total_count AS (
            SELECT COUNT(*) as total FROM base_patients
        ),
        paginated_patients AS (
            SELECT *, ROW_NUMBER() OVER (ORDER BY patient_id) as row_num
            FROM base_patients
        ),
        filtered_patients AS (
            SELECT pp.patient_id, pp.patient_first_name, pp.patient_last_name
            FROM paginated_patients pp
            WHERE pp.row_num BETWEEN (:offset + 1) AND (:offset + :pageSize)
        )
        SELECT fp.patient_id, fp.patient_first_name, fp.patient_last_name,
               v.id as visit_id, 
               v.start_date_time, 
               v.end_date_time,
               d.id as doctor_id, 
               d.first_name as doctor_first_name, 
               d.last_name as doctor_last_name, 
               d.timezone,
               COALESCE(doc_stats.patient_count, 0) as patient_count,
               tc.total
        FROM filtered_patients fp
        CROSS JOIN total_count tc
        LEFT JOIN (
            SELECT v1.patient_id, v1.doctor_id, v1.id, v1.start_date_time, v1.end_date_time,
                   ROW_NUMBER() OVER (PARTITION BY v1.patient_id, v1.doctor_id ORDER BY v1.start_date_time DESC, v1.id DESC) as rn
            FROM visits v1
        ) v ON fp.patient_id = v.patient_id AND v.rn = 1
        LEFT JOIN doctors d ON v.doctor_id = d.id
        LEFT JOIN (
            SELECT doctor_id, COUNT(DISTINCT patient_id) as patient_count
            FROM visits
            GROUP BY doctor_id
        ) doc_stats ON d.id = doc_stats.doctor_id
        ORDER BY fp.patient_id
        """, nativeQuery = true)
    List<Object[]> findPatientsWithAllDataOptimized(
            @Param("search") String search,
            @Param("doctorIds") List<Long> doctorIds,
            @Param("doctorIdsSize") Integer doctorIdsSize,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);

    @Query(value = """
        WITH base_patients AS (
            SELECT DISTINCT p.id as patient_id, 
                   p.first_name as patient_first_name, 
                   p.last_name as patient_last_name
            FROM patients p
            WHERE (:search IS NULL OR LOWER(CONCAT(p.first_name, ' ', p.last_name)) LIKE LOWER(CONCAT('%', :search, '%')))
        ),
        total_count AS (
            SELECT COUNT(*) as total FROM base_patients
        ),
        paginated_patients AS (
            SELECT *, ROW_NUMBER() OVER (ORDER BY patient_id) as row_num
            FROM base_patients
        ),
        filtered_patients AS (
            SELECT pp.patient_id, pp.patient_first_name, pp.patient_last_name
            FROM paginated_patients pp
            WHERE pp.row_num BETWEEN (:offset + 1) AND (:offset + :pageSize)
        )
        SELECT fp.patient_id, fp.patient_first_name, fp.patient_last_name,
               v.id as visit_id, 
               v.start_date_time, 
               v.end_date_time,
               d.id as doctor_id, 
               d.first_name as doctor_first_name, 
               d.last_name as doctor_last_name, 
               d.timezone,
               COALESCE(doc_stats.patient_count, 0) as patient_count,
               tc.total
        FROM filtered_patients fp
        CROSS JOIN total_count tc
        LEFT JOIN (
            SELECT v1.patient_id, v1.doctor_id, v1.id, v1.start_date_time, v1.end_date_time,
                   ROW_NUMBER() OVER (PARTITION BY v1.patient_id, v1.doctor_id ORDER BY v1.start_date_time DESC, v1.id DESC) as rn
            FROM visits v1
        ) v ON fp.patient_id = v.patient_id AND v.rn = 1
        LEFT JOIN doctors d ON v.doctor_id = d.id
        LEFT JOIN (
            SELECT doctor_id, COUNT(DISTINCT patient_id) as patient_count
            FROM visits
            GROUP BY doctor_id
        ) doc_stats ON d.id = doc_stats.doctor_id
        ORDER BY fp.patient_id
        """, nativeQuery = true)
    List<Object[]> findAllPatientsWithDataOptimized(
            @Param("search") String search,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);
}