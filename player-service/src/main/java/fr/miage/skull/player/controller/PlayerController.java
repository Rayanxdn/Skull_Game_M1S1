package fr.miage.skull.player.controller;

import fr.miage.skull.player.dto.PlayerRegistrationDTO;
import fr.miage.skull.player.model.Player;
import fr.miage.skull.player.service.PlayerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/players")
@Tag(name= "Player Service", description = "Gestion des comptes")
public class PlayerController {
    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @PostMapping("register")
    @Operation(summary = "Inscrire un joueur")
    @ApiResponse(responseCode = "201", description = "Joueur créé avec succès !")
    @ApiResponse(responseCode = "400", description = "Données invalides")
    public ResponseEntity<Player> register(@Valid @RequestBody PlayerRegistrationDTO dto) {
        Player newPlayer = playerService.registerPlayer(dto);
        newPlayer.setPassword(null); //Masque le mdp par mesure de sécurité
        return ResponseEntity.status(HttpStatus.CREATED).body(newPlayer);
    }
}
