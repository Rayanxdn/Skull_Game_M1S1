
package fr.miage.skull.player.controller;

import fr.miage.skull.player.dto.ReportRequestDto;
import fr.miage.skull.player.dto.ReportResponseDto;
import fr.miage.skull.player.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Reports", description = "Gestion des signalements")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/api/v1/players/{id}/report")
    @Operation(summary = "Signaler un joueur")
    public ResponseEntity<ReportResponseDto> createReport(
            @PathVariable Long id,
            @Valid @RequestBody ReportRequestDto request,
            Authentication authentication) {

        ReportResponseDto report = reportService.createReport(
                id,
                request,
                authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }

    @GetMapping("/api/v1/reports")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Consulter tous les signalements")
    public ResponseEntity<List<ReportResponseDto>> getAllReports() {

        return ResponseEntity.ok(reportService.getAllReports());
    }
}
