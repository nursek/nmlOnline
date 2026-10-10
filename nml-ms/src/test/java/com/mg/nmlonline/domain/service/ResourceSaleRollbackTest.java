package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.api.dto.SellResourceBatchItemDto;
import com.mg.nmlonline.config.TestDataInitializer;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.resource.PlayerResource;
import com.mg.nmlonline.domain.model.user.User;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@EmbeddedPostgresTest
@DisplayName("Vente groupée de ressources — atomicité")
class ResourceSaleRollbackTest {

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager txManager;

    @Test
    @DisplayName("Item invalide : la vente précédente est annulée (argent et stock inchangés)")
    void invalidItemRollsBackWholeBatch() {
        TransactionTemplate tx = new TransactionTemplate(txManager);

        User user = userRepository.findByUsername(TestDataInitializer.USER_1);
        assertNotNull(user);
        Long userId = user.getId();

        Long playerId = tx.execute(status -> {
            Player player = playerRepository.findByUserId(userId).orElseThrow();
            player.addResource("Or", 5);
            return playerRepository.save(player).getId();
        });

        PlayerResource or = tx.execute(status -> playerRepository.findById(playerId).orElseThrow()
                .getResources().stream()
                .filter(r -> "Or".equals(r.getResourceName()))
                .findFirst().orElseThrow());
        double moneyBefore = tx.execute(status -> playerRepository.findById(playerId).orElseThrow()
                .getStats().getMoney());
        int stockBefore = or.getQuantity();

        SellResourceBatchItemDto valid = new SellResourceBatchItemDto();
        valid.setPlayerResourceId(or.getId());
        valid.setQuantity(2);
        SellResourceBatchItemDto invalid = new SellResourceBatchItemDto();
        invalid.setPlayerResourceId(999_999L);
        invalid.setQuantity(1);

        try {
            assertThrows(RuntimeException.class,
                    () -> resourceService.sellResourcesBatch(userId, List.of(valid, invalid)));

            tx.executeWithoutResult(status -> {
                Player reloaded = playerRepository.findById(playerId).orElseThrow();
                assertEquals(moneyBefore, reloaded.getStats().getMoney(), 0.001,
                        "La vente valide ne doit pas être créditée si la seconde échoue");
                int stockAfter = reloaded.getResources().stream()
                        .filter(r -> "Or".equals(r.getResourceName()))
                        .mapToInt(PlayerResource::getQuantity)
                        .findFirst().orElse(0);
                assertEquals(stockBefore, stockAfter, "Le stock vendu doit être rendu par le rollback");
            });
        } finally {
            tx.executeWithoutResult(status -> playerRepository.findById(playerId).orElseThrow()
                    .removeResource("Or", 5));
        }
    }
}
