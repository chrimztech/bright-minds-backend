package com.brightminds.school.service;

import com.brightminds.school.entity.Invoice;
import com.brightminds.school.entity.enums.InvoiceStatus;
import com.brightminds.school.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class InvoiceBalanceService {

    private final InvoiceRepository invoiceRepo;

    // Applies a signed delta to an invoice's paid total — positive to record a payment,
    // negative to reverse one (editing or deleting a previously-confirmed payment) — and
    // recomputes status from the resulting balance rather than assuming it only grows.
    // Shared by FeeController's manual payment flows and LencoService's gateway-confirmed
    // ones, so both update an invoice's balance/status the exact same way.
    public void adjust(Invoice invoice, BigDecimal delta) {
        BigDecimal newPaid = invoice.getPaid().add(delta).max(BigDecimal.ZERO);
        invoice.setPaid(newPaid);
        InvoiceStatus status;
        if (newPaid.signum() <= 0) status = InvoiceStatus.UNPAID;
        else if (newPaid.compareTo(invoice.getTotal()) >= 0) status = InvoiceStatus.PAID;
        else status = InvoiceStatus.PARTIAL;
        invoice.setStatus(status);
        invoiceRepo.save(invoice);
    }
}
