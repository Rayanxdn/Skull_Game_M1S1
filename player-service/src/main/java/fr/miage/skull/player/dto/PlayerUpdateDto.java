package fr.miage.skull.player.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class PlayerUpdateDto {
    private String pseudo;

    @Email(message = "Format d'email invalide")
    private String email;

    private String password;

    public String getPseudo() {
        return pseudo;
    }

    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    public @Email(message = "Format d'email invalide") String getEmail() {
        return email;
    }

    public void setEmail(@Email(message = "Format d'email invalide") String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
