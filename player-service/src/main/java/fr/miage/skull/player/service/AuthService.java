package fr.miage.skull.player.service;

import fr.miage.skull.player.dto.LoginRequest;
import fr.miage.skull.player.dto.LoginResponse;
import fr.miage.skull.player.model.Player;
import fr.miage.skull.player.repository.PlayerRepository;
import fr.miage.skull.player.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final PlayerRepository playerRepository;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager,
                       PlayerRepository playerRepository,
                       JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.playerRepository = playerRepository;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        // Vérifie le mot de passe (BCrypt) et que le compte n'est pas banni.
        // Lève BadCredentialsException ou LockedException en cas d'échec.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        Player player = playerRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Identifiants invalides"));

        String token = jwtService.generateToken(player);
        return new LoginResponse(token, "Bearer", player.getId(), player.getPseudo(), player.getRole());
    }
}
