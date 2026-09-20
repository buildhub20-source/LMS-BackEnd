# LMS Backend Service

> **Multi-tenant Learning Management System — Core API**
> Spring Boot 3.3 · Java 17 · PostgreSQL · Flyway · JWT · Cloudflare R2

---

## Overview

The LMS Backend is the central REST API powering the entire Learning Management System. It manages authentication, authorization, course management, assessments, enrollments, gamification, analytics, and the multi-tenant control plane. Built as a modular Spring Boot monolith, it exposes a versioned REST API (`/api/v1/`) consumed by the React frontend and all satellite microservices.

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                    LMS Backend (8080)                     │
├──────────────────────────────────────────────────────────┤
│  Security Layer (JWT + RBAC + Permission Guards)         │
├──────────────────────────────────────────────────────────┤
│  Modules:                                                │
│  ┌─────────┐ ┌──────────┐ ┌────────────┐ ┌───────────┐  │
│  │  Auth   │ │  Course  │ │ Assessment │ │ Enrollment│  │
│  └─────────┘ └──────────┘ └────────────┘ └───────────┘  │
│  ┌─────────┐ ┌──────────┐ ┌────────────┐ ┌───────────┐  │
│  │Gamificat│ │ Analytics│ │  Resource  │ │   Notes   │  │
│  └─────────┘ └──────────┘ └────────────┘ └───────────┘  │
│  ┌─────────┐ ┌──────────┐ ┌────────────┐ ┌───────────┐  │
│  │Calendar │ │ Invitation│ │   Role    │ │ Permission│  │
│  └─────────┘ └──────────┘ └────────────┘ └───────────┘  │
│  ┌─────────┐ ┌──────────┐ ┌────────────┐               │
│  │Platform │ │ Student  │ │ Instructor │               │
│  └─────────┘ └──────────┘ └────────────┘               │
├──────────────────────────────────────────────────────────┤
│  Data: PostgreSQL (Supabase) · Flyway · Cloudflare R2   │
└──────────────────────────────────────────────────────────┘
```

## Tech Stack

| Layer          | Technology                              |
|----------------|-----------------------------------------|
| Runtime        | Java 17, Spring Boot 3.3.4              |
| Web Framework  | Spring Web MVC, Spring Validation       |
| Persistence    | Spring Data JPA, Hibernate, Flyway      |
| Database       | PostgreSQL (Supabase managed)           |
| Security       | Spring Security, JWT (jjwt 0.12.6)      |
| Object Storage | Cloudflare R2 (AWS SDK v2)              |
| API Docs       | SpringDoc OpenAPI 2.6.0 (Swagger UI)    |
| Mapping        | MapStruct 1.6.2, Lombok 1.18.48        |
| Mail           | Spring Boot Starter Mail                |
| Ops            | Spring Boot Actuator                    |
| Testing        | JUnit 5, Spring Test, H2 (in-memory)   |

## Module Breakdown

### Authentication & Authorization (`com.lms.auth`, `com.lms.security`)
- JWT-based login with access + refresh token rotation
- Magic-link invitation acceptance
- Password reset flow (forgot → email → reset)
- Session management (list, revoke, revoke-all)
- Multi-session limit enforcement (configurable max)
- Account lockout after configurable failed attempts

### Role-Based Access Control (`com.lms.role`, `com.lms.permission`)
- Hierarchical roles: `SUPER_ADMIN`, `ADMIN`, `INSTRUCTOR`, `STUDENT`
- Fine-grained permissions (e.g., `COURSE_VIEW`, `ASSESSMENT_CREATE`, `GAMIFICATION_VIEW`)
- Spring Security `@PreAuthorize` guards on every endpoint

### Course Management (`com.lms.course`)
- Full CRUD with lifecycle: `DRAFT → SUBMITTED → APPROVED/REJECTED → PUBLISHED → UNPUBLISHED → ARCHIVED`
- Curriculum builder: Modules → Lessons (hierarchical structure)
- Lesson thumbnail upload and video recording management
- Pre-signed Cloudflare R2 upload URLs for large media
- Course duplication (deep clone with curriculum)
- Course analytics (per-course enrollment & completion metrics)
- Learning progress tracking (per-lesson completion state)
- Instructor ownership enforcement

### Assessment Engine (`com.lms.assessment`)
- Assessment lifecycle: `DRAFT → PUBLISHED → CLOSED → ARCHIVED`
- Multi-section assessments with per-section configuration
- Question types: Coding challenges, MCQ (Multiple Choice)
- Question bank with reusable questions
- Randomization support (question and option shuffling)
- Scheduled assessment windows (start/end times, deadline extensions)
- Student attempt management (start, auto-save, submit, retest grants)
- Auto-grading + manual grading workflow
- Rubric system for consistent grading
- Assessment analytics (score distribution, attempt stats, per-student performance)
- Result analytics visibility toggle

### Enrollment Management (`com.lms.enrollment`)
- Bulk and individual student enrollment
- Enrollment statuses: `ACTIVE`, `COMPLETED`, `DROPPED`, `SUSPENDED`
- Enrollment-driven events (triggers certificate issuance, gamification)
- Per-student learning progress tracking with lesson completion

### Gamification (`com.lms.gamification`)
- Points system with configurable point values per action
- Badge framework (earn badges for achievements)
- Milestone tracking (progress toward defined goals)
- Learning streak tracking (consecutive active days)
- Leaderboard (global, per-batch, time-period filtering)
- Admin configuration panel for rules and rewards

### Analytics (`com.lms.analytics`)
- **Admin Dashboard**: Active learners, course stats, enrollment/completion rates, pending invitations, action queue, top courses, at-risk courses
- **Instructor Dashboard**: Student counts, course counts, enrollment stats, average completion
- **Student Dashboard**: Enrolled/completed courses, hours learned, overall progress, streak, weekly activity chart

### Campus Resources (`com.lms.resource`)
- Study toolkit management: upload cheatsheets, guides, PDFs
- Cloudflare R2 storage with pre-signed upload/download URLs
- Direct file streaming endpoint
- Category and status-based filtering

### Notes & Bookmarks (`com.lms.notes`)
- Per-user notes on courses/lessons
- Bookmark system for quick access to saved content

### Calendar (`com.lms.calendar`)
- Student calendar view (assessment deadlines, events)

### Invitation System (`com.lms.invitation`)
- Magic-link email invitations
- Role-scoped invitations (Admin, Instructor, Student)
- Configurable TTL (default 7 days)
- Invitation tracking and management

### User Management (`com.lms.user`, `com.lms.student`, `com.lms.instructor`)
- Full user CRUD with profile management
- Student profiles (batch assignment, academic details)
- Instructor profiles (specialization, bio)
- Profile image support

### Organization Settings (`com.lms.organization`)
- Tenant-level branding and configuration

### Multi-Tenant Control Plane (`com.lms.platform`)
- Tenant provisioning (auto-create Supabase projects)
- Tenant configuration management
- Platform admin authentication (separate from tenant admins)
- Tenant impersonation for support
- Cross-tenant audit logging
- Platform-wide announcements
- Dashboard overview across all tenants

## API Endpoints

All endpoints are prefixed with `/api/v1/`:

| Module              | Base Path                         | Key Operations                               |
|---------------------|-----------------------------------|----------------------------------------------|
| Auth                | `/auth`                           | login, refresh, logout, forgot/reset password |
| Users               | `/users`                          | CRUD, profile management                     |
| Roles               | `/roles`                          | List, details, assignments                   |
| Permissions         | `/permissions`                    | List available permissions                   |
| Invitations         | `/invitations`                    | Send, list, resend, revoke                   |
| Courses             | `/courses`                        | CRUD, lifecycle, recordings, curriculum      |
| Curriculum          | `/courses/{id}/curriculum`        | Modules, lessons, thumbnails, recordings     |
| Learning            | `/learning/courses`               | Course consumption, progress tracking        |
| Admin Assessments   | `/admin/assessments`              | CRUD, lifecycle, analytics, grading          |
| Student Assessments | `/student/assessments`            | Available tests, attempts, submissions       |
| Enrollments         | `/students`, `/batches`           | Enroll, manage enrollment status             |
| Gamification        | `/gamification`                   | Summary, badges, milestones, leaderboard     |
| Analytics           | `/analytics`                      | Admin, instructor, student dashboards        |
| Resources           | `/resources`                      | CRUD, upload, download, stream               |
| Notes               | `/student/notes`                  | Create, list, update, delete                 |
| Bookmarks           | `/student/bookmarks`              | Toggle, list                                 |
| Calendar            | `/student/calendar`               | Events and deadlines                         |
| Organization        | `/organization`                   | Branding and settings                        |
| Platform            | `/platform`                       | Tenant CRUD, config, audit, announcements    |
| Internal            | `/internal`                       | Service-to-service (X-Service-Key)           |
| Well-Known          | `/.well-known`                    | Discovery endpoints                          |

## Prerequisites

- **Java 17** (JDK)
- **Maven 3.8+**
- **PostgreSQL 14+** (or Supabase managed instance)
- **Cloudflare R2** account (for object storage)
- **SMTP server** (optional, for email delivery)

## Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd LMS-BackEnd
   ```

