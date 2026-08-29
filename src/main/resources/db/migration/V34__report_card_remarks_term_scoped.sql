-- Report cards move from "one per exam" to "one per term" (a term can now contain up to four
-- assessments — Test 1, Test 2, Mid Term, End of Term — pivoted into one card). Remarks need to
-- follow: one class-teacher/head-teacher remark per (pupil, term), not per (pupil, exam).

-- 1. Add nullable term_id, backfilled from each remark's exam's term.
ALTER TABLE report_card_remarks ADD COLUMN term_id UUID REFERENCES terms(id) ON DELETE CASCADE;

UPDATE report_card_remarks r
SET term_id = e.term_id
FROM exams e
WHERE r.exam_id = e.id;

-- 2. Collapse rows that now collide on (pupil_id, term_id) — e.g. a school that had already
--    saved separate remarks for the Mid Term and End of Term exams within the same term. Keep
--    the most-recently-updated non-null value per field, written onto the most-recently-updated
--    row for that (pupil_id, term_id) pair.
WITH ranked AS (
    SELECT id, pupil_id, term_id, class_teacher_remark, head_teacher_remark, updated_at,
           ROW_NUMBER() OVER (PARTITION BY pupil_id, term_id ORDER BY updated_at DESC) AS rn
    FROM report_card_remarks
    WHERE term_id IS NOT NULL
),
winners AS (
    SELECT pupil_id, term_id, id AS keep_id FROM ranked WHERE rn = 1
),
merged_class AS (
    SELECT DISTINCT ON (pupil_id, term_id) pupil_id, term_id, class_teacher_remark
    FROM ranked WHERE class_teacher_remark IS NOT NULL
    ORDER BY pupil_id, term_id, updated_at DESC
),
merged_head AS (
    SELECT DISTINCT ON (pupil_id, term_id) pupil_id, term_id, head_teacher_remark
    FROM ranked WHERE head_teacher_remark IS NOT NULL
    ORDER BY pupil_id, term_id, updated_at DESC
)
UPDATE report_card_remarks r
SET class_teacher_remark = mc.class_teacher_remark,
    head_teacher_remark  = mh.head_teacher_remark
FROM winners w
LEFT JOIN merged_class mc ON mc.pupil_id = w.pupil_id AND mc.term_id = w.term_id
LEFT JOIN merged_head  mh ON mh.pupil_id = w.pupil_id AND mh.term_id = w.term_id
WHERE r.id = w.keep_id;

-- 3. Drop the losing duplicate rows.
DELETE FROM report_card_remarks r
USING (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY pupil_id, term_id ORDER BY updated_at DESC) AS rn
    FROM report_card_remarks WHERE term_id IS NOT NULL
) ranked
WHERE r.id = ranked.id AND ranked.rn > 1;

-- 4. Remarks whose exam had no term at all can't be mapped to the new key — expected to be 0
--    rows in practice since exams are always created with a term in this app's UI.
DELETE FROM report_card_remarks WHERE term_id IS NULL;

-- 5. Swap the key.
DROP INDEX IF EXISTS report_card_remarks_pupil_exam_idx;
ALTER TABLE report_card_remarks DROP COLUMN exam_id;
ALTER TABLE report_card_remarks ALTER COLUMN term_id SET NOT NULL;
CREATE UNIQUE INDEX report_card_remarks_pupil_term_idx ON report_card_remarks(pupil_id, term_id);
