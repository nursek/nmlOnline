package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.api.dto.AllianceMeDto;
import com.mg.nmlonline.api.dto.AllianceProposalDto;
import com.mg.nmlonline.domain.model.alliance.ProposalKind;
import com.mg.nmlonline.domain.model.building.Headquarters;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.BuildingRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EmbeddedPostgresTest
@DisplayName("AllianceService — cycle de vie, annonces différées, trahison")
class AllianceServiceTest {

    private static final AtomicLong USER_SEQ = new AtomicLong(700_000_000L);

    @Autowired
    private AllianceService allianceService;

    @Autowired
    private BuildingService buildingService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private TurnService turnService;

    @Autowired
    private AllianceGraph allianceGraph;

    private Player createPlayer(String name) {
        Player player = new Player(name);
        player.setUserId(USER_SEQ.incrementAndGet());
        player = playerRepository.save(player);
        buildingService.createInitialBuildings(player);
        return playerRepository.save(player);
    }

    private Player createPlayerWithoutHeadquarters(String name) {
        Player player = new Player(name);
        player.setUserId(USER_SEQ.incrementAndGet());
        return playerRepository.save(player);
    }

    private AllianceProposalDto ally(Player first, Player second) {
        AllianceProposalDto proposal = allianceService.propose(first.getUserId(), ProposalKind.ALLIANCE, second.getId());
        allianceService.accept(second.getUserId(), proposal.getId());
        return proposal;
    }

    @Test
    @DisplayName("Acceptation : alliance active immédiatement, annonce publique seulement au tour suivant")
    void allianceFormedIsAnnouncedNextTurn() {
        Player a = createPlayer("AllianceA" + USER_SEQ.get());
        Player b = createPlayer("AllianceB" + USER_SEQ.get());
        ally(a, b);

        AllianceMeDto me = allianceService.getMe(a.getUserId());
        assertEquals(1, me.getAlliances().size());
        assertEquals(b.getId(), me.getAlliances().getFirst().getAllyPlayerId());
        assertTrue(me.isHeadquartersOperational());

        assertTrue(allianceService.getAnnouncements().stream()
                        .noneMatch(announcement -> announcement.getActorPlayerId().equals(a.getId())),
                "Annonce invisible le tour de l'acceptation");

        turnService.advanceTurn();

        assertTrue(allianceService.getAnnouncements().stream()
                        .anyMatch(announcement -> "ALLIANCE_FORMED".equals(announcement.getType())
                                && announcement.getActorPlayerId().equals(a.getId())
                                && announcement.getTargetPlayerId().equals(b.getId())),
                "Annonce visible au tour suivant");
    }

    @Test
    @DisplayName("Trahison : bonus 15 % + 10 %/tour, actif uniquement le tour de la trahison, sans plafond")
    void betrayalBonusActiveOnlyOnBetrayalTurn() {
        Player traitor = createPlayer("Traitre" + USER_SEQ.get());
        Player victim = createPlayer("Victime" + USER_SEQ.get());
        ally(traitor, victim);
        int turn = turnService.getCurrentTurn();

        assertEquals(0, allianceGraph.betrayalBonusPercent(turn, traitor.getId(), victim.getId()));

        Long allianceId = allianceService.getMe(traitor.getUserId()).getAlliances().getFirst().getId();
        allianceService.betray(traitor.getUserId(), allianceId);

        assertEquals(15, allianceGraph.betrayalBonusPercent(turn, traitor.getId(), victim.getId()));
        assertFalse(allianceGraph.areAllied(traitor.getId(), victim.getId()));
        assertTrue(allianceService.getMe(traitor.getUserId()).getAlliances().isEmpty());
        assertEquals(15, allianceService.getMe(traitor.getUserId()).getActiveBetrayalBonuses().getFirst().getBonusPercent());

        int nextTurn = turnService.advanceTurn();
        assertEquals(0, allianceGraph.betrayalBonusPercent(nextTurn, traitor.getId(), victim.getId()),
                "Le bonus s'éteint avec le tour de la trahison");
        assertThrows(IllegalStateException.class,
                () -> allianceService.betray(traitor.getUserId(), allianceId));
    }

    @Test
    @DisplayName("Rupture d'un commun accord : fin à l'acceptation, sans bonus de trahison")
    void mutualRuptureEndsAllianceWithoutBonus() {
        Player a = createPlayer("RuptureA" + USER_SEQ.get());
        Player b = createPlayer("RuptureB" + USER_SEQ.get());
        ally(a, b);
        int turn = turnService.getCurrentTurn();

        AllianceProposalDto rupture = allianceService.propose(a.getUserId(), ProposalKind.RUPTURE, b.getId());
        assertTrue(allianceService.getMe(b.getUserId()).getIncomingProposals().stream()
                .anyMatch(proposal -> "RUPTURE".equals(proposal.getKind())));
        allianceService.accept(b.getUserId(), rupture.getId());

        assertTrue(allianceService.getMe(a.getUserId()).getAlliances().isEmpty());
        assertEquals(0, allianceGraph.betrayalBonusPercent(turn, a.getId(), b.getId()));
        assertEquals(0, allianceGraph.betrayalBonusPercent(turn, b.getId(), a.getId()));
    }

    @Test
    @DisplayName("Sans QG opérationnel, proposer une alliance est refusé")
    void proposalRequiresHeadquarters() {
        Player noHeadquarters = createPlayerWithoutHeadquarters("SansQg" + USER_SEQ.get());
        Player target = createPlayer("CibleQg" + USER_SEQ.get());

        assertThrows(IllegalStateException.class,
                () -> allianceService.propose(noHeadquarters.getUserId(), ProposalKind.ALLIANCE, target.getId()));
    }

    @Test
    @DisplayName("Rupture amiable : accepter exige un QG opérationnel")
    void ruptureAcceptanceRequiresHeadquarters() {
        Player requester = createPlayer("RuptureQgA" + USER_SEQ.get());
        Player accepter = createPlayer("RuptureQgB" + USER_SEQ.get());
        ally(requester, accepter);

        Headquarters headquarters = buildingService.getHeadquarters(accepter.getId()).orElseThrow();
        headquarters.setOperational(false);
        buildingRepository.save(headquarters);

        AllianceProposalDto rupture = allianceService.propose(
                requester.getUserId(), ProposalKind.RUPTURE, accepter.getId());
        assertThrows(IllegalStateException.class,
                () -> allianceService.accept(accepter.getUserId(), rupture.getId()));
    }
}
