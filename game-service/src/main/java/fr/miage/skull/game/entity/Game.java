package fr.miage.skull.game.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "games")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus status;

    @Column(nullable = false)
    private int maxPlayers;

    @Column(nullable = false)
    private int aiCount;

    @Column(nullable = false)
    private Long creatorId;

    private Long winnerId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // pour état du moteur sérialisé en JSON (GameState)
    @Column(columnDefinition = "TEXT")
    private String stateJson;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("seatOrder ASC")
    @Builder.Default
    private List<GameParticipant> participants = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public void addParticipant(GameParticipant participant) {
        participant.setGame(this);
        participant.setSeatOrder(this.participants.size());
        this.participants.add(participant);
    }
}
