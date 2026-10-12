package fr.miage.skull.player.service;

import fr.miage.skull.player.dto.PlayerRegistrationDTO;
import fr.miage.skull.player.model.Player;
import fr.miage.skull.player.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Service
public class PlayerService {
    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;

    public PlayerService (PlayerRepository playerRepository, PasswordEncoder passwordEncoder) {
        this.playerRepository = playerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Player registerPlayer(PlayerRegistrationDTO dto) {
        if (playerRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException("Un compte existe déjà avec cet emali");
        }
        Player player = new Player(
                dto.getEmail(),
                passwordEncoder.encode(dto.getPassword()),
                dto.getPseudo(),
                List.of("USER")
        );

        return playerRepository.save(player);
    }
}
