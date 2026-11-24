
package fr.miage.skull.player.service;

import fr.miage.skull.player.dto.ReportRequestDto;
import fr.miage.skull.player.dto.ReportResponseDto;
import fr.miage.skull.player.exception.RessourceNotFoundException;
import fr.miage.skull.player.model.Player;
import fr.miage.skull.player.model.Report;
import fr.miage.skull.player.model.ReportStatus;
import fr.miage.skull.player.repository.PlayerRepository;
import fr.miage.skull.player.repository.ReportRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final PlayerRepository playerRepository;

    public ReportService(ReportRepository reportRepository,
                         PlayerRepository playerRepository) {
        this.reportRepository = reportRepository;
        this.playerRepository = playerRepository;
    }

    @Transactional
    public ReportResponseDto createReport(
            Long reportedPlayerId,
            ReportRequestDto request,
            String reporterEmail) {

        Player reporter = playerRepository.findByEmail(reporterEmail)
                .orElseThrow(() -> new RessourceNotFoundException(
                        "Joueur connecté introuvable"));

        Player reportedPlayer = playerRepository.findById(reportedPlayerId)
                .orElseThrow(() -> new RessourceNotFoundException(
                        "Joueur signalé introuvable"));

        if (reporter.getId().equals(reportedPlayerId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vous ne pouvez pas vous signaler vous-même");
        }

        boolean duplicate = reportRepository
                .existsByReporter_IdAndReportedPlayer_IdAndGameId(
                        reporter.getId(),
                        reportedPlayerId,
                        request.getGameId());

        if (duplicate) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Vous avez déjà signalé ce joueur pour cette partie");
        }

        Report report = new Report(
                reporter,
                reportedPlayer,
                request.getReason().trim(),
                request.getGameId());

        Report savedReport = reportRepository.save(report);

        return new ReportResponseDto(savedReport);
    }

    @Transactional(readOnly = true)
    public List<ReportResponseDto> getAllReports() {
        return reportRepository.findAll()
                .stream()
                .map(ReportResponseDto::new)
                .toList();
    }

    @Transactional
    public void markReportsAsTreated(Long reportedPlayerId) {
        List<Report> openReports =
                reportRepository.findByReportedPlayer_IdAndStatus(
                        reportedPlayerId,
                        ReportStatus.OPEN);

        for (Report report : openReports) {
            report.setStatus(ReportStatus.TREATED);
        }
    }
}
