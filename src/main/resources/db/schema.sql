-- =============================================================================
-- ShiftMate — Restaurant Staff & Shift Scheduling System
-- Database Schema + Sample Data
-- ISTE-432 Database Application Development
-- Authors: Lucija Nesnidal, Karmen Penga
-- Version: 1.0  |  Date: 2026-04-10
-- DBMS: MySQL 8.0
-- =============================================================================

CREATE DATABASE IF NOT EXISTS shiftmate;

USE shiftmate;

-- =============================================================================
-- SCHEMA
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. restaurant
--    Top-level entity. All other data belongs to a restaurant.
-- -----------------------------------------------------------------------------
CREATE TABLE restaurant (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    name        VARCHAR(120)    NOT NULL,
    address     VARCHAR(255)    NOT NULL,
    city        VARCHAR(100)    NOT NULL,
    phone       VARCHAR(30)     NOT NULL,
    email       VARCHAR(150)    NOT NULL,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_restaurant PRIMARY KEY (id),
    CONSTRAINT uq_restaurant_email UNIQUE (email)
);

-- -----------------------------------------------------------------------------
-- 2. department
--    Organisational unit within a restaurant (Kitchen, Bar, Front of House…).
-- -----------------------------------------------------------------------------
CREATE TABLE department (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    restaurant_id   BIGINT          NOT NULL,
    name            VARCHAR(80)     NOT NULL,

    CONSTRAINT pk_department PRIMARY KEY (id),
    CONSTRAINT fk_department_restaurant
        FOREIGN KEY (restaurant_id) REFERENCES restaurant (id)
        ON DELETE CASCADE,
    CONSTRAINT uq_department_name UNIQUE (restaurant_id, name)
);

-- -----------------------------------------------------------------------------
-- 3. role
--    Staff roles that exist across the restaurant (Chef, Waiter, etc.).
--    Scoped per restaurant to allow custom role names.
-- -----------------------------------------------------------------------------
CREATE TABLE role (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    restaurant_id   BIGINT          NOT NULL,
    name            VARCHAR(80)     NOT NULL,

    CONSTRAINT pk_role PRIMARY KEY (id),
    CONSTRAINT fk_role_restaurant
        FOREIGN KEY (restaurant_id) REFERENCES restaurant (id)
        ON DELETE CASCADE,
    CONSTRAINT uq_role_name UNIQUE (restaurant_id, name)
);

