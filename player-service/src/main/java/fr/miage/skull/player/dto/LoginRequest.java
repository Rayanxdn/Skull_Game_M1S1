package fr.miage.skull.player.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format invalide")
        String email,

        @NotBlank(message = "Mot de passe obligatoire")
        String password
) {}
