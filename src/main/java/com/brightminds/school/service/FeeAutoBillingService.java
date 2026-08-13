package com.brightminds.school.service;

import com.brightminds.school.entity.FeeItem;
import com.brightminds.school.entity.Invoice;
import com.brightminds.school.entity.Pupil;
import com.brightminds.school.repository.FeeItemRepository;
import com.brightminds.school.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

// Invoices a pupil for their class's recurring fee items whenever they're assigned to that
// class — at registration, on edit, or via the class roster — skipping any fee item they've
// already been billed for (tracked via Invoice.feeItem).
@Service
@RequiredArgsConstructor
public class FeeAutoBillingService {

    private final FeeItemRepository feeItemRepo;
    private final InvoiceRepository invoiceRepo;

    @Transactional
    public void billRecurringFeesForClass(Pupil pupil) {
        if (pupil.getSchoolClass() == null) return;
        List<FeeItem> recurring = feeItemRepo.findRecurringForClass(pupil.getSchoolClass().getId());
        for (FeeItem item : recurring) {
            if (invoiceRepo.existsByPupilIdAndFeeItemId(pupil.getId(), item.getId())) continue;
            Invoice invoice = Invoice.builder()
                    .invoiceNo(generateInvoiceNo())
                    .pupil(pupil)
                    .term(item.getTerm())
                    .feeItem(item)
                    .total(item.getAmount())
                    .description(item.getName())
                    .build();
            invoiceRepo.save(invoice);
        }
    }

    private String generateInvoiceNo() {
        return "INV-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
