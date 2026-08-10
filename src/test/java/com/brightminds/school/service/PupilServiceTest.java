package com.brightminds.school.service;

import com.brightminds.school.dto.PupilRequest;
import com.brightminds.school.entity.Pupil;
import com.brightminds.school.repository.AcademicYearRepository;
import com.brightminds.school.repository.PupilEnrollmentRepository;
import com.brightminds.school.repository.PupilRepository;
import com.brightminds.school.repository.SchoolClassRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PupilServiceTest {

    @Mock
    private PupilRepository pupilRepository;
    @Mock
    private SchoolClassRepository schoolClassRepository;
    @Mock
    private AcademicYearRepository academicYearRepository;
    @Mock
    private PupilEnrollmentRepository pupilEnrollmentRepository;

    @InjectMocks
    private PupilService pupilService;

    @Test
    void createsAdmissionNumberWhenRequestLeavesItBlank() {
        PupilRequest request = new PupilRequest();
        request.setFullName("Jane Doe");
        request.setAdmissionNo("   ");

        when(pupilRepository.existsByAdmissionNo(anyString())).thenReturn(false);
        when(pupilRepository.save(any(Pupil.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pupil saved = pupilService.create(request);

        assertThat(saved.getAdmissionNo()).matches("PUP-\\d{4}-[0-9A-F]{8}");
        assertThat(request.getAdmissionNo()).isEqualTo(saved.getAdmissionNo());
    }
}
