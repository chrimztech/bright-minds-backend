package com.brightminds.school.controller;

import com.brightminds.school.entity.LibraryBook;
import com.brightminds.school.entity.LibraryLoan;
import com.brightminds.school.entity.enums.LoanStatus;
import com.brightminds.school.repository.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/library")
@RequiredArgsConstructor
@Tag(name = "Library")
public class LibraryController {

    private final LibraryBookRepository bookRepo;
    private final LibraryLoanRepository loanRepo;
    private final PupilRepository pupilRepo;
    private final StaffRepository staffRepo;

    @PreAuthorize("@perm.has('library:view')")
    @GetMapping("/books")
    public Object listBooks(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        if (q != null && !q.isBlank()) return bookRepo.search(q, PageRequest.of(page, size));
        return bookRepo.findAll();
    }

    @PreAuthorize("@perm.has('library:view')")
    @GetMapping("/books/{id}")
    public LibraryBook getBook(@PathVariable UUID id) {
        return bookRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Book not found"));
    }

    @PreAuthorize("@perm.has('library:manage')")
    @PostMapping("/books")
    @ResponseStatus(HttpStatus.CREATED)
    public LibraryBook createBook(@RequestBody BookRequest req) {
        return bookRepo.save(LibraryBook.builder()
                .title(req.getTitle())
                .author(req.getAuthor())
                .isbn(req.getIsbn())
                .category(req.getCategory())
                .shelf(req.getShelf())
                .copiesTotal(req.getCopiesTotal())
                .copiesAvailable(req.getCopiesTotal())
                .build());
    }

    @PreAuthorize("@perm.has('library:manage')")
    @PutMapping("/books/{id}")
    public LibraryBook updateBook(@PathVariable UUID id, @RequestBody BookRequest req) {
        LibraryBook b = getBook(id);
        b.setTitle(req.getTitle());
        b.setAuthor(req.getAuthor());
        b.setIsbn(req.getIsbn());
        b.setCategory(req.getCategory());
        b.setShelf(req.getShelf());
        if (req.getCopiesTotal() > 0) b.setCopiesTotal(req.getCopiesTotal());
        return bookRepo.save(b);
    }

    @PreAuthorize("@perm.has('library:manage')")
    @DeleteMapping("/books/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(@PathVariable UUID id) { bookRepo.deleteById(id); }

    @PreAuthorize("@perm.has('library:view')")
    @GetMapping("/loans")
    public List<LibraryLoan> listLoans(@RequestParam(required = false) String status) {
        if (status != null) return loanRepo.findByStatus(LoanStatus.valueOf(status.toUpperCase()));
        return loanRepo.findAll();
    }

    @PreAuthorize("@perm.has('library:manage')")
    @PostMapping("/loans")
    @ResponseStatus(HttpStatus.CREATED)
    public LibraryLoan createLoan(@RequestBody LoanRequest req) {
        LibraryBook book = bookRepo.findById(req.getBookId())
                .orElseThrow(() -> new EntityNotFoundException("Book not found"));
        if (book.getCopiesAvailable() < 1) throw new IllegalArgumentException("No copies available");
        book.setCopiesAvailable(book.getCopiesAvailable() - 1);
        bookRepo.save(book);

        LibraryLoan loan = LibraryLoan.builder()
                .book(book)
                .dueOn(req.getDueOn())
                .build();
        if (req.getPupilId() != null) pupilRepo.findById(req.getPupilId()).ifPresent(loan::setPupil);
        if (req.getStaffId() != null) staffRepo.findById(req.getStaffId()).ifPresent(loan::setStaff);
        return loanRepo.save(loan);
    }

    @PreAuthorize("@perm.has('library:manage')")
    @PatchMapping("/loans/{id}/return")
    public LibraryLoan returnBook(@PathVariable UUID id) {
        LibraryLoan loan = loanRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Loan not found"));
        loan.setReturnedOn(LocalDate.now());
        loan.setStatus(LoanStatus.RETURNED);
        loan.getBook().setCopiesAvailable(loan.getBook().getCopiesAvailable() + 1);
        bookRepo.save(loan.getBook());
        return loanRepo.save(loan);
    }

    @Data public static class BookRequest {
        private String title;
        private String author;
        private String isbn;
        private String category;
        private String shelf;
        private int copiesTotal = 1;
    }

    @Data public static class LoanRequest {
        private UUID bookId;
        private UUID pupilId;
        private UUID staffId;
        private LocalDate dueOn;
    }
}
