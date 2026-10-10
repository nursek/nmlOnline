package com.mg.nmlonline;

import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;

@EmbeddedPostgresTest
@DisplayName("Verrouillage optimiste")
class OptimisticLockingTest {

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private PlayerRepository playerRepository;

    @Test
    @DisplayName("Enregistrer un Player avec une version périmée est rejeté")
    void stalePlayerSaveIsRejected() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        Long playerId = tx.execute(status ->
                playerRepository.findByName("TestPlayer1").orElseThrow().getId());
        Player stale = tx.execute(status -> playerRepository.findById(playerId).orElseThrow());

        tx.executeWithoutResult(status -> {
            Player current = playerRepository.findById(playerId).orElseThrow();
            current.setRankingComment("première écriture");
        });

        assertThrows(ObjectOptimisticLockingFailureException.class, () ->
                tx.executeWithoutResult(status -> {
                    stale.setRankingComment("écriture périmée");
                    playerRepository.save(stale);
                }));
    }
}
