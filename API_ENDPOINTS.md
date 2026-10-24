# CivicSense REST API Documentation

The **CivicSense** backend provides a RESTful API for citizen reporting, automated spatial deduplication, intelligent municipal department routing, and field-worker triage with mandatory photo evidence verification.

---

## Table of Contents
1. [General Architecture & Base Setup](#1-general-architecture--base-setup)
2. [Data Enums & Models](#2-data-enums--models)
3. [Global Error Response Schema](#3-global-error-response-schema)
4. [State Machine & Deduplication Lifecycle](#4-state-machine--deduplication-lifecycle)
5. [Endpoints Quick Reference](#5-endpoints-quick-reference)
6. [Detailed Endpoint Documentation](#6-detailed-endpoint-documentation)
   - [6.1 Authentication & Users (`AuthController`)](#61-authentication--users-authcontroller)
     - [1. Register User (`POST /api/auth/register`)](#1-register-user-post-apiauthregister)
     - [2. Login User (`POST /api/auth/login`)](#2-login-user-post-apiauthlogin)
   - [6.2 Complaint Management (`ComplaintController`)](#62-complaint-management-complaintcontroller)
     - [3. Submit a New Complaint (`POST /api/complaints`)](#3-submit-a-new-complaint-post-apicomplaints)
     - [4. Get All Public Complaints (`GET /api/complaints`)](#4-get-all-public-complaints-get-apicomplaints)
     - [5. Get Complaint Details (`GET /api/complaints/{id}`)](#5-get-complaint-details-get-apicomplaintsid)
     - [6. Get My Submitted Complaints (`GET /api/complaints/me`)](#6-get-my-submitted-complaints-get-apicomplaintsme)
   - [6.3 Worker & Department Triage (`WorkerController`)](#63-worker--department-triage-workercontroller)
     - [7. Get Department Queue (`GET /api/worker/complaints`)](#7-get-department-queue-get-apiworkercomplaints)
     - [8. Update Complaint Status & Resolve (`PUT /api/complaints/{id}/status`)](#8-update-complaint-status--resolve-put-apicomplaintsidstatus)
   - [6.4 System Configuration (`DepartmentController`)](#64-system-configuration-departmentcontroller)
     - [9. Get List of Departments (`GET /api/departments`)](#9-get-list-of-departments-get-apidepartments)
7. [Cloudinary Storage Integration](#7-cloudinary-storage-integration)
8. [Database Connection Details](#8-database-connection-details)

---

## 1. General Architecture & Base Setup

- **Base URL:** `http://localhost:8080` (Configured via `SERVER_PORT` in `.env`)
- **Database:** Neon PostgreSQL with Flyway migrations & SSL enabled
- **File Storage:** Cloudinary (Direct streaming of multipart files to Cloudinary CDN)
- **Authentication Scheme:** Bearer JWT token sent in the `Authorization` header:
  ```http
  Authorization: Bearer <jwt_token>
  ```
- **CORS:** Enabled for all origin patterns (`*`) with credentials support across all standard HTTP methods.

---

## 2. Data Enums & Models

### Roles (`Role`)
| Value | Description |
|---|---|
| `CITIZEN` | General public; can report issues, browse public feeds, view their own complaints. |
| `WORKER` | Municipal field staff; can view assigned department queues and resolve issues with evidence. |
| `ADMIN` | Administrative supervisory role with unrestricted access across all endpoints. |

### Categories (`ComplaintCategory`)
| Value | Mapped Department (Default) | Typical Civic Issues |
|---|---|---|
| `WASTE` | **Sanitation** | Overflowing dumpsters, illegal dumping, garbage piles |
| `ROADS` | **Public Works** | Potholes, damaged pavement, open manholes, cave-ins |
| `STREETLIGHTS` | **Electrical** | Non-functional streetlights, exposed wiring, dark corridors |
| `OTHER` | **General Administration** | Damaged signage, stray animal issues, encroachments |

### Statuses (`ComplaintStatus`)
| Value | Description |
|---|---|
| `REPORTED` | Initial submission recorded by citizen. |
| `ASSIGNED` | Department routed; placed into the department worker triage queue. |
| `IN_PROGRESS` | Work crew or field worker has started remediation. |
| `RESOLVED` | Work completed; **requires mandatory `afterImage` photo evidence**. |
| `CLOSED` | Inspection verified or automatically finalized; terminal state. |

---

## 3. Global Error Response Schema

Whenever a request fails, the API responds with a consistent JSON error body:

```json
{
  "timestamp": "2026-10-24T15:30:00Z",
  "status": 400,
  "error": "Validation Failed",
  "message": "Request validation errors",
  "fieldErrors": [
    "Title must be between 5 and 200 characters",
    "Latitude must be >= -90"
  ]
}
```

### Common HTTP Status Codes
- `200 OK` — Request succeeded; response body contains requested entity.
- `201 Created` — Resource created successfully.
- `400 Bad Request` — Validation failed or missing required fields.
- `401 Unauthorized` — JWT token missing, expired, or invalid credentials.
- `403 Forbidden` — Authenticated user does not possess the required role.
- `404 Not Found` — Resource with the specified ID or endpoint does not exist.
- `409 Conflict` — Unique constraint violation (e.g. email already registered).
- `422 Unprocessable Entity` — Invalid state machine transition or missing resolution proof.
- `500 Internal Server Error` — Unhandled server or storage failure.

---

## 4. State Machine & Deduplication Lifecycle

### State Machine Transition Rules
```
[REPORTED] ────> [ASSIGNED] ────> [IN_PROGRESS] ────> [RESOLVED] ────> [CLOSED]
     │                                                     ▲
     └─────────────────────────────────────────────────────┘
                 (Resolution requires afterImage)
```
- Forward progression only; backwards transitions are blocked (`422 Unprocessable Entity`).
- Marking an issue `RESOLVED` **strictly requires** uploading an `afterImage` photo.

### Spatial Deduplication Engine
When a new complaint is filed:
1. The platform executes a native PostgreSQL **Haversine formula** query scanning all existing open complaints in the **same category**.
2. If an open complaint exists within **50 metres** (`DEDUPLICATION_RADIUS_METRES`), the new complaint is saved as a **child duplicate** of that parent (`parent_complaint_id`).
3. Field worker queues display only **parent complaints**, keeping the triage board uncluttered while retaining all citizen reports for audit and duplicate counts.

---

## 5. Endpoints Quick Reference

| # | HTTP Method | Endpoint Path | Role / Auth | Content-Type | Summary |
|---|---|---|---|---|---|
| 1 | `POST` | `/api/auth/register` | Public | `application/json` | Register citizen or worker |
| 2 | `POST` | `/api/auth/login` | Public | `application/json` | Authenticate and obtain JWT |
| 3 | `POST` | `/api/complaints` | `CITIZEN` | `multipart/form-data` | Submit new complaint + photo |
| 4 | `GET` | `/api/complaints` | Public / `CITIZEN` | None | Map / feed view with geo search |
| 5 | `GET` | `/api/complaints/{id}` | Public / Any | None | Complaint details + duplicates |
| 6 | `GET` | `/api/complaints/me` | `CITIZEN` | None | Citizen's submitted complaints |
| 7 | `GET` | `/api/worker/complaints` | `WORKER` | None | Worker department Kanban queue |
| 8 | `PUT` | `/api/complaints/{id}/status` | `WORKER` | `multipart/form-data` | Update status & resolve with photo |
| 9 | `GET` | `/api/departments` | Public | None | List of civic departments |

---

## 6. Detailed Endpoint Documentation

### 6.1 Authentication & Users (`AuthController`)

---

#### 1. Register User (`POST /api/auth/register`)
Creates a new user profile with a BCrypt-hashed password. Both Citizens and Workers register through this endpoint.

- **URL:** `/api/auth/register`
- **Method:** `POST`
- **Access:** Public (Unauthenticated)
- **Headers:** `Content-Type: application/json`

##### Request Body Parameters
| Field | Type | Required | Constraints | Description |
|---|---|---|---|---|
| `name` | String | Yes | 2 to 120 characters | User's full name |
| `email` | String | Yes | Valid email format | Unique email address used as username |
| `password` | String | Yes | Min 6 characters | Plain text password (hashed via BCrypt) |
| `role` | String (Enum) | Yes | `CITIZEN` or `WORKER` | Account role (default `CITIZEN`) |
| `departmentId` | Long | Optional | Valid Department ID | Required for `WORKER` to assign their department |

##### Sample Request (`CITIZEN`)
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Jane Citizen",
    "email": "jane@example.com",
    "password": "Password123!",
    "role": "CITIZEN"
  }'
```

##### Sample Request (`WORKER`)
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alex Worker",
    "email": "alex.worker@city.gov",
    "password": "WorkerPassword123!",
    "role": "WORKER",
    "departmentId": 2
  }'
```

##### Success Response (`201 Created`)
```json
{
  "message": "User registered successfully",
  "userId": 1
}
```

##### Error Response: Email Already Exists (`409 Conflict`)
```json
{
  "timestamp": "2026-10-24T15:31:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "User with email [jane@example.com] already exists"
}
```

---

#### 2. Login User (`POST /api/auth/login`)
Authenticates credentials and returns a signed HS256 JWT valid for 24 hours.

- **URL:** `/api/auth/login`
- **Method:** `POST`
- **Access:** Public (Unauthenticated)
- **Headers:** `Content-Type: application/json`

##### Request Body Parameters
| Field | Type | Required | Description |
|---|---|---|---|
| `email` | String | Yes | User's registered email |
| `password` | String | Yes | User's password |

##### Sample Request
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "jane@example.com",
    "password": "Password123!"
  }'
```

##### Success Response (`200 OK`)
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlIjoiQ0lUSVpFTiIsInN1YiI6ImphbmVAZXhhbXBsZS5jb20iLCJpYXQiOjE3Mjk3ODMwMDAsImV4cCI6MTcyOTg2OTQwMH0.abcdef1234567890",
  "role": "CITIZEN",
  "email": "jane@example.com"
}
```

##### Error Response: Bad Credentials (`401 Unauthorized`)
```json
{
  "timestamp": "2026-10-24T15:32:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid email or password"
}
```

---

### 6.2 Complaint Management (`ComplaintController`)

---

#### 3. Submit a New Complaint (`POST /api/complaints`)
Allows a citizen to report a civic issue with description, GPS coordinates, and a mandatory "Before" photo. The backend streams the photo to Cloudinary, runs spatial deduplication, routes to the matching department, and saves the issue.

- **URL:** `/api/complaints`
- **Method:** `POST`
- **Access:** Authenticated (`CITIZEN`, `ADMIN`)
- **Headers:**
  - `Authorization: Bearer <token>`
  - `Content-Type: multipart/form-data`

##### Multipart Form Parameters
| Field Name | Type | Required | Validation Constraints | Description |
|---|---|---|---|---|
| `title` | Text | Yes | 5 to 200 characters | Concise issue title |
| `description` | Text | Yes | Minimum 10 characters | Detailed description of the problem |
| `category` | Text | Yes | `WASTE`, `ROADS`, `STREETLIGHTS`, `OTHER` | Category driving department routing |
| `latitude` | Decimal | Yes | -90.0 to 90.0 | GPS Latitude |
| `longitude` | Decimal | Yes | -180.0 to 180.0 | GPS Longitude |
| `beforeImage` | File | Yes | image/* (max 10 MB) | Photo evidence of the civic problem |

##### Sample Request (cURL)
```bash
curl -X POST http://localhost:8080/api/complaints \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1Ni..." \
  -F "title=Large pothole near intersection" \
  -F "description=Severe pothole on 5th Avenue causing vehicle damage and traffic bottleneck." \
  -F "category=ROADS" \
  -F "latitude=28.613939" \
  -F "longitude=77.209021" \
  -F "beforeImage=@/path/to/pothole.jpg;type=image/jpeg"
```

##### Success Response: Original Issue Created (`201 Created`)
```json
{
  "id": 1,
  "status": "ASSIGNED",
  "departmentId": 2,
  "parentComplaintId": null,
  "message": "Complaint recorded."
}
```

##### Success Response: Duplicate Issue Detected & Grouped (`201 Created`)
```json
{
  "id": 14,
  "status": "REPORTED",
  "departmentId": 2,
  "parentComplaintId": 1,
  "message": "Complaint recorded. Grouped with existing issue #1"
}
```

##### Error Response: Validation Failed (`400 Bad Request`)
```json
{
  "timestamp": "2026-10-24T15:33:00Z",
  "status": 400,
  "error": "Validation Failed",
  "message": "Request validation errors",
  "fieldErrors": [
    "beforeImage photo is required",
    "Description must be at least 10 characters"
  ]
}
```

---

#### 4. Get All Public Complaints (`GET /api/complaints`)
Returns a paginated list of top-level complaints for public map and feed views. Supports optional filtering by category, status, and proximity radius search via Haversine calculation.

- **URL:** `/api/complaints`
- **Method:** `GET`
- **Access:** Public (or `CITIZEN`)
- **Headers:** Optional `Authorization: Bearer <token>`

##### Query Parameters
| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| `category` | String | No | None | Filter by `WASTE`, `ROADS`, `STREETLIGHTS`, `OTHER` |
| `status` | String | No | None | Filter by status (`OPEN`, `ASSIGNED`, `IN_PROGRESS`, etc.) |
| `lat` | Double | No | None | GPS latitude for radius proximity search |
| `lng` | Double | No | None | GPS longitude for radius proximity search |
| `radius` | Double | No | None | Search radius in metres (e.g. `5000` = 5 km) |
| `page` | Integer | No | `0` | Page number (0-indexed) |
| `size` | Integer | No | `20` | Items per page (max 100) |
| `sort` | String | No | `createdAt,desc` | Sort field and direction (`field,asc` or `field,desc`) |

##### Sample Request (Proximity Search within 5km)
```bash
curl -X GET "http://localhost:8080/api/complaints?category=ROADS&status=OPEN&lat=28.6139&lng=77.2090&radius=5000&page=0&size=10"
```

##### Success Response (`200 OK`)
```json
{
  "content": [
    {
      "id": 1,
      "title": "Large pothole near intersection",
      "category": "ROADS",
      "status": "IN_PROGRESS",
      "latitude": 28.613939,
      "longitude": 77.209021,
      "lat": 28.613939,
      "lng": 77.209021,
      "beforeImageUrl": "https://res.cloudinary.com/demo/image/upload/v1729783000/civicsense/before/pothole_1.jpg",
      "departmentName": "Public Works",
      "duplicateCount": 3,
      "createdAt": "2026-10-24T10:15:30Z"
    },
    {
      "id": 8,
      "title": "Broken asphalt along bicycle lane",
      "category": "ROADS",
      "status": "ASSIGNED",
      "latitude": 28.615210,
      "longitude": 77.210450,
      "lat": 28.615210,
      "lng": 77.210450,
      "beforeImageUrl": "https://res.cloudinary.com/demo/image/upload/v1729783100/civicsense/before/road_crack.jpg",
      "departmentName": "Public Works",
      "duplicateCount": 0,
      "createdAt": "2026-10-24T11:45:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 2,
  "totalPages": 1,
  "last": true,
  "size": 10,
  "number": 0,
  "numberOfElements": 2,
  "first": true,
  "empty": false
}
```

---

#### 5. Get Complaint Details (`GET /api/complaints/{id}`)
Fetches complete information for a specific complaint, including full reporter data, department assignment, and a nested list of all grouped child duplicates.

- **URL:** `/api/complaints/{id}`
- **Method:** `GET`
- **Access:** Public (or `CITIZEN` / `WORKER`)
- **Path Variable:** `id` (Long, complaint ID)

##### Sample Request
```bash
curl -X GET http://localhost:8080/api/complaints/1
```

##### Success Response (`200 OK`)
```json
{
  "id": 1,
  "title": "Large pothole near intersection",
  "description": "Severe pothole on 5th Avenue causing vehicle damage and traffic bottleneck.",
  "category": "ROADS",
  "status": "IN_PROGRESS",
  "latitude": 28.613939,
  "longitude": 77.209021,
  "lat": 28.613939,
  "lng": 77.209021,
  "beforeImageUrl": "https://res.cloudinary.com/demo/image/upload/v1729783000/civicsense/before/pothole_1.jpg",
  "afterImageUrl": null,
  "workerNotes": "Work crew dispatched with cold-mix asphalt patcher.",
  "department": {
    "id": 2,
    "name": "Public Works"
  },
  "departmentName": "Public Works",
  "reporterName": "Jane Citizen",
  "reporterEmail": "jane@example.com",
  "parentComplaintId": null,
  "duplicates": [
    {
      "id": 14,
      "title": "Bad road crater on 5th Ave",
      "category": "ROADS",
      "status": "REPORTED",
      "latitude": 28.613950,
      "longitude": 77.209030,
      "lat": 28.613950,
      "lng": 77.209030,
      "beforeImageUrl": "https://res.cloudinary.com/demo/image/upload/v1729784000/civicsense/before/dup_photo_1.jpg",
      "departmentName": "Public Works",
      "duplicateCount": 0,
      "createdAt": "2026-10-24T11:00:00Z"
    }
  ],
  "createdAt": "2026-10-24T10:15:30Z",
  "updatedAt": "2026-10-24T12:00:00Z"
}
```

##### Error Response: Complaint Not Found (`404 Not Found`)
```json
{
  "timestamp": "2026-10-24T15:34:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Complaint with id [999] was not found"
}
```

---

#### 6. Get My Submitted Complaints (`GET /api/complaints/me`)
Returns complaints submitted by the currently logged-in citizen, extracted automatically from the Bearer JWT token.

- **URL:** `/api/complaints/me`
- **Method:** `GET`
- **Access:** Authenticated (`CITIZEN`, `ADMIN`)
- **Headers:** `Authorization: Bearer <token>`
- **Query Parameters:**
  - `page` (Optional int, default `0`)
  - `size` (Optional int, default `20`)

##### Sample Request
```bash
curl -X GET "http://localhost:8080/api/complaints/me?page=0&size=10" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1Ni..."
```

##### Success Response (`200 OK`)
```json
{
  "content": [
    {
      "id": 1,
      "title": "Large pothole near intersection",
      "category": "ROADS",
      "status": "IN_PROGRESS",
      "latitude": 28.613939,
      "longitude": 77.209021,
      "lat": 28.613939,
      "lng": 77.209021,
      "beforeImageUrl": "https://res.cloudinary.com/demo/image/upload/v1729783000/civicsense/before/pothole_1.jpg",
      "departmentName": "Public Works",
      "duplicateCount": 1,
      "createdAt": "2026-10-24T10:15:30Z"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 10,
  "number": 0,
  "first": true,
  "last": true,
  "empty": false
}
```

---

### 6.3 Worker & Department Triage (`WorkerController`)

---

#### 7. Get Department Queue (`GET /api/worker/complaints`)
Provides the department-level triage queue (Kanban view). The worker's assigned department ID is extracted from their profile in the database, automatically filtering the complaints.

- **URL:** `/api/worker/complaints`
- **Method:** `GET`
- **Access:** Authenticated (`WORKER`, `ADMIN`)
- **Headers:** `Authorization: Bearer <token>`
- **Query Parameters:**
  - `status` (Optional enum: `ASSIGNED`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`)
  - `page` (Optional int, default `0`)
  - `size` (Optional int, default `20`)

##### Sample Request
```bash
curl -X GET "http://localhost:8080/api/worker/complaints?status=ASSIGNED&page=0&size=10" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1Ni..."
```

##### Success Response (`200 OK`)
```json
{
  "content": [
    {
      "id": 8,
      "title": "Broken asphalt along bicycle lane",
      "category": "ROADS",
      "status": "ASSIGNED",
      "latitude": 28.615210,
      "longitude": 77.210450,
      "lat": 28.615210,
      "lng": 77.210450,
      "beforeImageUrl": "https://res.cloudinary.com/demo/image/upload/v1729783100/civicsense/before/road_crack.jpg",
      "departmentName": "Public Works",
      "duplicateCount": 0,
      "createdAt": "2026-10-24T11:45:00Z"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 10,
  "number": 0,
  "first": true,
  "last": true,
  "empty": false
}
```

##### Error Response: Worker Has No Department (`404 Not Found`)
```json
{
  "timestamp": "2026-10-24T15:35:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Worker is not assigned to any department."
}
```

---

#### 8. Update Complaint Status & Resolve (`PUT /api/complaints/{id}/status`)
Updates the lifecycle state of a complaint. When transitioning to `RESOLVED`, the field worker **must attach** an `afterImage` photograph showing the completed resolution evidence. The photo is uploaded directly to Cloudinary under the `civicsense/after/` directory.

- **URL:** `/api/complaints/{id}/status` *(Also accessible via `/api/worker/complaints/{id}/status`)*
- **Method:** `PUT`
- **Access:** Authenticated (`WORKER`, `ADMIN`)
- **Headers:**
  - `Authorization: Bearer <token>`
  - `Content-Type: multipart/form-data`
- **Path Variable:** `id` (Long, complaint ID)

##### Multipart Form Parameters
| Field Name | Type | Required | Description |
|---|---|---|---|
| `status` | Text (Enum) | Yes | Target status: `IN_PROGRESS`, `RESOLVED`, or `CLOSED` |
| `afterImage` | File | **Yes (if `RESOLVED`)** / Optional (otherwise) | Photo evidence of completed repair work |
| `workerNotes` | Text | Optional | Remarks, team name, materials used, etc. |

##### Sample Request: Move to `IN_PROGRESS` (No photo required)
```bash
curl -X PUT http://localhost:8080/api/complaints/1/status \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1Ni..." \
  -F "status=IN_PROGRESS" \
  -F "workerNotes=Crew Alpha assigned. Arriving at site."
```

##### Sample Request: Mark `RESOLVED` (With mandatory After-Photo)
```bash
curl -X PUT http://localhost:8080/api/complaints/1/status \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1Ni..." \
  -F "status=RESOLVED" \
  -F "workerNotes=Pothole filled with hot asphalt and steam-rolled flush with road surface." \
  -F "afterImage=@/path/to/repaired_road.jpg;type=image/jpeg"
```

##### Success Response (`200 OK`)
```json
{
  "id": 1,
  "status": "RESOLVED",
  "afterImageUrl": "https://res.cloudinary.com/demo/image/upload/v1729785500/civicsense/after/repaired_road.jpg",
  "message": "Status updated successfully"
}
```

##### Error Response: Missing Evidence Photo on Resolve (`422 Unprocessable Entity`)
```json
{
  "timestamp": "2026-10-24T15:36:00Z",
  "status": 422,
  "error": "Invalid State Transition",
  "message": "Cannot mark complaint RESOLVED without an after-evidence photo."
}
```

##### Error Response: Invalid Lifecycle State Jump (`422 Unprocessable Entity`)
```json
{
  "timestamp": "2026-10-24T15:37:00Z",
  "status": 422,
  "error": "Invalid State Transition",
  "message": "Invalid status transition from [REPORTED] to [CLOSED]"
}
```

---

### 6.4 System Configuration (`DepartmentController`)

---

#### 9. Get List of Departments (`GET /api/departments`)
Returns all municipal departments. Used by frontend client apps to populate category dropdowns, filter chips, and worker registration department selectors.

- **URL:** `/api/departments`
- **Method:** `GET`
- **Access:** Public (Unauthenticated)

##### Sample Request
```bash
curl -X GET http://localhost:8080/api/departments
```

##### Success Response (`200 OK`)
```json
[
  {
    "id": 1,
    "name": "Sanitation"
  },
  {
    "id": 2,
    "name": "Public Works"
  },
  {
    "id": 3,
    "name": "Electrical"
  },
  {
    "id": 4,
    "name": "General Administration"
  }
]
```

---

## 7. Cloudinary Storage Integration

CivicSense uses the **Cloudinary Java SDK** (`com.cloudinary:cloudinary-http5:2.5.0`) to handle media assets:

```
[Citizen / Client App]
      │
      │ 1. POST /api/complaints (multipart/form-data with beforeImage)
      ▼
[Spring Boot Backend]
      │
      │ 2. Streams bytes via Cloudinary SDK (folder: civicsense/before)
      ▼
[Cloudinary CDN Cloud]
      │
      │ 3. Generates optimized HTTPS delivery URL
      ▼
[Spring Boot Backend]
      │
      │ 4. Stores HTTPS URL in PostgreSQL (complaints.before_image_url)
      ▼
[PostgreSQL Database]
```

### Folder Structure
- `civicsense/before/` — Photos uploaded by citizens when reporting issues.
- `civicsense/after/` — Resolution evidence photos uploaded by workers upon fixing issues.

---

## 8. Database Connection Details

The platform is connected to **Neon PostgreSQL** with Flyway managing database migrations:

```properties
DB_HOST=ep-bold-hall-b5wcihyc-pooler.c-7.us-east-2.aws.neon.tech
DB_PORT=5432
DB_NAME=neondb
DB_USERNAME=neondb_owner
DB_URL=jdbc:postgresql://ep-bold-hall-b5wcihyc-pooler.c-7.us-east-2.aws.neon.tech:5432/neondb?sslmode=require
```

### Flyway Migrations Applied
1. `V1__init_schema.sql` — Initial schema: `users`, `departments`, and `complaints` tables with spatial and category indexes.
2. `V2__seed_departments.sql` — Seeds default departments (`Sanitation`, `Public Works`, `Electrical`, `General Administration`).
3. `V3__worker_department_and_notes.sql` — Adds `department_id` foreign key to `users` and `worker_notes` column to `complaints`.
