package fr.miage.skull.player.mapper;

import fr.miage.skull.player.dto.PlayerResponseDto;
import fr.miage.skull.player.model.Player;
import org.springframework.stereotype.Component;

@Component
public class PlayerMapper {
    public PlayerResponseDto toDto(Player player) {
        if (player == null) {
            return null;
        }
        PlayerResponseDto dto = new PlayerResponseDto();
        dto.setId(player.getId());
        dto.setPseudo(player.getPseudo());
        dto.setEmail(player.getEmail());
        dto.setBanned(player.isBanned());
        return dto;
    }

   /* public Player toEntity(PlayerResponseDto dto) {
        if (dto == null) {
            return null;
        }
        Player player = new Player();
        player.setId(dto.getId());
        player.setPseudo(dto.getPseudo());
        player.setEmail(dto.getEmail());
        player.setBanned(dto.isBanned());
        return player;
    } */
}
