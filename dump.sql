DROP TABLE IF EXISTS visits;
DROP TABLE IF EXISTS patients;
DROP TABLE IF EXISTS doctors;

CREATE TABLE patients (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          first_name VARCHAR(100) NOT NULL,
                          last_name VARCHAR(100) NOT NULL
);

CREATE TABLE doctors (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         first_name VARCHAR(100) NOT NULL,
                         last_name VARCHAR(100) NOT NULL,
                         timezone VARCHAR(50) NOT NULL
);

CREATE TABLE visits (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        start_date_time DATETIME NOT NULL,
                        end_date_time DATETIME NOT NULL,
                        patient_id BIGINT NOT NULL,
                        doctor_id BIGINT NOT NULL,
                        CONSTRAINT fk_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
                        CONSTRAINT fk_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id),
                        UNIQUE (doctor_id, start_date_time, end_date_time)
);


INSERT INTO patients (first_name, last_name) VALUES
                                                 ('John', 'Doe'),
                                                 ('Jane', 'Smith'),
                                                 ('Michael', 'Brown'),
                                                 ('Emily', 'Clark'),
                                                 ('David', 'Johnson');

INSERT INTO doctors (first_name, last_name, timezone) VALUES
                                                          ('Alice', 'Miller', 'America/New_York'),
                                                          ('Robert', 'Wilson', 'Europe/Kiev'),
                                                          ('Sophia', 'Taylor', 'Europe/London');

INSERT INTO visits (start_date_time, end_date_time, patient_id, doctor_id) VALUES
                                                                               ('2025-09-15T10:00:00-04:00', '2025-09-15T10:30:00-04:00', 1, 1),
                                                                               ('2025-09-15T11:00:00-04:00', '2025-09-15T11:30:00-04:00', 2, 1),
                                                                               ('2025-09-15T12:00:00-04:00', '2025-09-15T12:45:00-04:00', 3, 2),
                                                                               ('2025-09-16T09:00:00-04:00', '2025-09-16T09:30:00-04:00', 1, 2),
                                                                               ('2025-09-16T15:00:00-04:00', '2025-09-16T15:30:00-04:00', 4, 3),
                                                                               ('2025-09-16T16:00:00-04:00', '2025-09-16T16:30:00-04:00', 5, 3),
                                                                               ('2025-09-17T10:00:00-04:00', '2025-09-17T10:30:00-04:00', 2, 2),
                                                                               ('2025-09-17T11:00:00-04:00', '2025-09-17T11:30:00-04:00', 3, 3);