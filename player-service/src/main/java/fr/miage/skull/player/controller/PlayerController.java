package fr.miage.skull.player.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import fr.miage.skull.player.dto.PlayerRegistrationDTO;
import fr.miage.skull.player.dto.PlayerResponseDto;
import fr.miage.skull.player.dto.PlayerUpdateDto;
import fr.miage.skull.player.service.PlayerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/v1/players")
@Tag(name = "Player Service", description = "Gestion des comptes")
public class PlayerController {

    private final PlayerService playerService;

    @Autowired
    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @PostMapping("register")
    @Operation(summary = "Inscrire un joueur")
    @ApiResponse(responseCode = "201", description = "Joueur créé avec succès !")
    @ApiResponse(responseCode = "400", description = "Données invalides")
    public ResponseEntity<PlayerResponseDto> register(@Valid @RequestBody PlayerRegistrationDTO dto) {
        PlayerResponseDto newPlayer = playerService.registerPlayer(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(newPlayer);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}/ban") // Endpoint pour bannir un joueur par un admin uniquement.
    @Operation(summary = "Bannir un compte")
    public ResponseEntity<PlayerResponseDto> banPlayer(@PathVariable Long id, @RequestParam boolean banned) {
        PlayerResponseDto updatePlayer = playerService.setBannedStatus(id, banned);
        return ResponseEntity.ok(updatePlayer);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @playerSecurity.isOwner(#id, authentication)")
    @Operation(summary = "Afficher un compte")
    public ResponseEntity<PlayerResponseDto> getPlayer(@PathVariable Long id) {
        PlayerResponseDto playerDto = playerService.getPlayerDtoById(id);
        return ResponseEntity.ok(playerDto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @playerSecurity.isOwner(#id, authentication)")
    @Operation(summary = "Mettre à jour une donnée d'un joueur")
    public ResponseEntity<PlayerResponseDto> updatePlayer(
            @PathVariable Long id,
            @Valid @RequestBody PlayerUpdateDto updateDto) {
        PlayerResponseDto updatePlayer = playerService.updatePlayer(id, updateDto);
        return ResponseEntity.ok(updatePlayer);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @playerSecurity.isOwner(#id, authentication)")
    @Operation(summary = "Supprimer un compte")
    @ApiResponse(responseCode = "204", description = "Joueur supprimé avec succès")
    @ApiResponse(responseCode = "404", description = "Joueur non trouvé")
    public ResponseEntity<Void> deletePlayer(@PathVariable Long id) {
        playerService.deletePlayer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "lister tous les joueurs")
    @ApiResponse(responseCode = "200", description = " Liste des joueurs récuprée avec succès")
    public ResponseEntity<List<PlayerResponseDto>> getAllPlayers() {
        List<PlayerResponseDto> players = playerService.getAllePlayers();
        return ResponseEntity.ok(players);
    }
}