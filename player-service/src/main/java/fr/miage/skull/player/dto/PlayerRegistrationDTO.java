package fr.miage.skull.player.dto;

import jakarta.validation.constraints.*;

public class PlayerRegistrationDTO {

    @NotBlank(message = "Le pseudo est obligatoire")
    private String pseudo;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format invalide")
    private String email;

    @NotBlank(message= "Mot de passe obligatoire")
    @Size(min= 6 , message = "Le mot de passe doit contenir au moins 6 caractères")
    private String password;

    public @NotBlank(message = "Mot de passe obligatoire") @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères") String getPassword() {
        return password;
    }

    public void setPassword(@NotBlank(message = "Mot de passe obligatoire") @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères") String password) {
        this.password = password;
    }

    public @NotBlank(message = "L'email est obligatoire") @Email(message = "Format invalide") String getEmail() {
        return email;
    }

    public void setEmail(@NotBlank(message = "L'email est obligatoire") @Email(message = "Format invalide") String email) {
        this.email = email;
    }

    public @NotBlank(message = "Le pseudo est obligatoire") String getPseudo() {
        return pseudo;
    }

    public void setPseudo(@NotBlank(message = "Le pseudo est obligatoire") String pseudo) {
        this.pseudo = pseudo;
    }
}
