# CivicSense API Documentation

The **CivicSense** backend provides a RESTful API for reporting, routing, deduplicating, and tracking civic complaints across simulated municipal departments.

---

## 1. General Information

- **Base URL (Local):** `http://localhost:8080`
- **Default Port:** `8080` (configured via `SERVER_PORT` in `.env`)
- **Authentication:** Bearer JWT in HTTP Header (`Authorization: Bearer <token>`)
- **Supported Enums:**
  - **Roles:** `CITIZEN`, `WORKER`, `ADMIN`
  - **Categories:** `WASTE`, `ROADS`, `STREETLIGHTS`, `OTHER`
  - **Statuses:** `REPORTED`, `ASSIGNED`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`

---

## 2. Endpoints Summary Table

| Method | Endpoint | Access / Role | Content Type | Description |
|---|---|---|---|---|
| `POST` | `/api/auth/register` | Public | `application/json` | Register a new user |
| `POST` | `/api/auth/login` | Public | `application/json` | Authenticate and obtain JWT |
| `POST` | `/api/complaints` | `CITIZEN`, `ADMIN` | `multipart/form-data` | Report an issue (runs deduplication) |
| `GET` | `/api/complaints` | Authenticated | N/A | Paginated, filtered list of complaints |
| `GET` | `/api/complaints/{id}` | Authenticated | N/A | Get complaint details & child duplicates |
| `PUT` | `/api/complaints/{id}/status` | `WORKER`, `ADMIN` | `multipart/form-data` | Update status + optional/mandatory after-image |
| `GET` | `/uploads/{folder}/{filename}` | Public | Any | View uploaded image (when local storage is active) |

---

## 3. Authentication Endpoints

### 3.1 Register User
Register a new citizen, worker, or administrator account.

- **URL:** `/api/auth/register`
- **Method:** `POST`
- **Auth Required:** No
- **Headers:** `Content-Type: application/json`

#### Request Body
| Field | Type | Constraints | Description |
|---|---|---|---|
| `name` | `string` | Required, 2-120 chars | Full name of the user |
| `email` | `string` | Required, valid email | Unique login email |
| `password` | `string` | Required, min 8 chars | Plain-text password (hashed with BCrypt) |
| `role` | `string` | Optional (default: `CITIZEN`) | `CITIZEN`, `WORKER`, or `ADMIN` |

```json
{
  "name": "Jane Citizen",
  "email": "jane.citizen@example.com",
  "password": "Password123!",
  "role": "CITIZEN"
}
```

#### Response: `201 Created`
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqYW5lLmNpdGl6ZW5AZXhhbXBsZS5jb20iLCJyb2xlIjoiQ0lUSVpFTiIsImlhdCI6MTY5ODExMjAwMCwiZXhwIjoxNjk4MTk4NDAwfQ.example_signature",
  "email": "jane.citizen@example.com",
  "role": "CITIZEN"
}
```

---

### 3.2 Login User
Authenticate an existing account and obtain a JWT bearer token.

- **URL:** `/api/auth/login`
- **Method:** `POST`
- **Auth Required:** No
- **Headers:** `Content-Type: application/json`

#### Request Body
```json
{
  "email": "jane.citizen@example.com",
  "password": "Password123!"
}
```

