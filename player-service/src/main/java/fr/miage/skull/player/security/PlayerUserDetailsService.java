package fr.miage.skull.player.security;

import fr.miage.skull.player.model.Player;
import fr.miage.skull.player.repository.PlayerRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PlayerUserDetailsService implements UserDetailsService {

    private final PlayerRepository playerRepository;

    public PlayerUserDetailsService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Player player = playerRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Joueur introuvable"));

        String[] roles = player.getRole() == null
                ? new String[0]
                : player.getRole().toArray(new String[0]);

        return User.withUsername(player.getEmail())
                .password(player.getPassword())   // déjà haché en BCrypt
                .roles(roles)                      // "USER" devient l'autorité ROLE_USER
                .accountLocked(player.isBanned())  // banni = compte verrouillé
                .build();
    }
}
