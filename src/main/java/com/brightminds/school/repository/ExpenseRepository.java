package com.brightminds.school.repository;

import com.brightminds.school.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    List<Expense> findAllByOrderBySpentOnDesc();
    List<Expense> findBySpentOnBetween(LocalDate from, LocalDate to);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.spentOn >= :from")
    BigDecimal sumFrom(LocalDate from);
}
