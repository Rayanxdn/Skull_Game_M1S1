package fr.miage.skull.player.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import fr.miage.skull.player.model.Player;
import fr.miage.skull.player.repository.PlayerRepository;
import org.springframework.boot.test.mock.mockito.MockBean;
import java.util.Optional;
import fr.miage.skull.player.dto.PlayerUpdateDto;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.junit.jupiter.api.BeforeEach;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;



@SpringBootTest
class BanSecurityTest {

    @Autowired
    private PlayerService playerService;

    @MockBean
    private PlayerRepository playerRepository;
    @BeforeEach
    void resetRepositoryInteractions() {
        clearInvocations(playerRepository);
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCannotBanPlayer(){assertThrows(AccessDeniedException.class,()-> playerService.setBannedStatus(1L,true));
    verifyNoInteractions(playerRepository);}


    

    


    


    @Test
    @WithMockUser(roles = "USER")
    void userCannotUnbanPlayer() {

        assertThrows(
                AccessDeniedException.class,
                () -> playerService.setBannedStatus(1L, false)
        );

        verifyNoInteractions(playerRepository);
    }


    


    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAcessBanMethod(){

        Player player = new Player();
        player.setId(1L);
        player.setBanned(false);

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));

        when(playerRepository.save(any(Player.class))).thenAnswer((invocation -> invocation.getArgument(0)));

        playerService.setBannedStatus(1L, true);

        assertTrue(player.isBanned());

        verify(playerRepository).save(player);


    }


    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanDeletePlayer() {

        when(playerRepository.existsById(1L))
                .thenReturn(true);

        playerService.deletePlayer(1L);

        verify(playerRepository).deleteById(1L);
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    void updatingPlayerDoesNotRemoveBan() {

        Player player = new Player();
        player.setId(1L);
        player.setPseudo("AncienPseudo");
        player.setBanned(true);

        PlayerUpdateDto updateDto = new PlayerUpdateDto();
        updateDto.setPseudo("NouveauPseudo");

        when(playerRepository.findById(1L))
                .thenReturn(Optional.of(player));

        when(playerRepository.save(any(Player.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        playerService.updatePlayer(1L, updateDto);

        assertEquals("NouveauPseudo", player.getPseudo());
        assertTrue(player.isBanned());

        verify(playerRepository).save(player);
    }


    @Test
    void bannedPlayerIsDetected() {

        Player player = new Player();
        player.setId(1L);
        player.setBanned(true);

        when(playerRepository.findById(1L))
                .thenReturn(Optional.of(player));

        boolean result = playerService.isPlayerBanned(1L);

        assertTrue(result);
        verify(playerRepository).findById(1L);
    }


    @Test
    void nonBannedPlayerIsDetected() {

        Player player = new Player();
        player.setId(2L);
        player.setBanned(false);

        when(playerRepository.findById(2L))
                .thenReturn(Optional.of(player));

        boolean result = playerService.isPlayerBanned(2L);

        assertFalse(result);
        verify(playerRepository).findById(2L);
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanUnbanPlayer() {

        Player player = new Player();
        player.setId(1L);
        player.setBanned(true);

        when(playerRepository.findById(1L))
                .thenReturn(Optional.of(player));

        when(playerRepository.save(any(Player.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        playerService.setBannedStatus(1L, false);

        assertFalse(player.isBanned());

        verify(playerRepository).save(player);
    }


}
