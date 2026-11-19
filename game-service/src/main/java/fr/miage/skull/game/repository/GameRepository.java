package fr.miage.skull.game.repository;

import fr.miage.skull.game.entity.Game;
import fr.miage.skull.game.entity.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GameRepository extends JpaRepository<Game, Long> {
    List<Game> findByStatusOrderByCreatedAtDesc(GameStatus status);
}
