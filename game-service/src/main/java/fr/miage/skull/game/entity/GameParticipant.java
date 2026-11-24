package fr.miage.skull.game.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "game_participants")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class GameParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    // Null si c'est une IA créée au départ
    @Column(name = "player_id")
    private Long playerId;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private boolean ai;

    // Pour la position des joueurs autour de la table
    @Column(nullable = false)
    private int seatOrder;

    // Motifs choisis par le joueur (par défaut pour l'instant)
    private String flowerTheme;
    private String skullTheme;

    @Column(nullable = false)
    private boolean forfeited;
}
