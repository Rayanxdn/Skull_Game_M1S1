package fr.miage.skull.player.dto;

import java.util.List;

public record LoginResponse(
        String token,
        String tokenType,
        Long id,
        String pseudo,
        List<String> roles
) {}