#### Response: `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqYW5lLmNpdGl6ZW5AZXhhbXBsZS5jb20iLCJyb2xlIjoiQ0lUSVpFTiIsImlhdCI6MTY5ODExMjAwMCwiZXhwIjoxNjk4MTk4NDAwfQ.example_signature",
  "email": "jane.citizen@example.com",
  "role": "CITIZEN"
}
```

#### Error Response: `401 Unauthorized`
```json
{
  "timestamp": "2026-10-24T13:50:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid email or password"
}
```

---

## 4. Complaint Management Endpoints

### 4.1 Create Complaint (Report an Issue)
Files a new civic issue report. The system:
1. Runs the **Haversine Deduplication Engine** (queries for unclosed complaints in the same category within 50m).
2. If duplicate found: links new complaint as a child of the nearest parent (`parentComplaintId` set).
3. If unique: assigns to municipal department (`WASTE` → Sanitation, `ROADS` → Public Works, etc.) and sets status to `ASSIGNED`.
4. Uploads before-image to Local Disk or AWS S3.

- **URL:** `/api/complaints`
- **Method:** `POST`
- **Auth Required:** Yes (`CITIZEN` or `ADMIN`)
- **Headers:**
  - `Authorization: Bearer <token>`
  - `Content-Type: multipart/form-data`

#### Multipart Form Parts
| Part Name | Type | Content-Type | Required | Description |
|---|---|---|---|---|
| `complaint` | JSON Blob | `application/json` | **Yes** | Complaint metadata object |
| `beforeImage` | Binary File | `image/*` | No | Photo evidence of the issue |

#### `complaint` JSON Structure
| Field | Type | Constraints | Description |
|---|---|---|---|
| `title` | `string` | Required, 5-200 chars | Short title of the issue |
| `description` | `string` | Required, min 10 chars | Detailed explanation |
| `category` | `string` | Required (`WASTE`, `ROADS`, `STREETLIGHTS`, `OTHER`) | Categorization |
| `latitude` | `number` | Required, -90.0 to 90.0 | GPS latitude |
| `longitude` | `number` | Required, -180.0 to 180.0 | GPS longitude |

#### cURL Example
```bash
curl -X POST http://localhost:8080/api/complaints \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F 'complaint={"title":"Pothole on 5th Main","description":"Deep pothole causing bike skid","category":"ROADS","latitude":12.971598,"longitude":77.594562};type=application/json' \
  -F 'beforeImage=@/path/to/pothole.jpg'
```

#### Response: `201 Created`
```json
{
  "id": 1,
  "title": "Pothole on 5th Main",
  "description": "Deep pothole causing bike skid",
  "category": "ROADS",
  "status": "ASSIGNED",
  "latitude": 12.971598,
  "longitude": 77.594562,
  "beforeImageUrl": "http://localhost:8080/uploads/before/3fa85f64-5717-4562-b3fc-2c963f66afa6.jpg",
  "afterImageUrl": null,
  "departmentName": "Public Works",
  "reporterName": "Jane Citizen",
  "reporterEmail": "jane.citizen@example.com",
  "parentComplaintId": null,
  "duplicates": [],
  "createdAt": "2026-10-24T13:50:00Z",
  "updatedAt": "2026-10-24T13:50:00Z"
}
```

---

### 4.2 List Complaints (Paginated & Filtered)
Retrieves a paginated list of top-level complaints (child duplicates are excluded from this list to prevent queue clutter).

- **URL:** `/api/complaints`
- **Method:** `GET`
- **Auth Required:** Yes (Any authenticated role)
- **Headers:** `Authorization: Bearer <token>`

#### Query Parameters
| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| `status` | `string` | No | *All* | Filter by status (`REPORTED`, `ASSIGNED`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`) |
| `category` | `string` | No | *All* | Filter by category (`WASTE`, `ROADS`, `STREETLIGHTS`, `OTHER`) |
| `deptId` | `number` | No | *All* | Filter by department ID (e.g. `1`) |
| `page` | `number` | No | `0` | Zero-based page index |
| `size` | `number` | No | `10` | Page size (max capped at 50) |
| `sort` | `string` | No | `createdAt,desc` | Sort field and direction (`field,asc` or `field,desc`) |

#### cURL Example
```bash
curl -X GET "http://localhost:8080/api/complaints?status=ASSIGNED&category=ROADS&page=0&size=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### Response: `200 OK`
```json
{
  "content": [
    {
      "id": 1,
      "title": "Pothole on 5th Main",
      "category": "ROADS",
      "status": "ASSIGNED",
      "latitude": 12.971598,
      "longitude": 77.594562,
      "departmentName": "Public Works",
      "duplicateCount": 2,
      "createdAt": "2026-10-24T13:50:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

---

### 4.3 Get Complaint by ID
Fetches full details of a specific complaint, including information on reporter, department, before/after images, and **all child duplicate complaints linked to it**.

- **URL:** `/api/complaints/{id}`
- **Method:** `GET`
- **Auth Required:** Yes (Any authenticated role)
- **Headers:** `Authorization: Bearer <token>`

#### cURL Example
```bash
curl -X GET http://localhost:8080/api/complaints/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### Response: `200 OK`
```json
{
  "id": 1,
  "title": "Pothole on 5th Main",
  "description": "Deep pothole causing bike skid",
  "category": "ROADS",
  "status": "ASSIGNED",
  "latitude": 12.971598,
  "longitude": 77.594562,
  "beforeImageUrl": "http://localhost:8080/uploads/before/3fa85f64-5717-4562-b3fc-2c963f66afa6.jpg",
  "afterImageUrl": null,
  "departmentName": "Public Works",
  "reporterName": "Jane Citizen",
  "reporterEmail": "jane.citizen@example.com",
  "parentComplaintId": null,
  "duplicates": [
    {
      "id": 4,
      "title": "Huge road crater near bakery",
      "category": "ROADS",
      "status": "REPORTED",
      "latitude": 12.971620,
      "longitude": 77.594580,
      "departmentName": "Public Works",
      "duplicateCount": 0,
      "createdAt": "2026-10-24T14:15:00Z"
    }
  ],
  "createdAt": "2026-10-24T13:50:00Z",
  "updatedAt": "2026-10-24T13:50:00Z"
}
```

#### Error Response: `404 Not Found`
```json
{
  "timestamp": "2026-10-24T13:55:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Complaint with id [999] was not found"
}
```

---

### 4.4 Update Complaint Status
Transitions a complaint to its next lifecycle state.

#### State Machine Workflow
```
REPORTED ──► ASSIGNED ──► IN_PROGRESS ──► RESOLVED ──► CLOSED
                                              ▲
                                    afterImage REQUIRED
```

> [!IMPORTANT]
> Transitioning to `RESOLVED` **strictly requires** an `afterImage` file in the multipart request (or an existing verified `afterImageUrl`).

- **URL:** `/api/complaints/{id}/status`
- **Method:** `PUT`
- **Auth Required:** Yes (`WORKER` or `ADMIN`)
- **Headers:**
  - `Authorization: Bearer <token>`
  - `Content-Type: multipart/form-data`

#### Multipart Form Parts
| Part Name | Type | Content-Type | Required | Description |
|---|---|---|---|---|
| `statusUpdate` | JSON Blob | `application/json` | **Yes** | Target status JSON: `{"status": "..."}` |
| `afterImage` | Binary File | `image/*` | **Mandatory if status = RESOLVED** | Evidence photo of the fixed issue |

#### cURL Example (Transitioning to RESOLVED)
```bash
curl -X PUT http://localhost:8080/api/complaints/1/status \
  -H "Authorization: Bearer WORKER_JWT_TOKEN" \
  -F 'statusUpdate={"status":"RESOLVED"};type=application/json' \
  -F 'afterImage=@/path/to/fixed_road.jpg'
```

#### Response: `200 OK`
```json
{
  "id": 1,
  "title": "Pothole on 5th Main",
  "description": "Deep pothole causing bike skid",
  "category": "ROADS",
  "status": "RESOLVED",
  "latitude": 12.971598,
  "longitude": 77.594562,
  "beforeImageUrl": "http://localhost:8080/uploads/before/3fa85f64-5717-4562-b3fc-2c963f66afa6.jpg",
  "afterImageUrl": "http://localhost:8080/uploads/after/7eb85b12-4217-4862-a3fc-8c963f66bba9.jpg",
  "departmentName": "Public Works",
  "reporterName": "Jane Citizen",
  "reporterEmail": "jane.citizen@example.com",
  "parentComplaintId": null,
  "duplicates": [],
  "createdAt": "2026-10-24T13:50:00Z",
  "updatedAt": "2026-10-24T14:30:00Z"
}
```

#### Error Response: `422 Unprocessable Entity` (Evidence missing or invalid transition)
```json
{
  "timestamp": "2026-10-24T14:31:00Z",
  "status": 422,
  "error": "Invalid State Transition",
  "message": "Cannot mark complaint RESOLVED without an after-evidence photo."
}
```

---

## 5. Static File Access

When running with local storage (`USE_LOCAL_STORAGE=true`), uploaded files can be directly retrieved in the browser or front-end client:

- **Before Images:** `GET http://localhost:8080/uploads/before/{uuid}.jpg`
- **After Images:** `GET http://localhost:8080/uploads/after/{uuid}.jpg`
- **Auth Required:** No (Public static resource route configured in `WebConfig.java`)

---

## 6. Standard Error Format

All error responses from the API follow the unified `ApiError` format:

```json
{
  "timestamp": "2026-10-24T14:00:00Z",
  "status": 400,
  "error": "Validation Failed",
  "message": "Request validation errors",
  "fieldErrors": [
    "title: Title must be between 5 and 200 characters",
    "latitude: Latitude must be <= 90"
  ]
}
```
