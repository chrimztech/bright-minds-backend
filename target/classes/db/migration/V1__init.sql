-- ============================================================
-- Bright Minds School Hub - Database Schema
-- ============================================================

-- Enums
CREATE TYPE app_role AS ENUM ('SUPER_ADMIN','HEAD_TEACHER','DEPUTY_HEAD','ADMIN','ACCOUNTANT','TEACHER','CLASS_TEACHER','PARENT','LIBRARIAN','STORE_OFFICER','TRANSPORT_OFFICER','NURSE','SECURITY');
CREATE TYPE admission_status AS ENUM ('APPLIED','REVIEWING','INTERVIEWED','ADMITTED','REJECTED','WAITLISTED','ENROLLED');
CREATE TYPE attendance_status AS ENUM ('PRESENT','ABSENT','LATE','SICK','EXCUSED');
CREATE TYPE discipline_kind AS ENUM ('INCIDENT','MERIT','DEMERIT','REWARD','WARNING');
CREATE TYPE event_audience AS ENUM ('ALL','STAFF','PARENTS','PUPILS','CLASS');
CREATE TYPE inv_direction AS ENUM ('IN','OUT','ADJUST','DAMAGE','LOST');
CREATE TYPE invoice_status AS ENUM ('UNPAID','PARTIAL','PAID','CANCELLED');
CREATE TYPE leave_status AS ENUM ('PENDING','APPROVED','REJECTED','CANCELLED');
CREATE TYPE loan_status AS ENUM ('BORROWED','RETURNED','OVERDUE','LOST');
CREATE TYPE payment_method AS ENUM ('CASH','BANK','MOBILE_MONEY','CARD','CHEQUE','ONLINE');
CREATE TYPE payroll_status AS ENUM ('DRAFT','APPROVED','PAID');
CREATE TYPE po_status AS ENUM ('DRAFT','SENT','RECEIVED','CANCELLED','PAID');
CREATE TYPE pupil_status AS ENUM ('ACTIVE','TRANSFERRED','SUSPENDED','GRADUATED','WITHDRAWN');
CREATE TYPE staff_status AS ENUM ('ACTIVE','RESIGNED','TERMINATED','RETIRED','SUSPENDED');

-- Users
CREATE TABLE app_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL DEFAULT '',
    phone VARCHAR(50),
    avatar_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE user_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    role app_role NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Academic structure
