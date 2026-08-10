-- PTC session (the event block, e.g. "Term 1 PTC – March 2026")
CREATE TABLE ptc_sessions (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title        VARCHAR(255) NOT NULL,
    session_date DATE NOT NULL,
    venue        VARCHAR(255),
    description  TEXT,
    start_time   TIME,
    end_time     TIME,
    term_id      UUID REFERENCES terms(id) ON DELETE SET NULL,
    created_at   TIMESTAMP DEFAULT NOW()
);

-- Individual meeting slot within (or outside) a session
CREATE TABLE ptc_meetings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id      UUID REFERENCES ptc_sessions(id) ON DELETE CASCADE,
    staff_id        UUID REFERENCES staff(id)    ON DELETE CASCADE,
    guardian_id     UUID REFERENCES guardians(id) ON DELETE CASCADE,
    pupil_id        UUID REFERENCES pupils(id)   ON DELETE SET NULL,
    meeting_date    DATE NOT NULL,
    start_time      TIME NOT NULL,
    end_time        TIME,
    venue           VARCHAR(255),
    status          VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    agenda          TEXT,
    teacher_notes   TEXT,
    admin_notes     TEXT,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);
