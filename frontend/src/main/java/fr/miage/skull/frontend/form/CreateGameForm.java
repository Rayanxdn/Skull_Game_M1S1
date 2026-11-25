package fr.miage.skull.frontend.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Mêmes bornes que le game-service, qui revalide de son côté. Motif vide : tiré au hasard. */
public class CreateGameForm {

    @NotNull(message = "Indique le nombre maximum de joueurs")
    @Min(value = 3, message = "Une partie se joue à 3 joueurs minimum")
    @Max(value = 6, message = "Une partie est limitée à 6 joueurs")
    private Integer maxPlayers = 4;

    @NotNull(message = "Indique le nombre d'IA")
    @Min(value = 0, message = "Le nombre d'IA ne peut pas être négatif")
    @Max(value = 5, message = "5 IA au maximum")
    private Integer aiCount = 2;

    @Size(max = 30)
    private String flowerTheme = "";

    @Size(max = 30)
    private String skullTheme = "";

    public Integer getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(Integer maxPlayers) { this.maxPlayers = maxPlayers; }
    public Integer getAiCount() { return aiCount; }
    public void setAiCount(Integer aiCount) { this.aiCount = aiCount; }
    public String getFlowerTheme() { return flowerTheme; }
    public void setFlowerTheme(String flowerTheme) { this.flowerTheme = flowerTheme; }
    public String getSkullTheme() { return skullTheme; }
    public void setSkullTheme(String skullTheme) { this.skullTheme = skullTheme; }
}
