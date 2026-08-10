-- Link guardian profiles to login accounts and prevent duplicate child links.
UPDATE guardians g
SET user_id = NULL
WHERE user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM app_users u WHERE u.id = g.user_id);

ALTER TABLE guardians
    ADD CONSTRAINT guardians_user_id_fk
    FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE SET NULL;

CREATE UNIQUE INDEX guardians_user_id_uidx
    ON guardians(user_id) WHERE user_id IS NOT NULL;

DELETE FROM guardian_pupils a
USING guardian_pupils b
WHERE a.id > b.id
  AND a.guardian_id = b.guardian_id
  AND a.pupil_id = b.pupil_id;

CREATE UNIQUE INDEX guardian_pupils_guardian_pupil_uidx
    ON guardian_pupils(guardian_id, pupil_id);

-- Enrollment history preserves the grade, class stream, and class teacher that
-- applied during an academic year, even after a pupil is promoted.
CREATE TABLE pupil_enrollments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pupil_id UUID NOT NULL REFERENCES pupils(id) ON DELETE CASCADE,
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE RESTRICT,
    academic_year_id UUID REFERENCES academic_years(id) ON DELETE SET NULL,
    started_on DATE NOT NULL,
    ended_on DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pupil_enrollment_dates_chk CHECK (ended_on IS NULL OR ended_on >= started_on)
);

CREATE INDEX pupil_enrollments_pupil_idx ON pupil_enrollments(pupil_id);
CREATE INDEX pupil_enrollments_year_idx ON pupil_enrollments(academic_year_id);
CREATE UNIQUE INDEX pupil_enrollments_active_uidx
    ON pupil_enrollments(pupil_id) WHERE ended_on IS NULL;

INSERT INTO pupil_enrollments (pupil_id, class_id, academic_year_id, started_on)
SELECT p.id,
       p.class_id,
       ay.id,
       CASE
           WHEN ay.start_date IS NOT NULL THEN GREATEST(p.admitted_on, ay.start_date)
           ELSE p.admitted_on
       END
FROM pupils p
LEFT JOIN LATERAL (
    SELECT id, start_date
    FROM academic_years
    ORDER BY is_current DESC, start_date DESC
    LIMIT 1
) ay ON TRUE
WHERE p.class_id IS NOT NULL;

CREATE TABLE pupil_promotions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pupil_id UUID NOT NULL REFERENCES pupils(id) ON DELETE CASCADE,
    from_class_id UUID NOT NULL REFERENCES classes(id) ON DELETE RESTRICT,
    to_class_id UUID NOT NULL REFERENCES classes(id) ON DELETE RESTRICT,
    academic_year_id UUID REFERENCES academic_years(id) ON DELETE SET NULL,
    promoted_on DATE NOT NULL,
    promoted_by UUID REFERENCES app_users(id) ON DELETE SET NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pupil_promotion_class_chk CHECK (from_class_id <> to_class_id)
);

CREATE INDEX pupil_promotions_pupil_idx
    ON pupil_promotions(pupil_id, promoted_on DESC);
