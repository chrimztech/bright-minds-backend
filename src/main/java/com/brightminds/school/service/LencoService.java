package com.brightminds.school.service;

import com.brightminds.school.entity.GatewayTransaction;
import com.brightminds.school.entity.Guardian;
import com.brightminds.school.entity.Invoice;
import com.brightminds.school.entity.Payment;
import com.brightminds.school.entity.enums.GatewayTransactionStatus;
import com.brightminds.school.entity.enums.PaymentMethod;
import com.brightminds.school.entity.enums.PaymentStatus;
import com.brightminds.school.repository.GatewayTransactionRepository;
import com.brightminds.school.repository.PaymentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Lenco mobile money collections (https://lenco-api.readme.io/v2.0/reference). Deliberately
// treats GET /collections/status/:reference as the single source of truth for whether a
// collection actually succeeded — both the polling path (checkStatus, called repeatedly by the
// parent portal) and the webhook path (which only extracts a reference and re-triggers
// checkStatus, never trusting the webhook payload's own claimed status) funnel through the same
// method, so there is exactly one place a payment is ever created from a gateway result.
@Service
@RequiredArgsConstructor
@Slf4j
public class LencoService {

    private static final Set<String> ZAMBIA_OPERATORS = Set.of("airtel", "mtn", "zamtel");

    private final GatewayTransactionRepository gatewayRepo;
    private final PaymentRepository paymentRepo;
    private final InvoiceBalanceService invoiceBalanceService;
    private final AuditService audit;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.lenco.base-url}")
    private String baseUrl;

    @Value("${app.lenco.secret-key}")
    private String secretKey;

    @Value("${app.lenco.signature-key}")
    private String signatureKey;

    public GatewayTransaction initiate(Invoice invoice, Guardian guardian, BigDecimal amount, String phone, String operator) {
        String normalizedOperator = operator == null ? "" : operator.toLowerCase().trim();
        if (!ZAMBIA_OPERATORS.contains(normalizedOperator)) {
            throw new IllegalArgumentException("Operator must be one of: airtel, mtn, zamtel");
        }
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone number is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        String reference = "SCH-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", amount.toPlainString());
        body.put("reference", reference);
        body.put("phone", phone);
        body.put("operator", normalizedOperator);
        body.put("country", "zm");
        body.put("bearer", "merchant");

        JsonNode data;
        String rawResponse;
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/collections/mobile-money", HttpMethod.POST,
                    new HttpEntity<>(body, authHeaders()), String.class);
            rawResponse = response.getBody();
            data = objectMapper.readTree(rawResponse).path("data");
        } catch (HttpStatusCodeException e) {
            log.warn("Lenco collection initiation failed: {}", e.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Could not start the mobile money payment — please try again shortly.");
        } catch (Exception e) {
            log.error("Lenco collection initiation error", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Could not start the mobile money payment — please try again shortly.");
        }

        GatewayTransaction tx = GatewayTransaction.builder()
                .provider("LENCO")
                .reference(reference)
                .lencoId(data.path("id").asText(null))
                .lencoReference(data.path("lencoReference").asText(null))
                .invoice(invoice)
                .guardian(guardian)
                .phone(phone)
                .operator(normalizedOperator)
                .amount(amount)
                .status(GatewayTransactionStatus.PENDING)
                .rawResponse(rawResponse)
                .build();
        return gatewayRepo.save(tx);
    }

    // Always re-queries Lenco directly rather than trusting whatever is cached locally — this
    // is what both the parent portal's polling and the webhook handler call, and it's the only
    // place a successful gateway transaction turns into a real, invoice-adjusting Payment.
    public GatewayTransaction checkStatus(String reference) {
        GatewayTransaction tx = gatewayRepo.findByReference(reference)
                .orElseThrow(() -> new EntityNotFoundException("Payment attempt not found"));
        if (tx.getStatus() != GatewayTransactionStatus.PENDING) {
            return tx;
        }

        JsonNode data;
        String rawResponse;
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/collections/status/" + reference, HttpMethod.GET,
                    new HttpEntity<>(authHeaders()), String.class);
            rawResponse = response.getBody();
            data = objectMapper.readTree(rawResponse).path("data");
        } catch (Exception e) {
            log.warn("Lenco status check failed for {}: {}", reference, e.getMessage());
            return tx;
        }

        String lencoStatus = data.path("status").asText("");
        tx.setRawResponse(rawResponse);
        if (data.hasNonNull("lencoReference")) tx.setLencoReference(data.path("lencoReference").asText());

        if ("successful".equalsIgnoreCase(lencoStatus)) {
            tx.setStatus(GatewayTransactionStatus.SUCCESSFUL);
            gatewayRepo.save(tx);
            confirmAsPayment(tx);
        } else if ("failed".equalsIgnoreCase(lencoStatus)) {
            tx.setStatus(GatewayTransactionStatus.FAILED);
            tx.setFailureReason(data.path("reasonForFailure").asText(null));
            gatewayRepo.save(tx);
        } else {
            gatewayRepo.save(tx); // still pending — just refresh the cached raw response
        }
        return tx;
    }

    // The single choke point where a gateway-confirmed collection becomes a real Payment —
    // guarded by paymentId being null so a re-check (polling AND webhook can both land on the
    // same already-successful transaction) never double-applies the amount to the invoice.
    private void confirmAsPayment(GatewayTransaction tx) {
        if (tx.getPaymentId() != null) return;
        Invoice invoice = tx.getInvoice();
        Payment payment = Payment.builder()
                .receiptNo(generateReceiptNo())
                .pupil(invoice.getPupil())
                .invoice(invoice)
                .amount(tx.getAmount())
                .method(PaymentMethod.MOBILE_MONEY)
                .paidOn(LocalDate.now())
                .reference(tx.getLencoReference() != null ? tx.getLencoReference() : tx.getReference())
                .status(PaymentStatus.CONFIRMED)
                .submittedBy(tx.getGuardian())
                .build();
        payment = paymentRepo.save(payment);
        invoiceBalanceService.adjust(invoice, tx.getAmount());
        tx.setPaymentId(payment.getId());
        gatewayRepo.save(tx);
        audit.log("LENCO_PAYMENT_CONFIRMED", "Payment", payment.getId().toString(),
                "Mobile money via " + tx.getOperator() + ", reference " + tx.getReference()
                        + ", invoice " + invoice.getInvoiceNo());
    }

    public boolean verifySignature(String rawBody, String signatureHeader) {
        if (signatureHeader == null || signatureKey == null || signatureKey.isBlank()) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(signatureKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] computed = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String computedHex = HexFormat.of().formatHex(computed);
            return MessageDigest.isEqual(
                    computedHex.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.trim().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("Lenco webhook signature verification error", e);
            return false;
        }
    }

    // Best-effort: whatever event shape Lenco actually sends, find something reference-like so
    // checkStatus can be re-triggered — the webhook is only ever a "check again now" nudge, its
    // own claimed status/event type is never trusted.
    public String extractReference(String rawBody) {
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            JsonNode data = root.path("data");
            for (String field : List.of("reference", "clientReference", "transactionReference")) {
                if (data.hasNonNull(field)) return data.path(field).asText();
                if (root.hasNonNull(field)) return root.path(field).asText();
            }
        } catch (Exception e) {
            log.warn("Could not parse Lenco webhook body", e);
        }
        return null;
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + secretKey);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return headers;
    }

    private String generateReceiptNo() {
        return "RCT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
