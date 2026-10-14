package fr.miage.skull.player.service;

import fr.miage.skull.player.dto.PlayerRegistrationDTO;
import fr.miage.skull.player.dto.PlayerResponseDto;
import fr.miage.skull.player.dto.PlayerUpdateDto;
import fr.miage.skull.player.exception.EmailAlreadyUsedException;
import fr.miage.skull.player.exception.RessourceNotFoundException;
import fr.miage.skull.player.mapper.PlayerMapper;
import fr.miage.skull.player.model.Player;
import fr.miage.skull.player.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlayerMapper playerMapper;

    public PlayerService(PlayerRepository playerRepository, PasswordEncoder passwordEncoder, PlayerMapper playerMapper) {
        this.playerRepository = playerRepository;
        this.passwordEncoder = passwordEncoder;
        this.playerMapper = playerMapper;
    }

    public PlayerResponseDto registerPlayer(PlayerRegistrationDTO dto) {
        if (playerRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new EmailAlreadyUsedException("Un compte existe déjà avec cet email");
        }
        Player player = new Player(
                dto.getEmail(),
                passwordEncoder.encode(dto.getPassword()),
                dto.getPseudo(),
                List.of("USER")
        );

        Player savedPlayer = playerRepository.save(player);
        return playerMapper.toDto(savedPlayer);
    }

    public PlayerResponseDto setBannedStatus(Long playerId, boolean banned) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RessourceNotFoundException("Joueur non trouvé"));
        player.setBanned(banned);
        Player savedPlayer = playerRepository.save(player);
        return playerMapper.toDto(savedPlayer);
    }

    public PlayerResponseDto getPlayerDtoById(Long id) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Joueur non trouvé"));
        return playerMapper.toDto(player);
    }

    public PlayerResponseDto updatePlayer(Long id, PlayerUpdateDto updateDto) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Joueur non trouvé"));

        if (updateDto.getPseudo() != null && !updateDto.getPseudo().trim().isEmpty()) {
            player.setPseudo(updateDto.getPseudo());
        }
        if (updateDto.getEmail() != null && !updateDto.getEmail().trim().isEmpty()) {
            if (!player.getEmail().equals(updateDto.getEmail())) {
                if (playerRepository.findByEmail(updateDto.getEmail()).isPresent()) {
                    throw new EmailAlreadyUsedException("Cet email est déjà utilisé par un autre compte !");
                }
            }
            player.setEmail(updateDto.getEmail());
        }
        if (updateDto.getPassword() != null && !updateDto.getPassword().isEmpty()) {
            player.setPassword(passwordEncoder.encode(updateDto.getPassword()));
        }

        Player updatedPlayer = playerRepository.save(player);
        return playerMapper.toDto(updatedPlayer);
    }

    public void deletePlayer(Long id) {
        if (!playerRepository.existsById(id)) {
            throw new RessourceNotFoundException("Joueur non trouvé");
        }
        playerRepository.deleteById(id);
    }

    public List<PlayerResponseDto> getAllePlayers() {
        List<Player> players = playerRepository.findAll();
        return players.stream().map(playerMapper::toDto).toList();
    }
}