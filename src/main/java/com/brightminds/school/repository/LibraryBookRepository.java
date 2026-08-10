package com.brightminds.school.repository;

import com.brightminds.school.entity.LibraryBook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.UUID;

public interface LibraryBookRepository extends JpaRepository<LibraryBook, UUID> {
    @Query("SELECT b FROM LibraryBook b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(b.author) LIKE LOWER(CONCAT('%',:q,'%'))")
    Page<LibraryBook> search(String q, Pageable pageable);
}