2. **Configure environment**
   ```bash
   cp .env.example .env
   # Fill in all required values (DB, JWT, R2, SMTP)
   ```

3. **Key environment variables**
   | Variable                  | Description                                  |
   |---------------------------|----------------------------------------------|
   | `DB_URL`                  | JDBC PostgreSQL connection string             |
   | `DB_USERNAME` / `DB_PASSWORD` | Database credentials                    |
   | `DB_SCHEMA`               | Database schema (default: `lms`)             |
   | `JWT_SECRET`              | Min 32 bytes, shared with all services       |
   | `CORS_ALLOWED_ORIGINS`    | Comma-separated frontend URLs                |
   | `SERVICE_KEY_SECRET`      | Shared secret for service-to-service auth    |
   | `CERT_SERVICE_URL`        | Certificate service base URL                 |
   | `MAIL_ENABLED`            | `true` to send real emails                   |
   | `BOOTSTRAP_ADMIN_ENABLED` | `true` for first-run admin creation          |

4. **Build and run**
   ```bash
   # Build
   mvn clean package -DskipTests

   # Run
   java -jar target/lms-backend-0.0.1-SNAPSHOT.jar

   # Or using Maven
   mvn spring-boot:run
   ```

5. **Database migrations** run automatically on startup via Flyway (50+ versioned migrations).

