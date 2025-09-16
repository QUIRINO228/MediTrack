# MediTrack

## Project Overview

MediTrack is a healthcare solution originating from the USA, serving as an essential resource for thousands seeking to address their health concerns. Since its inception four years ago, the project has demonstrated consistent growth, continually expanding its reach and impact in the healthcare sector. Its innovative approach and commitment to improving health outcomes have made it a trusted ally for both patients and healthcare providers, with promising prospects for further development.

## Requirements

- Experience in Java and Spring Framework development
- In-depth knowledge of MySQL and query optimization skills
- Experience writing automated tests using JUnit

## Technical Stack

- **Java**: Core programming language
- **Spring Framework**: For building robust REST APIs and managing dependencies
- **MySQL 8**: Database for storing patients, doctors, and visit records
- **JUnit**: For unit and integration testing
- **Docker**: For containerized MySQL and phpMyAdmin setup

## Project Description

MediTrack is a system designed to track patient visits to doctors. It provides two main API endpoints:

1. **POST /api/visits**: Creates a new visit record.
2. **GET /api/patients**: Retrieves a paginated list of patients with their latest visits.

### Entities

- **Patient**:
    - `firstName`: String (required)
    - `lastName`: String (required)
- **Doctor**:
    - `firstName`: String (required)
    - `lastName`: String (required)
    - `timezone`: String (required, e.g., "America/New_York")
- **Visit**:
    - `startDateTime`: DateTime (required)
    - `endDateTime`: DateTime (required)
    - `patient`: Reference to Patient
    - `doctor`: Reference to Doctor

### API Endpoints

#### POST /api/visits

Creates a new visit with the following payload:

```json
{
    "start": "2025-09-15T10:00:00-04:00",
    "end": "2025-09-15T10:30:00-04:00",
    "patientId": 1,
    "doctorId": 1
}
```

- **Validation**: Ensures no overlapping visits for the same doctor at the same time. The `start` and `end` times must be in the doctor's timezone.
- **Response**: Returns HTTP 200 on success or HTTP 400 with an error message for invalid requests (e.g., overlapping visits, missing doctor/patient, invalid time range).

#### GET /api/patients

Retrieves a paginated list of patients with their latest visits to each doctor. Query parameters:

- `page`: Optional, page number (default: 0)
- `size`: Optional, number of patients per page (default: 20)
- `search`: Optional, search by patient name
- `doctorIds`: Optional, comma-separated list of doctor IDs to filter visits

**Response**:

```json
{
    "data": [
        {
            "firstName": "John",
            "lastName": "Doe",
            "lastVisits": [
                {
                    "start": "2025-09-15T10:00:00-04:00",
                    "end": "2025-09-15T10:30:00-04:00",
                    "doctor": {
                        "firstName": "Alice",
                        "lastName": "Miller",
                        "totalPatients": 3
                    }
                }
            ]
        }
    ],
    "count": 5
}
```

- **Behavior**: Returns the latest visit per doctor for each patient. The `start` and `end` times are in the doctor's timezone. The `totalPatients` field indicates the number of unique patients who have visited the doctor. The `count` field reflects the total number of patients matching the query.

### Optimization

The system is designed for scalability, handling hundreds of thousands of patients, doctors, and visits. Key optimizations include:

- **Database**:
    - Indexes on frequently queried fields (e.g., `doctor_id`, `startDateTime`, `patient_id`) to improve query performance.
    - Efficient JPQL queries in `PatientRepository` and `VisitRepository` to minimize database calls.
    - Batching and ordering of inserts/updates in Hibernate configuration (`application.yml`).
- **Application**:
    - Lazy loading for relationships to reduce memory usage.
    - Single database query for fetching patients with filters, followed by optimized queries for latest visits and doctor patient counts.
    - Timezone conversions handled efficiently using Java `ZonedDateTime`.

### Setup Instructions

1. **Prerequisites**:

    - Java 17+
    - Maven
    - Docker (for MySQL and phpMyAdmin)
    - MySQL Workbench or similar (optional, for manual database inspection)

2. **Clone the Repository**:

   ```bash
   git clone https://github.com/QUIRINO228/MediTrack
   cd meditrack
   ```

3. **Database Setup**:

    - Run the Docker Compose file to start MySQL and phpMyAdmin:

      ```bash
      docker-compose up -d
      ```

    - The database will be initialized with the schema and test data from `dump.sql`.

    - Access phpMyAdmin at `http://localhost:8080` (login with `root`/`SuperSecretRoot123!` or `app_user`/`AppUserPass456@`).

4. **Run the Application**:

    - Build and run the Spring Boot application:

      ```bash
      mvn clean install
      mvn spring-boot:run
      ```

    - The application runs on `http://localhost:8081`.

5. **Test the Endpoints**:

    - Use tools like Postman or curl to test the API.

    - Example POST request:

      ```bash
      curl -X POST http://localhost:8081/api/visits \
      -H "Content-Type: application/json" \
      -d '{"start":"2025-09-18T10:00:00-04:00","end":"2025-09-18T10:30:00-04:00","patientId":1,"doctorId":1}'
      ```

    - Example GET request:

      ```bash
      curl "http://localhost:8081/api/patients?page=0&size=20&search=John&doctorIds=1,2"
      ```

### Test Data

The `dump.sql` file includes test data with:

- **5 Patients**: John Doe, Jane Smith, Michael Brown, Emily Clark, David Johnson
- **3 Doctors**: Alice Miller (America/New_York), Robert Wilson (Europe/Kiev), Sophia Taylor (Europe/London)
- **8 Visits**: Configured to demonstrate cross-relations:
    - Doctor 1 (Alice) has visits from patients 1, 2
    - Doctor 2 (Robert) has visits from patients 1, 2, 3
    - Doctor 3 (Sophia) has visits from patients 3, 4, 5
    - Patient 1 (John) visits doctors 1, 2
    - Patient 2 (Jane) visits doctors 1, 2
    - Patient 3 (Michael) visits doctors 2, 3
    - Patient 4 (Emily) visits doctor 3
    - Patient 5 (David) visits doctor 3

### Testing

The project includes comprehensive unit tests in `VisitServiceTest.java` using JUnit and Mockito. Tests cover:

- Successful visit creation
- Overlapping visit validation
- Patient list retrieval with/without filters
- Timezone handling
- Error scenarios (e.g., missing doctor/patient, invalid time ranges)

Run tests with:

```bash
mvn test
```

### Notes

1. Ensure MySQL is running before starting the application.
2. The `application.yml` configures Hibernate to use the MySQL database and optimizes batch processing.
3. The `GlobalExceptionHandler` ensures consistent error responses for `BusinessException`, validation errors, and unexpected errors.

- For production, update `MYSQL_ROOT_PASSWORD` and `MYSQL_PASSWORD` to secure values.