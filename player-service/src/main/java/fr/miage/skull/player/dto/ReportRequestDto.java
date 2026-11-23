
package fr.miage.skull.player.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ReportRequestDto {

    @NotBlank(message = "Le motif du signalement est obligatoire")
    @Size(max = 1000, message = "Le motif ne doit pas dépasser 1000 caractères")
    private String reason;

    private Long gameId;

    public ReportRequestDto() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }
}
