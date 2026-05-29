package com.onlinebanking.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.opencsv.CSVWriter;
import com.onlinebanking.entity.BankAccount;
import com.onlinebanking.entity.TransactionRecord;
import com.onlinebanking.entity.TransactionType;
import com.onlinebanking.exception.ApiException;
import com.onlinebanking.repository.BankAccountRepository;
import com.onlinebanking.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatementService {

    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public byte[] generateStatement(String accountNumber, String ownerEmail, String format, Instant from, Instant to) throws IOException {
        BankAccount account = bankAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ApiException("Bank account not found"));

        if (!account.getOwner().getEmail().equalsIgnoreCase(ownerEmail)) {
            throw new ApiException("Unauthorized access to account statement");
        }

        List<TransactionRecord> transactions = transactionRepository.findBySourceAccountIdOrDestinationAccountId(account.getId(), account.getId());

        // Filter by date range if provided
        if (from != null) {
            transactions = transactions.stream()
                    .filter(tx -> tx.getCreatedAt().isAfter(from))
                    .collect(Collectors.toList());
        }
        if (to != null) {
            transactions = transactions.stream()
                    .filter(tx -> tx.getCreatedAt().isBefore(to))
                    .collect(Collectors.toList());
        }

        // Sort descending by date
        transactions.sort((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()));

        if ("csv".equalsIgnoreCase(format)) {
            return generateCsv(account, transactions);
        } else if ("pdf".equalsIgnoreCase(format)) {
            return generatePdf(account, transactions);
        } else {
            throw new ApiException("Unsupported format: " + format);
        }
    }

    private byte[] generateCsv(BankAccount account, List<TransactionRecord> transactions) throws IOException {
        StringWriter stringWriter = new StringWriter();
        try (CSVWriter csvWriter = new CSVWriter(stringWriter)) {
            csvWriter.writeNext(new String[]{"Statement for Account", account.getAccountNumber()});
            csvWriter.writeNext(new String[]{"Balance", account.getBalance().toString(), "Currency", account.getCurrency()});
            csvWriter.writeNext(new String[]{""});
            csvWriter.writeNext(new String[]{"Transaction ID", "Date", "Type", "Amount", "Currency", "Description", "Source Account", "Destination Account"});

            for (TransactionRecord tx : transactions) {
                csvWriter.writeNext(new String[]{
                        tx.getId().toString(),
                        tx.getCreatedAt().toString(),
                        tx.getTransactionType().name(),
                        tx.getAmount().toString(),
                        tx.getCurrency(),
                        tx.getDescription() != null ? tx.getDescription() : "",
                        tx.getSourceAccount() != null ? tx.getSourceAccount().getAccountNumber() : "",
                        tx.getDestinationAccount() != null ? tx.getDestinationAccount().getAccountNumber() : ""
                });
            }
        }
        return stringWriter.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] generatePdf(BankAccount account, List<TransactionRecord> transactions) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Styling & Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

            // Title
            Paragraph title = new Paragraph("ONLINE BANKING SYSTEM", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("Account Statement", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);
            document.add(new Paragraph(" "));

            // Details Section
            document.add(new Paragraph("Account Number: " + account.getAccountNumber(), headerFont));
            document.add(new Paragraph("Account Owner: " + account.getOwner().getFirstName() + " " + account.getOwner().getLastName(), subTitleFont));
            document.add(new Paragraph("Current Balance: " + account.getBalance() + " " + account.getCurrency(), subTitleFont));
            document.add(new Paragraph("Generated At: " + Instant.now().toString(), subTitleFont));
            document.add(new Paragraph(" "));

            // Table
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.0f, 2.0f, 1.5f, 1.5f, 2.5f, 2.0f});

            // Headers
            String[] headers = {"ID", "Date", "Type", "Amount", "Description", "Detail"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Paragraph(header, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            // Data rows
            for (TransactionRecord tx : transactions) {
                table.addCell(new Paragraph(tx.getId().toString(), cellFont));
                table.addCell(new Paragraph(tx.getCreatedAt().toString().substring(0, 19).replace("T", " "), cellFont));
                table.addCell(new Paragraph(tx.getTransactionType().name(), cellFont));
                table.addCell(new Paragraph(tx.getAmount().toString() + " " + tx.getCurrency(), cellFont));
                table.addCell(new Paragraph(tx.getDescription() != null ? tx.getDescription() : "", cellFont));
                
                String detail = "";
                if (tx.getTransactionType() == TransactionType.TRANSFER) {
                    if (tx.getSourceAccount() != null && tx.getSourceAccount().getAccountNumber().equals(account.getAccountNumber())) {
                        detail = "To: " + (tx.getDestinationAccount() != null ? tx.getDestinationAccount().getAccountNumber() : "Unknown");
                    } else {
                        detail = "From: " + (tx.getSourceAccount() != null ? tx.getSourceAccount().getAccountNumber() : "Unknown");
                    }
                } else {
                    detail = "-";
                }
                table.addCell(new Paragraph(detail, cellFont));
            }

            document.add(table);
        } catch (DocumentException e) {
            throw new ApiException("Error creating PDF document: " + e.getMessage());
        } finally {
            document.close();
        }
        return out.toByteArray();
    }
}
