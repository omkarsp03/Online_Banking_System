package com.onlinebanking.controller;

import com.onlinebanking.service.StatementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.Principal;
import java.time.Instant;

@RestController
@RequestMapping("/api/customer/accounts")
@RequiredArgsConstructor
@Tag(name = "Account Statement", description = "Endpoints for exporting bank account statements")
public class StatementController {

    private final StatementService statementService;

    @GetMapping("/{accountNumber}/statement")
    @Operation(summary = "Export account statement", description = "Generates and downloads transaction history in PDF or CSV format")
    public ResponseEntity<byte[]> exportStatement(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            Principal principal) throws IOException {

        Instant fromInstant = (from != null && !from.isBlank()) ? Instant.parse(from) : null;
        Instant toInstant = (to != null && !to.isBlank()) ? Instant.parse(to) : null;

        byte[] data = statementService.generateStatement(accountNumber, principal.getName(), format, fromInstant, toInstant);

        MediaType mediaType = "csv".equalsIgnoreCase(format) ? MediaType.parseMediaType("text/csv") : MediaType.APPLICATION_PDF;
        String extension = "csv".equalsIgnoreCase(format) ? "csv" : "pdf";
        String filename = "statement_" + accountNumber + "." + extension;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(data);
    }
}
