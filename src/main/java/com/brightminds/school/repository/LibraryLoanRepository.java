package com.brightminds.school.repository;

import com.brightminds.school.entity.LibraryLoan;
import com.brightminds.school.entity.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LibraryLoanRepository extends JpaRepository<LibraryLoan, UUID> {
    List<LibraryLoan> findByPupilId(UUID pupilId);
    List<LibraryLoan> findByStatus(LoanStatus status);
    List<LibraryLoan> findByBookId(UUID bookId);
}
