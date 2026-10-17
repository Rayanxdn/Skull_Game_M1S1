package fr.miage.skull.player.config;

import fr.miage.skull.player.model.Player;
import fr.miage.skull.player.repository.PlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/** Crée le compte administrateur au premier démarrage, s'il n'existe pas encore. */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminPseudo;

    public AdminInitializer(PlayerRepository playerRepository,
                            PasswordEncoder passwordEncoder,
                            @Value("${admin.email}") String adminEmail,
                            @Value("${admin.password}") String adminPassword,
                            @Value("${admin.pseudo}") String adminPseudo) {
        this.playerRepository = playerRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminPseudo = adminPseudo;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("ADMIN_PASSWORD non défini : le compte administrateur n'est pas créé.");
            return;
        }
        if (playerRepository.findByEmail(adminEmail).isPresent()) {
            return;
        }
        playerRepository.save(new Player(
                adminEmail,
                passwordEncoder.encode(adminPassword),
                adminPseudo,
                List.of("ADMIN")));
        log.info("Compte administrateur créé : {}", adminEmail);
    }
}
