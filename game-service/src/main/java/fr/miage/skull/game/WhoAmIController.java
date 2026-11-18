package fr.miage.skull.game;

import fr.miage.skull.game.security.AuthPlayer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WhoAmIController {

    @Operation(summary = "Identité du joueur connecté, lue dans le jeton",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/api/v1/whoami")
    public String whoami(@AuthenticationPrincipal AuthPlayer player) {
        return player.email() + " (id=" + player.id() + ") roles=" + player.roles();
    }
}
