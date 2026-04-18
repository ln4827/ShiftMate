# ShiftMate

Restaurant staff and shift scheduling system built for **ISTE-432 Database Application Development**.

**Authors:** Lucija Nesnidal, Karmen Penga

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Database | MySQL 8.0 |
| Backend | Java 25, Spring Boot 3.2, Spring Data JPA, Spring Security |
| Frontend | React 18, Vite 5, React Router 6 |
| Build | Maven 3.9 (frontend built automatically via `frontend-maven-plugin`) |

---

## Prerequisites

- Java 25+
- Maven 3.9+
- MySQL 8.0+
- Node.js 20+ *(only needed for frontend development — Maven downloads its own copy for production builds)*

---

## Database Setup

```sql
-- Run the schema script in MySQL
source src/main/resources/db/schema.sql
```

This creates the `shiftmate` database with all 12 tables and sample data.

Then create a dedicated user:

```sql
CREATE USER 'shiftmate_user'@'localhost' IDENTIFIED BY 'shiftmate_pass';
GRANT ALL PRIVILEGES ON shiftmate.* TO 'shiftmate_user'@'localhost';
FLUSH PRIVILEGES;
```

---

## Running the Project

### Development (recommended)

Run the backend and frontend dev server separately so you get hot reload on both sides.

**Terminal 1 — Backend:**
```bash
mvn spring-boot:run -Dmaven.frontend.skip=true
```

**Terminal 2 — Frontend:**
```bash
cd src/main/frontend
npm install       # first time only
npm run dev       # starts at http://localhost:5173
```

The Vite dev server proxies all `/api` requests to `localhost:8080`, so no CORS issues.

### Production build

```bash
mvn package
java -jar target/shiftmate-0.0.1-SNAPSHOT.jar
```

Maven automatically runs `npm install` and `npm run build`, which compiles React into `src/main/resources/static/`. The result is a single self-contained `.jar` that serves both the API and the frontend at `http://localhost:8080`.

---

## Project Structure

```
shiftmate/
├── src/main/
│   ├── java/com/shiftmate/
│   │   ├── config/          # Security, CORS
│   │   ├── controller/      # REST controllers + SPA fallback
│   │   ├── dto/             # Request / response objects
│   │   ├── entity/          # JPA entities (one per DB table)
│   │   ├── exception/       # Custom exception classes
│   │   ├── repository/      # Spring Data JPA repositories
│   │   ├── security/        # UserDetails, UserDetailsService
│   │   └── service/         # Business logic (interface + impl)
│   ├── resources/
│   │   ├── db/schema.sql    # Full MySQL schema + sample data
│   │   ├── static/          # Vite build output (git-ignored, auto-generated)
│   │   └── application.properties
│   └── frontend/            # React source
│       ├── src/
│       │   ├── api/         # API client wrappers
│       │   ├── pages/       # Page components
│       │   └── App.jsx      # Router + auth context
│       ├── package.json
│       └── vite.config.js
└── pom.xml
```

---

## Database Schema

12 tables covering the full scheduling domain:

| Table | Description |
|-------|-------------|
| `restaurant` | Top-level tenant — all data is scoped to a restaurant |
| `department` | Organisational units within a restaurant (Kitchen, Bar, etc.) |
| `role` | Staff roles scoped per restaurant (Chef, Waiter, etc.) |
| `employee` | All staff members; `is_manager` flag controls access |
| `employee_role` | Junction table — employees to roles (many-to-many) |
| `shift` | Scheduled work periods with start/end times |
| `shift_assignment` | Which employee is assigned to which shift |
| `shift_coverage_requirement` | How many staff of each role a shift needs |
| `availability` | Employee weekly availability windows |
| `time_off_request` | PTO / sick leave requests |
| `swap_request` | Shift swap requests between employees |
| `notification` | In-app notifications per employee |

---

## API Endpoints

All endpoints are prefixed with `/api`. Authentication uses session cookies.

### Auth — `/api/auth`

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| `POST` | `/login` | Public | Log in — body: `{ email, password }` |
| `POST` | `/logout` | Auth | End the current session |
| `GET` | `/me` | Auth | Returns current user info |

### Employees — `/api/employees`

| Method | Path | Access | Description |
|--------|------|--------|-------------|
| `GET` | `/` | Manager | List all employees |
| `POST` | `/` | Manager | Create a new employee |
| `GET` | `/{id}` | Any | Get employee by ID |
| `PUT` | `/{id}` | Manager | Update employee profile |
| `POST` | `/{id}/deactivate` | Manager | Soft-deactivate an employee |
| `POST` | `/{id}/reactivate` | Manager | Reactivate an employee |
| `PUT` | `/{id}/roles` | Manager | Replace all role assignments |
| `POST` | `/{id}/roles/{roleId}` | Manager | Add a single role |
| `DELETE` | `/{id}/roles/{roleId}` | Manager | Remove a single role |
| `GET` | `/available-roles` | Manager | List roles for this restaurant |

---

## Architecture

The backend follows a strict layered architecture:

```
Controller → Service (interface) → ServiceImpl → Repository → Entity → DB
```

- **Controllers** handle HTTP, validation, and auth — no business logic
- **Services** enforce business rules and orchestrate repositories
- **Repositories** are Spring Data JPA interfaces — no SQL written by hand
- **Entities** map 1-to-1 with database tables via JPA annotations
- **DTOs** (`*Request`, `*Response`) decouple the API contract from the entity model

Optimistic locking (`@Version`) on the `employee` table prevents concurrent edits from silently overwriting each other.

---

## Configuration

`src/main/resources/application.properties` contains the database connection. Update these values to match your local setup:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/shiftmate?useSSL=false&serverTimezone=Europe/Zagreb
spring.datasource.username=shiftmate_user
spring.datasource.password=shiftmate_pass
```
