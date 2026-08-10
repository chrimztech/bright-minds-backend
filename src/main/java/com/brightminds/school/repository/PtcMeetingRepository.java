package com.brightminds.school.repository;

import com.brightminds.school.entity.PtcMeeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PtcMeetingRepository extends JpaRepository<PtcMeeting, UUID> {
    List<PtcMeeting> findBySessionIdOrderByMeetingDateAscStartTimeAsc(UUID sessionId);
    List<PtcMeeting> findByStaffIdOrderByMeetingDateAscStartTimeAsc(UUID staffId);
    List<PtcMeeting> findByGuardianIdOrderByMeetingDateAscStartTimeAsc(UUID guardianId);
    List<PtcMeeting> findAllByOrderByMeetingDateDescStartTimeAsc();
}
