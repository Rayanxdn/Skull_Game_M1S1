
package fr.miage.skull.player.dto;

import fr.miage.skull.player.model.Report;
import fr.miage.skull.player.model.ReportStatus;

import java.time.LocalDateTime;

public class ReportResponseDto {

    private Long id;
    private Long reporterId;
    private String reporterPseudo;
    private Long reportedPlayerId;
    private String reportedPlayerPseudo;
    private String reason;
    private Long gameId;
    private LocalDateTime createdAt;
    private ReportStatus status;

    public ReportResponseDto(Report report) {
        this.id = report.getId();
        this.reporterId = report.getReporter().getId();
        this.reporterPseudo = report.getReporter().getPseudo();
        this.reportedPlayerId = report.getReportedPlayer().getId();
        this.reportedPlayerPseudo = report.getReportedPlayer().getPseudo();
        this.reason = report.getReason();
        this.gameId = report.getGameId();
        this.createdAt = report.getCreatedAt();
        this.status = report.getStatus();
    }

    public Long getId() {
        return id;
    }

    public Long getReporterId() {
        return reporterId;
    }

    public String getReporterPseudo() {
        return reporterPseudo;
    }

    public Long getReportedPlayerId() {
        return reportedPlayerId;
    }

    public String getReportedPlayerPseudo() {
        return reportedPlayerPseudo;
    }

    public String getReason() {
        return reason;
    }

    public Long getGameId() {
        return gameId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public ReportStatus getStatus() {
        return status;
    }
}