6. **Swagger UI** available at: `http://localhost:8080/swagger-ui.html`

## First-Run Bootstrap

1. Set `BOOTSTRAP_ADMIN_ENABLED=true` with admin credentials in `.env`
2. Start the application — the admin account is created automatically
3. Set `BOOTSTRAP_ADMIN_ENABLED=false` and restart
4. All subsequent users are created via the invitation flow

## Project Structure

```
src/main/java/com/lms/
├── LmsApplication.java         # Spring Boot entry point
├── analytics/                  # Dashboard analytics
├── assessment/                 # Assessment engine (questions, attempts, grading)
├── auth/                       # Authentication (login, JWT, sessions, password reset)
├── calendar/                   # Student calendar
├── common/                     # Shared utilities, exceptions, response wrappers
├── config/                     # App config (Security, CORS, JWT, Flyway, OpenAPI)
├── course/                     # Course management & curriculum builder
├── enrollment/                 # Student enrollment & progress
├── gamification/               # Points, badges, milestones, leaderboard
├── instructor/                 # Instructor profiles
├── invitation/                 # Magic-link invitation system
├── notes/                      # Student notes & bookmarks
├── organization/               # Organization/tenant settings
├── permission/                 # Permission entities & management
├── platform/                   # Multi-tenant control plane
├── resource/                   # Campus resource management
├── role/                       # Role entities & management
├── security/                   # JWT filters, authentication, authorization
├── student/                    # Student profiles
└── user/                       # User entities & management
```

## Service Integration

| Service              | Protocol       | Purpose                          |
|----------------------|----------------|----------------------------------|
| Certificate Service  | HTTP (REST)    | PDF generation & issuance        |
| Chat Service         | Shared JWT     | Auth token validation            |
| Notification Service | HTTP (webhook) | Push notifications on events     |
| Frontend             | REST + CORS    | Primary UI consumer              |
