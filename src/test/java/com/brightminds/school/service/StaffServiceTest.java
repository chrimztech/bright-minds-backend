package com.brightminds.school.service;

import com.brightminds.school.dto.StaffRequest;
import com.brightminds.school.entity.Staff;
import com.brightminds.school.repository.StaffRepository;
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
class StaffServiceTest {

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private StaffService staffService;

    @Test
    void createsStaffNumberWhenRequestLeavesItBlank() {
        StaffRequest request = new StaffRequest();
        request.setFullName("John Doe");
        request.setStaffNo(null);

        when(staffRepository.existsByStaffNo(anyString())).thenReturn(false);
        when(staffRepository.save(any(Staff.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Staff saved = staffService.create(request);

        assertThat(saved.getStaffNo()).matches("STF-\\d{4}-[0-9A-F]{8}");
        assertThat(request.getStaffNo()).isEqualTo(saved.getStaffNo());
    }
}
