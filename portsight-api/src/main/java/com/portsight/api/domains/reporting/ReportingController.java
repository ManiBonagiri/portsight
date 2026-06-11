package com.portsight.api.domains.reporting;

import com.portsight.api.domains.reporting.dto.ReportResponse;
import com.portsight.api.domains.reporting.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;

    /**
     * Generate a new report.
     * Body: { "portfolioId": "uuid", "reportType":
     * "PORTFOLIO|RISK|PERFORMANCE|ALLOCATION" }
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateReport(
            @AuthenticationPrincipal String principal,
            @RequestBody Map<String, String> body) {

        UUID userId = UUID.fromString(principal);
        UUID portfolioId = UUID.fromString(body.get("portfolioId"));
        String reportType = body.get("reportType");

        ReportResponse response = reportingService.generateReport(userId, portfolioId, reportType);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("success", true, "data", response));
    }

    /**
     * List all reports for a portfolio.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getReports(
            @RequestParam UUID portfolioId) {

        List<ReportResponse> reports = reportingService.getReports(portfolioId);
        return ResponseEntity.ok(Map.of("success", true, "data", reports));
    }

    /**
     * Download a report PDF by report ID.
     */
    @GetMapping("/{reportId}")
    public ResponseEntity<byte[]> downloadReport(
            @PathVariable UUID reportId) {

        byte[] pdf = reportingService.downloadReport(reportId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
                "portsight-report-" + reportId + ".pdf");
        headers.setContentLength(pdf.length);

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    /**
     * Delete a report record.
     */
    @DeleteMapping("/{reportId}")
    public ResponseEntity<Map<String, Object>> deleteReport(
            @PathVariable UUID reportId) {

        reportingService.deleteReport(reportId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Report deleted"));
    }
}