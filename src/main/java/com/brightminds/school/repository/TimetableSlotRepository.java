package com.brightminds.school.repository;

import com.brightminds.school.entity.TimetableSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TimetableSlotRepository extends JpaRepository<TimetableSlot, UUID> {
    List<TimetableSlot> findBySchoolClassIdOrderByDayOfWeekAscStartTimeAsc(UUID classId);
    List<TimetableSlot> findByTeacherId(UUID teacherId);
}
