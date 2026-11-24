
package fr.miage.skull.player.service;

import fr.miage.skull.player.dto.ReportRequestDto;
import fr.miage.skull.player.dto.ReportResponseDto;
import fr.miage.skull.player.exception.RessourceNotFoundException;
import fr.miage.skull.player.model.Player;
import fr.miage.skull.player.model.Report;
import fr.miage.skull.player.model.ReportStatus;
import fr.miage.skull.player.repository.PlayerRepository;
import fr.miage.skull.player.repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private ReportService reportService;

    private Player reporter;
    private Player reportedPlayer;
    private ReportRequestDto request;

    @BeforeEach
    void setUp() {
        reporter = new Player();
        reporter.setId(1L);
        reporter.setEmail("alice@example.com");
        reporter.setPseudo("Alice");

        reportedPlayer = new Player();
        reportedPlayer.setId(2L);
        reportedPlayer.setPseudo("Bob");

        request = new ReportRequestDto();
        request.setReason("Comportement suspect");
        request.setGameId(10L);
    }

    @Test
    void validReportIsCreated() {
        when(playerRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(reporter));
        when(playerRepository.findById(2L)).thenReturn(Optional.of(reportedPlayer));
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReportResponseDto result = reportService.createReport(
                2L, request, "alice@example.com");

        assertEquals(1L, result.getReporterId());
        assertEquals(2L, result.getReportedPlayerId());
        assertEquals(ReportStatus.OPEN, result.getStatus());
        assertEquals("Comportement suspect", result.getReason());

        verify(reportRepository).save(any(Report.class));
    }

    @Test
    void playerCannotReportThemselves() {
        when(playerRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(reporter));
        when(playerRepository.findById(1L)).thenReturn(Optional.of(reporter));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> reportService.createReport(1L, request, "alice@example.com"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    void unknownReportedPlayerIsRejected() {
        when(playerRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(reporter));
        when(playerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RessourceNotFoundException.class,
                () -> reportService.createReport(99L, request, "alice@example.com"));

        verify(reportRepository, never()).save(any(Report.class));
    }


    @Test
    void duplicateReportIsRejected() {when(playerRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(reporter));

        when(playerRepository.findById(2L)).thenReturn(Optional.of(reportedPlayer));

        when(reportRepository
                .existsByReporter_IdAndReportedPlayer_IdAndGameId(
                        1L, 2L, 10L))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> reportService.createReport(2L, request, "alice@example.com"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    void openReportsAreMarkedAsTreated() {
        Report report = new Report(
                reporter,
                reportedPlayer,
                "Comportement suspect",
                10L
        );

        when(reportRepository.findByReportedPlayer_IdAndStatus(2L, ReportStatus.OPEN))
                .thenReturn(java.util.List.of(report));

        reportService.markReportsAsTreated(2L);

        assertEquals(ReportStatus.TREATED, report.getStatus());

        verify(reportRepository).findByReportedPlayer_IdAndStatus(2L, ReportStatus.OPEN);
    }

}
