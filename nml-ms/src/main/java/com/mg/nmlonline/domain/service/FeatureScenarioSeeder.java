package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.ScenarioSummaryDto;
import com.mg.nmlonline.domain.model.alliance.ProposalKind;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.resource.PlayerResource;
import com.mg.nmlonline.infrastructure.repository.AllianceRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Seeds unitaires des features hors combat : alliance déjà scellée et vente de ressources. */
@Service
public class FeatureScenarioSeeder {

    private static final String ALLIANCE_PROPOSER = "mortarion";
    private static final String ALLIANCE_PARTNER = "angron";
    private static final String SALE_PLAYER = "lurio";
    private static final String SALE_RESOURCE = "Caisse d'armes";
    private static final int SALE_QUANTITY = 5;

    private final PlayerRepository playerRepository;
    private final AllianceRepository allianceRepository;
    private final AllianceService allianceService;
    private final ResourceService resourceService;
    private final TurnService turnService;
    private final EntityManager entityManager;

    public FeatureScenarioSeeder(PlayerRepository playerRepository,
                                 AllianceRepository allianceRepository,
                                 AllianceService allianceService,
                                 ResourceService resourceService,
                                 TurnService turnService,
                                 EntityManager entityManager) {
        this.playerRepository = playerRepository;
        this.allianceRepository = allianceRepository;
        this.allianceService = allianceService;
        this.resourceService = resourceService;
        this.turnService = turnService;
        this.entityManager = entityManager;
    }

    @Transactional
    public ScenarioSummaryDto seedAllianceScenario() {
        Player proposer = resolvePlayer(ALLIANCE_PROPOSER);
        Player partner = resolvePlayer(ALLIANCE_PARTNER);
        if (allianceRepository.findActiveBetween(proposer.getId(), partner.getId()).isPresent()) {
            return summary("Alliance déjà active entre " + proposer.getName()
                    + " et " + partner.getName() + ".");
        }
        Long proposalId = allianceService
                .propose(requireUserId(proposer), ProposalKind.ALLIANCE, partner.getId())
                .getId();
        allianceService.accept(requireUserId(partner), proposalId);
        return summary("Alliance formée — " + proposer.getName() + " ↔ " + partner.getName()
                + ". L'annonce sera visible au tour suivant.");
    }

    @Transactional
    public ScenarioSummaryDto seedResourceSaleScenario() {
        Player player = resolvePlayer(SALE_PLAYER);
        PlayerResource resource = ensureResource(player);
        ResourceService.SaleResult sale = resourceService.sellResource(
                resource.getId(), SALE_QUANTITY, requireUserId(player));
        return summary("Vente seedée — " + player.getName() + " vend " + sale.quantitySold()
                + " × " + sale.resourceName() + " pour " + sale.saleValue() + " $.");
    }

    /** Le stock de démo s'épuise à force de re-seeds : on réapprovisionne le strict nécessaire. */
    private PlayerResource ensureResource(Player player) {
        PlayerResource resource = player.getResources().stream()
                .filter(candidate -> SALE_RESOURCE.equals(candidate.getResourceName()))
                .findFirst()
                .orElse(null);
        int missing = SALE_QUANTITY - (resource == null ? 0 : resource.getQuantity());
        if (missing > 0) {
            player.addResource(SALE_RESOURCE, missing);
            playerRepository.save(player);
            entityManager.flush();
        }
        return player.getResources().stream()
                .filter(candidate -> SALE_RESOURCE.equals(candidate.getResourceName()))
                .filter(candidate -> candidate.getId() != null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Ressource " + SALE_RESOURCE + " introuvable après réapprovisionnement"));
    }

    private Player resolvePlayer(String name) {
        return playerRepository.findByName(name)
                .orElseThrow(() -> new IllegalStateException(
                        "Joueur de fixture introuvable : " + name + " (import démo requis)"));
    }

    private Long requireUserId(Player player) {
        if (player.getUserId() == null) {
            throw new IllegalStateException("Le joueur " + player.getName() + " n'a pas de compte utilisateur");
        }
        return player.getUserId();
    }

    private ScenarioSummaryDto summary(String message) {
        ScenarioSummaryDto dto = new ScenarioSummaryDto();
        dto.setTurn(turnService.getCurrentTurn());
        dto.setMessage(message);
        return dto;
    }
}