-- -----------------------------------------------------------------------------
-- 4. department_allowed_roles
--    Defines which roles are eligible to work in a given department.
--    If a department has no rows here, any role may be assigned (backwards compat).
--    Must come after both `department` and `role`.
-- -----------------------------------------------------------------------------
CREATE TABLE department_allowed_roles (
    department_id   BIGINT  NOT NULL,
    role_id         BIGINT  NOT NULL,

    CONSTRAINT pk_dar PRIMARY KEY (department_id, role_id),
    CONSTRAINT fk_dar_department
        FOREIGN KEY (department_id) REFERENCES department (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_dar_role
        FOREIGN KEY (role_id) REFERENCES role (id)
        ON DELETE CASCADE
);

-- -----------------------------------------------------------------------------
-- 6. employee
--    All staff members including managers. is_manager flag distinguishes roles.
--    password_hash stores BCrypt hash (never plain text).
-- -----------------------------------------------------------------------------
CREATE TABLE employee (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    restaurant_id   BIGINT          NOT NULL,
    first_name      VARCHAR(80)     NOT NULL,
    last_name       VARCHAR(80)     NOT NULL,
    email           VARCHAR(150)    NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    is_manager      TINYINT(1)      NOT NULL DEFAULT 0,
    is_active       TINYINT(1)      NOT NULL DEFAULT 1,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         BIGINT          NOT NULL DEFAULT 0,   -- optimistic locking

    CONSTRAINT pk_employee PRIMARY KEY (id),
    CONSTRAINT fk_employee_restaurant
        FOREIGN KEY (restaurant_id) REFERENCES restaurant (id)
        ON DELETE CASCADE,
    CONSTRAINT uq_employee_email UNIQUE (email)
);

-- -----------------------------------------------------------------------------
-- 7. employee_role
--    Many-to-many: one employee can hold multiple roles (e.g. Chef + Sous Chef).
-- -----------------------------------------------------------------------------
CREATE TABLE employee_role (
    employee_id BIGINT  NOT NULL,
    role_id     BIGINT  NOT NULL,

    CONSTRAINT pk_employee_role PRIMARY KEY (employee_id, role_id),
    CONSTRAINT fk_er_employee
        FOREIGN KEY (employee_id) REFERENCES employee (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_er_role
        FOREIGN KEY (role_id) REFERENCES role (id)
        ON DELETE CASCADE
);

-- -----------------------------------------------------------------------------
-- 8. availability
--    Weekly recurring availability set by the employee.
--    day_of_week: 1=Monday … 7=Sunday (ISO).
-- -----------------------------------------------------------------------------
CREATE TABLE availability (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    employee_id BIGINT      NOT NULL,
    day_of_week TINYINT     NOT NULL,   -- 1 (Mon) – 7 (Sun)
    start_time  TIME        NOT NULL,
    end_time    TIME        NOT NULL,

    CONSTRAINT pk_availability PRIMARY KEY (id),
    CONSTRAINT fk_availability_employee
        FOREIGN KEY (employee_id) REFERENCES employee (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_availability_day CHECK (day_of_week BETWEEN 1 AND 7)
);

-- -----------------------------------------------------------------------------
-- 9. shift
--    A scheduled shift slot. is_published controls employee visibility.
--    version column enables optimistic locking in Hibernate.
-- -----------------------------------------------------------------------------
CREATE TABLE shift (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    department_id   BIGINT      NOT NULL,
    shift_date      DATE        NOT NULL,
    start_time      TIME        NOT NULL,
    end_time        TIME        NOT NULL,
    is_published    TINYINT(1)  NOT NULL DEFAULT 0,
    created_by      BIGINT      NOT NULL,   -- FK to employee (manager)
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         BIGINT      NOT NULL DEFAULT 0,   -- optimistic locking

    CONSTRAINT pk_shift PRIMARY KEY (id),
    CONSTRAINT fk_shift_department
        FOREIGN KEY (department_id) REFERENCES department (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_shift_created_by
        FOREIGN KEY (created_by) REFERENCES employee (id)
        ON DELETE RESTRICT
);

-- -----------------------------------------------------------------------------
-- 10. shift_assignment
--    Assigns one employee to one shift in one role.
--    version column enables optimistic locking — critical for concurrency (FR-07, AT-12).
-- -----------------------------------------------------------------------------
CREATE TABLE shift_assignment (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    shift_id    BIGINT      NOT NULL,
    employee_id BIGINT      NOT NULL,
    role_id     BIGINT      NOT NULL,
    assigned_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version     BIGINT      NOT NULL DEFAULT 0,   -- optimistic locking

    CONSTRAINT pk_shift_assignment PRIMARY KEY (id),
    CONSTRAINT fk_sa_shift
        FOREIGN KEY (shift_id) REFERENCES shift (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_sa_employee
        FOREIGN KEY (employee_id) REFERENCES employee (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_sa_role
        FOREIGN KEY (role_id) REFERENCES role (id)
        ON DELETE RESTRICT,
    -- One employee can only be assigned once per shift
    CONSTRAINT uq_sa_shift_employee UNIQUE (shift_id, employee_id)
);

-- -----------------------------------------------------------------------------
-- 11. shift_coverage_requirement
--    Minimum number of employees per role required for a shift to be valid.
--    Used for coverage validation (FR-08, FR-09, AT-06).
-- -----------------------------------------------------------------------------
CREATE TABLE shift_coverage_requirement (
    id          BIGINT  NOT NULL AUTO_INCREMENT,
    shift_id    BIGINT  NOT NULL,
    role_id     BIGINT  NOT NULL,
    min_count   INT     NOT NULL DEFAULT 1,

    CONSTRAINT pk_scr PRIMARY KEY (id),
    CONSTRAINT fk_scr_shift
        FOREIGN KEY (shift_id) REFERENCES shift (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_scr_role
        FOREIGN KEY (role_id) REFERENCES role (id)
        ON DELETE CASCADE,
    CONSTRAINT uq_scr_shift_role UNIQUE (shift_id, role_id),
    CONSTRAINT chk_scr_min_count CHECK (min_count >= 1)
);

-- -----------------------------------------------------------------------------
-- 12. swap_request
--     Tracks shift swap requests between employees (FR-13, FR-14, FR-15).
--     status: PENDING | APPROVED | REJECTED
--     Both assignment FKs are preserved after a swap for audit purposes.
-- -----------------------------------------------------------------------------
CREATE TABLE swap_request (
    id                          BIGINT          NOT NULL AUTO_INCREMENT,
    requester_assignment_id     BIGINT          NOT NULL,
    target_assignment_id        BIGINT          NOT NULL,
    status                      VARCHAR(10)     NOT NULL DEFAULT 'PENDING',
    requested_at                DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at                 DATETIME        NULL,
    resolved_by                 BIGINT          NULL,   -- FK to employee (manager)

    CONSTRAINT pk_swap_request PRIMARY KEY (id),
    CONSTRAINT fk_sr_requester_assignment
        FOREIGN KEY (requester_assignment_id) REFERENCES shift_assignment (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_sr_target_assignment
        FOREIGN KEY (target_assignment_id) REFERENCES shift_assignment (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_sr_resolved_by
        FOREIGN KEY (resolved_by) REFERENCES employee (id)
        ON DELETE SET NULL,
    CONSTRAINT chk_sr_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    -- Prevent duplicate open requests for the same pair
    CONSTRAINT uq_sr_assignments UNIQUE (requester_assignment_id, target_assignment_id)
);

-- -----------------------------------------------------------------------------
-- 13. time_off_request
--     Employee requests for time off (FR-16, FR-17).
--     status: PENDING | APPROVED | REJECTED
-- -----------------------------------------------------------------------------
CREATE TABLE time_off_request (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    employee_id BIGINT          NOT NULL,
    start_date  DATE            NOT NULL,
    end_date    DATE            NOT NULL,
    reason      VARCHAR(500)    NOT NULL,
    status      VARCHAR(10)     NOT NULL DEFAULT 'PENDING',
    requested_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at     DATETIME    NULL,
    resolved_by     BIGINT      NULL,   -- FK to employee (manager)

    CONSTRAINT pk_time_off_request PRIMARY KEY (id),
    CONSTRAINT fk_tor_employee
        FOREIGN KEY (employee_id) REFERENCES employee (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_tor_resolved_by
        FOREIGN KEY (resolved_by) REFERENCES employee (id)
        ON DELETE SET NULL,
    CONSTRAINT chk_tor_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_tor_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

-- -----------------------------------------------------------------------------
-- 14. notification
--     In-app notification records stored in the database (FR-18, FR-19).
--     NOT real-time push — polled on page load / navigation.
--     type: SCHEDULE_PUBLISHED | SWAP_APPROVED | SWAP_REJECTED |
--           TIMEOFF_APPROVED | TIMEOFF_REJECTED | SWAP_REQUESTED | GENERAL
-- -----------------------------------------------------------------------------
CREATE TABLE notification (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    employee_id BIGINT          NOT NULL,
    message     VARCHAR(500)    NOT NULL,
    type        VARCHAR(30)     NOT NULL DEFAULT 'GENERAL',
    is_read     TINYINT(1)      NOT NULL DEFAULT 0,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_notification PRIMARY KEY (id),
    CONSTRAINT fk_notification_employee
        FOREIGN KEY (employee_id) REFERENCES employee (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_notification_type CHECK (
        type IN ('SCHEDULE_PUBLISHED', 'SWAP_APPROVED', 'SWAP_REJECTED',
                 'TIMEOFF_APPROVED', 'TIMEOFF_REJECTED', 'TIMEOFF_REQUESTED', 'SWAP_REQUESTED', 'GENERAL')
    )
);

-- =============================================================================
-- INDEXES
-- Indexes on frequently queried foreign keys and filter columns.
-- =============================================================================

-- Speeds up weekly schedule queries (shift_date range scans)
CREATE INDEX idx_shift_date          ON shift (shift_date);
CREATE INDEX idx_shift_department    ON shift (department_id);
CREATE INDEX idx_shift_published     ON shift (is_published);

-- Speeds up "all assignments for employee" and "all assignments for shift"
CREATE INDEX idx_sa_employee         ON shift_assignment (employee_id);
CREATE INDEX idx_sa_shift            ON shift_assignment (shift_id);

-- Speeds up availability lookup by employee + day
CREATE INDEX idx_avail_employee_day  ON availability (employee_id, day_of_week);

-- Speeds up pending request queries
CREATE INDEX idx_sr_status           ON swap_request (status);
CREATE INDEX idx_tor_status          ON time_off_request (status);
CREATE INDEX idx_tor_employee        ON time_off_request (employee_id);

-- Speeds up unread notification badge count
CREATE INDEX idx_notif_employee_read ON notification (employee_id, is_read);

-- =============================================================================
-- SAMPLE DATA
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Restaurant
-- -----------------------------------------------------------------------------
INSERT INTO restaurant (name, address, city, phone, email) VALUES
('Konoba Dalmatino', 'Ilica 42', 'Zagreb', '+385 1 555 0100', 'info@dalmatino.hr');

-- -----------------------------------------------------------------------------
-- Departments  (restaurant_id = 1)
-- -----------------------------------------------------------------------------
INSERT INTO department (restaurant_id, name) VALUES
(1, 'Kitchen'),
(1, 'Bar'),
(1, 'Front of House');

-- -----------------------------------------------------------------------------
-- Roles  (restaurant_id = 1)
-- id: 1=Head Chef, 2=Sous Chef, 3=Line Cook, 4=Bartender, 5=Waiter, 6=Host, 7=Manager
-- -----------------------------------------------------------------------------
INSERT INTO role (restaurant_id, name) VALUES
(1, 'Head Chef'),
(1, 'Sous Chef'),
(1, 'Line Cook'),
(1, 'Bartender'),
(1, 'Waiter'),
(1, 'Host'),
(1, 'Manager');

-- -----------------------------------------------------------------------------
-- Employees
-- NOTE: password_hash values below are BCrypt hashes of 'Password1!'
-- In production, Hibernate/Spring Security generates these — never store plaintext.
-- All employees belong to restaurant_id = 1.
-- id: 1=Manager Ana, 2=Manager Ivan, 3-10=Employees
-- -----------------------------------------------------------------------------
INSERT INTO employee (restaurant_id, first_name, last_name, email, password_hash, is_manager, is_active) VALUES
-- Managers
(1, 'Ana',     'Kovač',     'ana.kovac@dalmatino.hr',     '$2a$10$zh5JX8cDvocPExOGQ4/5q./JHKYlXuPlEjLTMOix9kUy5rFVvBEnm', 1, 1),
(1, 'Ivan',    'Perić',     'ivan.peric@dalmatino.hr',    '$2a$10$zh5JX8cDvocPExOGQ4/5q./JHKYlXuPlEjLTMOix9kUy5rFVvBEnm', 1, 1),
-- Kitchen staff
(1, 'Marko',   'Horvat',    'marko.horvat@dalmatino.hr',  '$2a$10$zh5JX8cDvocPExOGQ4/5q./JHKYlXuPlEjLTMOix9kUy5rFVvBEnm', 0, 1),
(1, 'Lena',    'Babić',     'lena.babic@dalmatino.hr',    '$2a$10$zh5JX8cDvocPExOGQ4/5q./JHKYlXuPlEjLTMOix9kUy5rFVvBEnm', 0, 1),
(1, 'Tomislav','Novak',     'tomislav.novak@dalmatino.hr','$2a$10$zh5JX8cDvocPExOGQ4/5q./JHKYlXuPlEjLTMOix9kUy5rFVvBEnm', 0, 1),
-- Bar staff
(1, 'Sara',    'Jurić',     'sara.juric@dalmatino.hr',    '$2a$10$zh5JX8cDvocPExOGQ4/5q./JHKYlXuPlEjLTMOix9kUy5rFVvBEnm', 0, 1),
-- Front of House
(1, 'Petra',   'Blažević',  'petra.blazevic@dalmatino.hr','$2a$10$zh5JX8cDvocPExOGQ4/5q./JHKYlXuPlEjLTMOix9kUy5rFVvBEnm', 0, 1),
(1, 'Nikola',  'Šimić',     'nikola.simic@dalmatino.hr',  '$2a$10$zh5JX8cDvocPExOGQ4/5q./JHKYlXuPlEjLTMOix9kUy5rFVvBEnm', 0, 1),
(1, 'Maja',    'Tomić',     'maja.tomic@dalmatino.hr',    '$2a$10$zh5JX8cDvocPExOGQ4/5q./JHKYlXuPlEjLTMOix9kUy5rFVvBEnm', 0, 1),
(1, 'Dario',   'Knežević',  'dario.knezevic@dalmatino.hr','$2a$10$zh5JX8cDvocPExOGQ4/5q./JHKYlXuPlEjLTMOix9kUy5rFVvBEnm', 0, 1);

-- -----------------------------------------------------------------------------
-- Department Allowed Roles  (department_id, role_id)
-- Kitchen (1): Head Chef, Sous Chef, Line Cook
-- Bar (2): Bartender
-- Front of House (3): Waiter, Host
-- -----------------------------------------------------------------------------
INSERT INTO department_allowed_roles (department_id, role_id) VALUES
(1, 1), (1, 2), (1, 3),
(2, 4),
(3, 5), (3, 6);

-- -----------------------------------------------------------------------------
-- Employee Roles  (employee_id, role_id)
-- -----------------------------------------------------------------------------
INSERT INTO employee_role (employee_id, role_id) VALUES
-- Managers have Manager role
(1, 7),
(2, 7),
-- Marko: Head Chef
(3, 1),
-- Lena: Sous Chef
(4, 2),
-- Tomislav: Line Cook
(5, 3),
-- Sara: Bartender
(6, 4),
-- Petra, Nikola, Maja: Waiters
(7, 5),
(8, 5),
(9, 5),
-- Dario: Host + Waiter (multi-role example)
(10, 5),
(10, 6);

-- -----------------------------------------------------------------------------
-- Availability  (weekly recurring windows)
-- day_of_week: 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat, 7=Sun
-- -----------------------------------------------------------------------------
INSERT INTO availability (employee_id, day_of_week, start_time, end_time) VALUES
-- Marko (Head Chef): available Mon-Fri 08:00-22:00
(3, 1, '08:00', '22:00'), (3, 2, '08:00', '22:00'),
(3, 3, '08:00', '22:00'), (3, 4, '08:00', '22:00'),
(3, 5, '08:00', '22:00'),
-- Lena (Sous Chef): available Tue-Sat
(4, 2, '09:00', '22:00'), (4, 3, '09:00', '22:00'),
(4, 4, '09:00', '22:00'), (4, 5, '09:00', '22:00'),
(4, 6, '09:00', '23:00'),
-- Tomislav (Line Cook): available Mon-Sun afternoons
(5, 1, '14:00', '23:00'), (5, 2, '14:00', '23:00'),
(5, 3, '14:00', '23:00'), (5, 4, '14:00', '23:00'),
(5, 5, '14:00', '23:00'), (5, 6, '14:00', '23:00'),
(5, 7, '14:00', '23:00'),
-- Sara (Bartender): available Wed-Sun
(6, 3, '16:00', '02:00'), (6, 4, '16:00', '02:00'),
(6, 5, '16:00', '02:00'), (6, 6, '16:00', '02:00'),
(6, 7, '16:00', '02:00'),
-- Petra (Waiter): Mon-Fri
(7, 1, '10:00', '22:00'), (7, 2, '10:00', '22:00'),
(7, 3, '10:00', '22:00'), (7, 4, '10:00', '22:00'),
(7, 5, '10:00', '22:00'),
-- Nikola (Waiter): Thu-Sun
(8, 4, '12:00', '23:00'), (8, 5, '12:00', '23:00'),
(8, 6, '12:00', '23:00'), (8, 7, '12:00', '23:00'),
-- Maja (Waiter): Mon, Wed, Fri, Sat
(9, 1, '11:00', '22:00'), (9, 3, '11:00', '22:00'),
(9, 5, '11:00', '22:00'), (9, 6, '11:00', '23:00'),
-- Dario (Host+Waiter): Fri-Sun
(10, 5, '17:00', '02:00'), (10, 6, '17:00', '02:00'),
(10, 7, '17:00', '02:00');

-- -----------------------------------------------------------------------------
-- Shifts  (current week: 2026-04-13 Mon to 2026-04-19 Sun)
-- Departments: 1=Kitchen, 2=Bar, 3=Front of House
-- Created by manager Ana (id=1)
-- -----------------------------------------------------------------------------
INSERT INTO shift (department_id, shift_date, start_time, end_time, is_published, created_by) VALUES
-- Monday 13 Apr
(1, '2026-04-13', '09:00', '17:00', 1, 1),  -- id=1  Kitchen Lunch
(3, '2026-04-13', '11:00', '21:00', 1, 1),  -- id=2  FoH Lunch+Dinner
-- Tuesday 14 Apr
(1, '2026-04-14', '09:00', '17:00', 1, 1),  -- id=3  Kitchen Lunch
(3, '2026-04-14', '11:00', '21:00', 1, 1),  -- id=4  FoH Lunch+Dinner
-- Wednesday 15 Apr
(1, '2026-04-15', '09:00', '17:00', 1, 1),  -- id=5  Kitchen Lunch
(2, '2026-04-15', '17:00', '23:00', 1, 1),  -- id=6  Bar Evening
(3, '2026-04-15', '11:00', '23:00', 1, 1),  -- id=7  FoH Full Day
-- Thursday 16 Apr
(1, '2026-04-16', '09:00', '17:00', 1, 1),  -- id=8  Kitchen Lunch
(2, '2026-04-16', '17:00', '23:00', 1, 1),  -- id=9  Bar Evening
(3, '2026-04-16', '12:00', '23:00', 1, 1),  -- id=10 FoH Dinner
-- Friday 17 Apr  (busy night)
(1, '2026-04-17', '10:00', '22:00', 1, 1),  -- id=11 Kitchen Full Day
(2, '2026-04-17', '17:00', '02:00', 1, 1),  -- id=12 Bar Night
(3, '2026-04-17', '12:00', '23:00', 1, 1),  -- id=13 FoH Dinner
-- Saturday 18 Apr
(1, '2026-04-18', '10:00', '22:00', 1, 1),  -- id=14 Kitchen Full Day
(2, '2026-04-18', '17:00', '02:00', 1, 1),  -- id=15 Bar Night
(3, '2026-04-18', '12:00', '23:00', 1, 1),  -- id=16 FoH Dinner
-- Sunday 19 Apr
(1, '2026-04-19', '12:00', '20:00', 1, 1),  -- id=17 Kitchen Lunch Only
(3, '2026-04-19', '12:00', '20:00', 1, 1),  -- id=18 FoH Lunch
-- Next week unpublished draft shifts
(1, '2026-04-20', '09:00', '17:00', 0, 1),  -- id=19 (draft)
(3, '2026-04-20', '11:00', '21:00', 0, 1);  -- id=20 (draft)

-- -----------------------------------------------------------------------------
-- Shift Coverage Requirements
-- role ids: 1=Head Chef, 2=Sous Chef, 3=Line Cook, 4=Bartender, 5=Waiter, 6=Host
-- -----------------------------------------------------------------------------
INSERT INTO shift_coverage_requirement (shift_id, role_id, min_count) VALUES
-- Kitchen shifts need at least 1 Head Chef + 1 cook
(1, 1, 1), (1, 3, 1),
(3, 1, 1), (3, 3, 1),
(5, 1, 1), (5, 3, 1),
(8, 1, 1), (8, 3, 1),
-- Friday/Saturday kitchen: need Head Chef + Sous Chef + Line Cook
(11, 1, 1), (11, 2, 1), (11, 3, 1),
(14, 1, 1), (14, 2, 1), (14, 3, 1),
-- Sunday lighter kitchen
(17, 1, 1),
-- Bar shifts: 1 bartender minimum
(6, 4, 1), (9, 4, 1), (12, 4, 1), (15, 4, 1),
-- FoH shifts: 2 waiters minimum on weekdays, 1 host + 2 waiters on weekends
(2, 5, 2), (4, 5, 2), (7, 5, 2), (10, 5, 2),
(13, 5, 2), (13, 6, 1),
(16, 5, 2), (16, 6, 1),
(18, 5, 1);

-- -----------------------------------------------------------------------------
-- Shift Assignments
-- role ids: 1=Head Chef, 2=Sous Chef, 3=Line Cook, 4=Bartender, 5=Waiter, 6=Host
-- -----------------------------------------------------------------------------
INSERT INTO shift_assignment (shift_id, employee_id, role_id) VALUES
-- Monday Kitchen (shift 1): Marko=Head Chef, Tomislav=Line Cook
(1, 3, 1), (1, 5, 3),
-- Monday FoH (shift 2): Petra=Waiter, Maja=Waiter
(2, 7, 5), (2, 9, 5),
-- Tuesday Kitchen (shift 3): Marko=Head Chef, Tomislav=Line Cook
(3, 3, 1), (3, 5, 3),
-- Tuesday FoH (shift 4): Petra=Waiter, Maja=Waiter (Lena starts Tue)
(4, 7, 5), (4, 9, 5),
-- Wednesday Kitchen (shift 5): Marko=Head Chef, Lena=Sous Chef, Tomislav=Line Cook
(5, 3, 1), (5, 4, 2), (5, 5, 3),
-- Wednesday Bar (shift 6): Sara=Bartender
(6, 6, 4),
-- Wednesday FoH (shift 7): Petra=Waiter, Maja=Waiter
(7, 7, 5), (7, 9, 5),
-- Thursday Kitchen (shift 8): Marko=Head Chef, Lena=Sous Chef
(8, 3, 1), (8, 4, 2),
-- Thursday Bar (shift 9): Sara=Bartender
(9, 6, 4),
-- Thursday FoH (shift 10): Nikola=Waiter, Maja=Waiter
(10, 8, 5), (10, 9, 5),
-- Friday Kitchen (shift 11): Marko=Head Chef, Lena=Sous Chef, Tomislav=Line Cook
(11, 3, 1), (11, 4, 2), (11, 5, 3),
-- Friday Bar (shift 12): Sara=Bartender
(12, 6, 4),
-- Friday FoH (shift 13): Nikola=Waiter, Petra=Waiter, Dario=Host
(13, 8, 5), (13, 7, 5), (13, 10, 6),
-- Saturday Kitchen (shift 14): Marko=Head Chef, Lena=Sous Chef, Tomislav=Line Cook
(14, 3, 1), (14, 4, 2), (14, 5, 3),
-- Saturday Bar (shift 15): Sara=Bartender
(15, 6, 4),
-- Saturday FoH (shift 16): Nikola=Waiter, Maja=Waiter, Dario=Host
(16, 8, 5), (16, 9, 5), (16, 10, 6),
-- Sunday Kitchen (shift 17): Marko=Head Chef
(17, 3, 1),
-- Sunday FoH (shift 18): Nikola=Waiter
(18, 8, 5);

-- -----------------------------------------------------------------------------
-- Swap Requests
-- Petra (id=7, assignment on shift 13 = id 35) wants to swap
-- with Nikola (id=8, assignment on shift 13 = id 34)
-- Let's look at actual assignment IDs:
-- shift 13 assignments inserted as: (13,8,5)=34, (13,7,5)=35, (13,10,6)=36
-- Petra's Friday FoH assignment id=35, Nikola's Thursday FoH assignment id=30
-- -----------------------------------------------------------------------------
INSERT INTO swap_request (requester_assignment_id, target_assignment_id, status, requested_at) VALUES
(25, 18, 'PENDING', '2026-04-09 10:15:00');  -- Petra(shift13,id=25) <-> Nikola(shift10,id=18)

-- -----------------------------------------------------------------------------
-- Time-Off Requests
-- -----------------------------------------------------------------------------
INSERT INTO time_off_request (employee_id, start_date, end_date, reason, status, requested_at) VALUES
-- Tomislav requests next weekend off
(5, '2026-04-25', '2026-04-26', 'Family event out of town', 'PENDING', '2026-04-09 09:00:00'),
-- Lena has an approved day off
(4, '2026-04-22', '2026-04-22', 'Medical appointment', 'APPROVED', '2026-04-07 14:30:00');

-- Update the approved one to include resolution info (manager Ana approved)
UPDATE time_off_request
SET resolved_at = '2026-04-08 09:00:00', resolved_by = 1
WHERE id = 2;

-- -----------------------------------------------------------------------------
-- Notifications
-- -----------------------------------------------------------------------------
INSERT INTO notification (employee_id, message, type, is_read, created_at) VALUES
-- Employees notified when their shifts were published
(3, 'Your shifts for the week of Apr 13 have been published.', 'SCHEDULE_PUBLISHED', 1, '2026-04-08 08:00:00'),
(4, 'Your shifts for the week of Apr 13 have been published.', 'SCHEDULE_PUBLISHED', 1, '2026-04-08 08:00:00'),
(5, 'Your shifts for the week of Apr 13 have been published.', 'SCHEDULE_PUBLISHED', 0, '2026-04-08 08:00:00'),
(6, 'Your shifts for the week of Apr 13 have been published.', 'SCHEDULE_PUBLISHED', 1, '2026-04-08 08:00:00'),
(7, 'Your shifts for the week of Apr 13 have been published.', 'SCHEDULE_PUBLISHED', 0, '2026-04-08 08:00:00'),
(8, 'Your shifts for the week of Apr 13 have been published.', 'SCHEDULE_PUBLISHED', 0, '2026-04-08 08:00:00'),
(9, 'Your shifts for the week of Apr 13 have been published.', 'SCHEDULE_PUBLISHED', 1, '2026-04-08 08:00:00'),
(10, 'Your shifts for the week of Apr 13 have been published.', 'SCHEDULE_PUBLISHED', 0, '2026-04-08 08:00:00'),
-- Lena notified of approved time off
(4, 'Your time-off request for Apr 22 has been approved.', 'TIMEOFF_APPROVED', 0, '2026-04-08 09:00:00'),
-- Manager notified of Tomislav time-off request
(1, 'Tomislav Novak submitted a time-off request for Apr 25-26.', 'TIMEOFF_REQUESTED', 0, '2026-04-09 09:01:00'),
-- Nikola notified of incoming swap request from Petra
(8, 'Petra Blažević has requested a shift swap with you for Apr 17.', 'SWAP_REQUESTED', 0, '2026-04-09 10:15:00'),
-- Manager notified of swap request pending approval
(1, 'A shift swap request between Petra Blažević and Nikola Šimić is pending your approval.', 'SWAP_REQUESTED', 0, '2026-04-09 10:15:00');

-- =============================================================================
-- END OF SCRIPT
-- =============================================================================