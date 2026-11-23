
package fr.miage.skull.player.repository;

import fr.miage.skull.player.model.Report;
import fr.miage.skull.player.model.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporter_IdAndReportedPlayer_IdAndGameId(
            Long reporterId,
            Long reportedPlayerId,
            Long gameId
    );

    List<Report> findByReportedPlayer_IdAndStatus(
            Long reportedPlayerId,
            ReportStatus status
    );
}
