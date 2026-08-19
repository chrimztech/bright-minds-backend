package com.brightminds.school.service;

import com.brightminds.school.entity.FeeItem;
import com.brightminds.school.entity.Invoice;
import com.brightminds.school.entity.Pupil;
import com.brightminds.school.entity.Term;
import com.brightminds.school.entity.TransportAssignment;
import com.brightminds.school.repository.FeeItemRepository;
import com.brightminds.school.repository.InvoiceRepository;
import com.brightminds.school.repository.TermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final TermRepository termRepo;

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
                    .dueDate(item.getDueDate())
                    .build();
            invoiceRepo.save(invoice);
        }
    }

    // Bills a pupil for their assigned transport pickup point's fee (falling back to the
    // route's flat fee if no specific point was chosen), same skip-if-already-billed pattern
    // as billRecurringFeesForClass — one FeeItem per route+point (created on first use) ties
    // the invoice back to Invoice.feeItem so re-assigning to the same point never double-bills
    // within a term, while a different-priced point on the same route bills separately.
    @Transactional
    public void billTransportFee(TransportAssignment assignment) {
        var route = assignment.getRoute();
        var point = assignment.getPickupPointRef();
        BigDecimal fee = point != null ? point.getFee() : route.getFee();
        if (fee == null || fee.signum() <= 0) return;

        String itemName = point != null ? route.getName() + " — " + point.getName() : route.getName();
        Term currentTerm = termRepo.findByIsCurrentTrue().orElse(null);

        FeeItem feeItem = (currentTerm != null
                ? feeItemRepo.findByCategoryAndNameAndTerm_Id("TRANSPORT", itemName, currentTerm.getId())
                : feeItemRepo.findByCategoryAndNameAndTermIsNull("TRANSPORT", itemName))
                .orElseGet(() -> feeItemRepo.save(FeeItem.builder()
                        .name(itemName)
                        .category("TRANSPORT")
                        .amount(fee)
                        .isRecurring(true)
                        .term(currentTerm)
                        .build()));

        Pupil pupil = assignment.getPupil();
        if (invoiceRepo.existsByPupilIdAndFeeItemId(pupil.getId(), feeItem.getId())) return;
        invoiceRepo.save(Invoice.builder()
                .invoiceNo(generateInvoiceNo())
                .pupil(pupil)
                .term(currentTerm)
                .feeItem(feeItem)
                .total(feeItem.getAmount())
                .description("Transport — " + itemName)
                .build());
    }

    private String generateInvoiceNo() {
        return "INV-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