CREATE TABLE academic_years (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_current BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE terms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    academic_year_id UUID NOT NULL REFERENCES academic_years(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_current BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE subjects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    code VARCHAR(20) UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Staff
CREATE TABLE staff (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    staff_no VARCHAR(50) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    gender VARCHAR(20),
    email VARCHAR(255),
    phone VARCHAR(50),
    address TEXT,
    dob DATE,
    date_joined DATE,
    employment_type VARCHAR(50),
    is_teacher BOOLEAN NOT NULL DEFAULT FALSE,
    basic_salary NUMERIC(12,2),
    qualifications TEXT,
    next_of_kin TEXT,
    bank_name VARCHAR(100),
    bank_account VARCHAR(100),
    photo_url TEXT,
    status staff_status NOT NULL DEFAULT 'ACTIVE',
    user_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Classes
CREATE TABLE classes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    stream VARCHAR(50),
    level_order INT NOT NULL DEFAULT 1,
    capacity INT,
    class_teacher_id UUID REFERENCES staff(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE teacher_subjects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_id UUID NOT NULL REFERENCES staff(id) ON DELETE CASCADE,
    subject_id UUID NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    class_id UUID REFERENCES classes(id) ON DELETE CASCADE
);

CREATE TABLE timetable_slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    subject_id UUID REFERENCES subjects(id) ON DELETE SET NULL,
    teacher_id UUID REFERENCES staff(id) ON DELETE SET NULL,
    day_of_week INT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    room VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Pupils
CREATE TABLE pupils (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admission_no VARCHAR(50) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    gender VARCHAR(20),
    dob DATE,
    address TEXT,
    town VARCHAR(100),
    province VARCHAR(100),
    postal_code VARCHAR(20),
    nationality VARCHAR(100),
    tribe VARCHAR(100),
    religion VARCHAR(100),
    home_language VARCHAR(100),
    blood_group VARCHAR(10),
    allergies TEXT,
    medical_info TEXT,
    special_needs TEXT,
    nrc_no VARCHAR(50),
    birth_cert_no VARCHAR(50),
    previous_school VARCHAR(255),
    referral_source VARCHAR(100),
    siblings_in_school TEXT,
    house VARCHAR(50),
    boarding_status VARCHAR(50),
    transport_mode VARCHAR(50),
    emergency_contact TEXT,
    photo_url TEXT,
    notes TEXT,
    admitted_on DATE NOT NULL DEFAULT CURRENT_DATE,
    status pupil_status NOT NULL DEFAULT 'ACTIVE',
    class_id UUID REFERENCES classes(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE guardians (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(255) NOT NULL,
    relationship VARCHAR(50),
    phone VARCHAR(50),
    email VARCHAR(255),
    address TEXT,
    occupation VARCHAR(100),
    national_id VARCHAR(50),
    user_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE guardian_pupils (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    guardian_id UUID NOT NULL REFERENCES guardians(id) ON DELETE CASCADE,
    pupil_id UUID NOT NULL REFERENCES pupils(id) ON DELETE CASCADE,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE
);

-- Attendance
CREATE TABLE attendance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pupil_id UUID NOT NULL REFERENCES pupils(id) ON DELETE CASCADE,
    class_id UUID REFERENCES classes(id) ON DELETE SET NULL,
    date DATE NOT NULL,
    status attendance_status NOT NULL DEFAULT 'PRESENT',
    notes TEXT,
    recorded_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX attendance_pupil_date_idx ON attendance(pupil_id, date);

-- Admissions
CREATE TABLE admissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_no VARCHAR(50) UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    gender VARCHAR(20),
    dob DATE,
    previous_school VARCHAR(255),
    parent_name VARCHAR(255),
    parent_phone VARCHAR(50),
    parent_email VARCHAR(255),
    interview_date DATE,
    reg_fee_paid NUMERIC(10,2),
    status admission_status NOT NULL DEFAULT 'APPLIED',
    target_class_id UUID REFERENCES classes(id) ON DELETE SET NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Exams & Marks
CREATE TABLE exams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    term_id UUID REFERENCES terms(id) ON DELETE SET NULL,
    exam_date DATE,
    out_of INT NOT NULL DEFAULT 100,
    weight DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE marks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pupil_id UUID NOT NULL REFERENCES pupils(id) ON DELETE CASCADE,
    exam_id UUID NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    subject_id UUID NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    score DOUBLE PRECISION NOT NULL,
    comment TEXT,
    entered_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(pupil_id, exam_id, subject_id)
);

-- Fees
CREATE TABLE fee_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    class_id UUID REFERENCES classes(id) ON DELETE SET NULL,
    term_id UUID REFERENCES terms(id) ON DELETE SET NULL,
    is_recurring BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_no VARCHAR(50) NOT NULL UNIQUE,
    pupil_id UUID NOT NULL REFERENCES pupils(id) ON DELETE CASCADE,
    term_id UUID REFERENCES terms(id) ON DELETE SET NULL,
    total NUMERIC(12,2) NOT NULL DEFAULT 0,
    paid NUMERIC(12,2) NOT NULL DEFAULT 0,
    status invoice_status NOT NULL DEFAULT 'UNPAID',
    due_date DATE,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    receipt_no VARCHAR(50) NOT NULL UNIQUE,
    pupil_id UUID NOT NULL REFERENCES pupils(id) ON DELETE CASCADE,
    invoice_id UUID REFERENCES invoices(id) ON DELETE SET NULL,
    amount NUMERIC(12,2) NOT NULL,
    method payment_method NOT NULL DEFAULT 'CASH',
    paid_on DATE NOT NULL DEFAULT CURRENT_DATE,
    reference VARCHAR(100),
    notes TEXT,
    recorded_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Announcements & Events
CREATE TABLE announcements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    audience VARCHAR(50) NOT NULL DEFAULT 'all',
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ,
    location VARCHAR(255),
    audience event_audience NOT NULL DEFAULT 'ALL',
    class_id UUID REFERENCES classes(id) ON DELETE SET NULL,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Homework
CREATE TABLE homework (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    class_id UUID REFERENCES classes(id) ON DELETE SET NULL,
    subject_id UUID REFERENCES subjects(id) ON DELETE SET NULL,
    due_date DATE,
    attachment_url TEXT,
    assigned_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Health
CREATE TABLE health_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pupil_id UUID NOT NULL REFERENCES pupils(id) ON DELETE CASCADE,
    visit_date DATE NOT NULL DEFAULT CURRENT_DATE,
    complaint TEXT,
    diagnosis TEXT,
    treatment TEXT,
    medication TEXT,
    notes TEXT,
    attended_by VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Discipline
CREATE TABLE discipline_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pupil_id UUID NOT NULL REFERENCES pupils(id) ON DELETE CASCADE,
    kind discipline_kind NOT NULL DEFAULT 'INCIDENT',
    description TEXT NOT NULL,
    action_taken TEXT,
    occurred_on DATE NOT NULL DEFAULT CURRENT_DATE,
    points INT,
    reported_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Library
CREATE TABLE library_books (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255),
    isbn VARCHAR(20),
    category VARCHAR(100),
    shelf VARCHAR(50),
    copies_total INT NOT NULL DEFAULT 1,
    copies_available INT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE library_loans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    book_id UUID NOT NULL REFERENCES library_books(id) ON DELETE CASCADE,
    pupil_id UUID REFERENCES pupils(id) ON DELETE SET NULL,
    staff_id UUID REFERENCES staff(id) ON DELETE SET NULL,
    borrowed_on DATE NOT NULL DEFAULT CURRENT_DATE,
    due_on DATE NOT NULL,
    returned_on DATE,
    status loan_status NOT NULL DEFAULT 'BORROWED',
    fine NUMERIC(10,2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Inventory
CREATE TABLE suppliers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    contact_person VARCHAR(255),
    phone VARCHAR(50),
    email VARCHAR(255),
    address TEXT,
    tax_no VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE inventory_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    sku VARCHAR(50),
    category VARCHAR(100),
    unit VARCHAR(50),
    location VARCHAR(100),
    quantity INT NOT NULL DEFAULT 0,
    reorder_level INT,
    unit_cost NUMERIC(12,2),
    supplier_id UUID REFERENCES suppliers(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE inventory_txns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id UUID NOT NULL REFERENCES inventory_items(id) ON DELETE CASCADE,
    direction inv_direction NOT NULL,
    quantity INT NOT NULL,
    reason TEXT,
    reference VARCHAR(100),
    occurred_on DATE NOT NULL DEFAULT CURRENT_DATE,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Transport
CREATE TABLE vehicles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reg_no VARCHAR(50) NOT NULL UNIQUE,
    model VARCHAR(100),
    capacity INT,
    driver_name VARCHAR(255),
    driver_phone VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE transport_routes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    pickup_points TEXT,
    vehicle_id UUID REFERENCES vehicles(id) ON DELETE SET NULL,
    fee NUMERIC(10,2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE transport_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pupil_id UUID NOT NULL REFERENCES pupils(id) ON DELETE CASCADE,
    route_id UUID NOT NULL REFERENCES transport_routes(id) ON DELETE CASCADE,
    pickup_point VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Canteen
CREATE TABLE canteen_meal_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    description TEXT,
    meals_per_day INT NOT NULL DEFAULT 1,
    price_per_term NUMERIC(10,2) NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE canteen_menu_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    category VARCHAR(50) NOT NULL DEFAULT 'general',
    description TEXT,
    price NUMERIC(10,2) NOT NULL DEFAULT 0,
    image_url TEXT,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE canteen_sales (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id UUID REFERENCES canteen_menu_items(id) ON DELETE SET NULL,
    item_name VARCHAR(150) NOT NULL,
    pupil_id UUID REFERENCES pupils(id) ON DELETE SET NULL,
    staff_id UUID REFERENCES staff(id) ON DELETE SET NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price NUMERIC(10,2) NOT NULL DEFAULT 0,
    total NUMERIC(10,2) NOT NULL DEFAULT 0,
    payment_method VARCHAR(50) NOT NULL DEFAULT 'cash',
    served_on DATE NOT NULL DEFAULT CURRENT_DATE,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE canteen_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pupil_id UUID NOT NULL REFERENCES pupils(id) ON DELETE CASCADE,
    plan_id UUID NOT NULL REFERENCES canteen_meal_plans(id) ON DELETE CASCADE,
    term_id UUID REFERENCES terms(id) ON DELETE SET NULL,
    start_date DATE NOT NULL DEFAULT CURRENT_DATE,
    end_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Leave
CREATE TABLE leave_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    staff_id UUID NOT NULL REFERENCES staff(id) ON DELETE CASCADE,
    leave_type VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason TEXT,
    status leave_status NOT NULL DEFAULT 'PENDING',
    approver_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Payroll
CREATE TABLE payroll_periods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    period_label VARCHAR(100) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    status payroll_status NOT NULL DEFAULT 'DRAFT',
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE payslips (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    staff_id UUID NOT NULL REFERENCES staff(id) ON DELETE CASCADE,
    period_id UUID NOT NULL REFERENCES payroll_periods(id) ON DELETE CASCADE,
    basic NUMERIC(12,2) NOT NULL DEFAULT 0,
    allowances NUMERIC(12,2) NOT NULL DEFAULT 0,
    deductions NUMERIC(12,2) NOT NULL DEFAULT 0,
    tax NUMERIC(12,2) NOT NULL DEFAULT 0,
    net_pay NUMERIC(12,2) NOT NULL DEFAULT 0,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(staff_id, period_id)
);

-- Procurement
CREATE TABLE purchase_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    po_no VARCHAR(50) UNIQUE,
    supplier_id UUID REFERENCES suppliers(id) ON DELETE SET NULL,
    order_date DATE NOT NULL DEFAULT CURRENT_DATE,
    status po_status NOT NULL DEFAULT 'DRAFT',
    total NUMERIC(12,2) NOT NULL DEFAULT 0,
    notes TEXT,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Expenses (Accounts)
CREATE TABLE expenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category VARCHAR(100) NOT NULL,
    amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    payee VARCHAR(255),
    description TEXT,
    payment_method VARCHAR(50),
    ref_no VARCHAR(100),
    spent_on DATE NOT NULL DEFAULT CURRENT_DATE,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Communication
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    body TEXT NOT NULL,
    subject VARCHAR(255),
    channel VARCHAR(20) NOT NULL DEFAULT 'sms',
    audience VARCHAR(50),
    class_id UUID REFERENCES classes(id) ON DELETE SET NULL,
    recipient_count INT,
    sent_by UUID,
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Documents
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    url TEXT NOT NULL,
    category VARCHAR(100),
    description TEXT,
    pupil_id UUID REFERENCES pupils(id) ON DELETE CASCADE,
    staff_id UUID REFERENCES staff(id) ON DELETE CASCADE,
    uploaded_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Audit Logs
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    action VARCHAR(100) NOT NULL,
    entity VARCHAR(100),
    entity_id VARCHAR(100),
    details JSONB,
    user_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- School Settings
CREATE TABLE school_settings (
    id BIGINT PRIMARY KEY DEFAULT 1,
    name VARCHAR(255) NOT NULL DEFAULT 'Bright Minds School',
    address TEXT,
    city VARCHAR(100),
    district VARCHAR(100),
    province VARCHAR(100),
    country VARCHAR(100),
    po_box VARCHAR(50),
    postal_code VARCHAR(20),
    plot_number VARCHAR(50),
    phone VARCHAR(50),
    email VARCHAR(255),
    website VARCHAR(255),
    motto TEXT,
    head_teacher VARCHAR(255),
    deputy_head VARCHAR(255),
    registration_no VARCHAR(100),
    tpin VARCHAR(50),
    established_year INT,
    currency VARCHAR(10) NOT NULL DEFAULT 'ZMW',
    logo_url TEXT,
    map_url TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    grading_scale JSONB NOT NULL DEFAULT '[]',
    current_academic_year_id UUID,
    current_term_id UUID,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Seed default settings row
INSERT INTO school_settings (id, name, motto, currency) VALUES (1, 'Chaz Crestview Academy', 'For Premium Education', 'ZMW') ON CONFLICT DO NOTHING;
