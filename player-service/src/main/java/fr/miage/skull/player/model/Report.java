
package fr.miage.skull.player.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private Player reporter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_player_id", nullable = false)
    private Player reportedPlayer;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(name = "game_id")
    private Long gameId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status = ReportStatus.OPEN;

    public Report() {
    }

    public Report(Player reporter, Player reportedPlayer,
                  String reason, Long gameId) {
        this.reporter = reporter;
        this.reportedPlayer = reportedPlayer;
        this.reason = reason;
        this.gameId = gameId;
        this.createdAt = LocalDateTime.now();
        this.status = ReportStatus.OPEN;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Player getReporter() {
        return reporter;
    }

    public Player getReportedPlayer() {
        return reportedPlayer;
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

    public void setStatus(ReportStatus status) {
        this.status = status;
    }
}
