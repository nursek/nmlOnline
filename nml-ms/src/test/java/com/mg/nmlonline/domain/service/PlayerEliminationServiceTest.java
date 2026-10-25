package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.domain.model.alliance.Alliance;
import com.mg.nmlonline.domain.model.alliance.AllianceProposal;
import com.mg.nmlonline.domain.model.alliance.PendingCapture;
import com.mg.nmlonline.domain.model.alliance.ProposalKind;
import com.mg.nmlonline.domain.model.alliance.ProposalStatus;
import com.mg.nmlonline.domain.model.bank.ExchangeOffer;
import com.mg.nmlonline.domain.model.bank.ExchangeOfferStatus;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.building.Bank;
import com.mg.nmlonline.domain.model.building.Headquarters;
import com.mg.nmlonline.domain.model.building.WeaponCache;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import com.mg.nmlonline.domain.model.player.EliminationCause;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
import com.mg.nmlonline.infrastructure.repository.AllianceProposalRepository;
import com.mg.nmlonline.infrastructure.repository.AllianceRepository;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.ExchangeOfferRepository;
import com.mg.nmlonline.infrastructure.repository.PendingCaptureRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EmbeddedPostgresTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("PlayerEliminationService — conséquences territoriales et matérielles")
class PlayerEliminationServiceTest {

    private static final int HQ_SECTOR = 43;
    private static final int OTHER_SECTOR = 38;

    @Autowired
    private PlayerEliminationService eliminationService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private AllianceRepository allianceRepository;

    @Autowired
    private AllianceProposalRepository proposalRepository;

    @Autowired
    private ExchangeOfferRepository exchangeOfferRepository;

    @Autowired
    private PendingCaptureRepository pendingCaptureRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private PlatformTransactionManager txManager;

    private record World(Long victimId, Long killerId, Long allyId, Long allianceId,
                         Long proposalId, Long offerId) {
    }

    private World seed(EliminationCause cause) {
        return new TransactionTemplate(txManager).execute(status -> {
            Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
            Player victim = new Player("VictimeElim");
            Player killer = new Player("TueurElim");
            Player ally = new Player("AllieElim");
            playerRepository.saveAll(List.of(victim, killer, ally));
            em.flush();

            Sector hqSector = board.getSector(HQ_SECTOR);
            Sector otherSector = board.getSector(OTHER_SECTOR);
            assertTrue(hqSector.isNeutral() && otherSector.isNeutral());
            hqSector.setOwnerAndColor(victim.getId(), "#111111");
            otherSector.setOwnerAndColor(victim.getId(), "#111111");

            Headquarters hq = new Headquarters(victim.getId());
            Bank hqBank = new Bank(victim.getId());
            WeaponCache cache = new WeaponCache(victim.getId());
            Bank otherBank = new Bank(victim.getId());
            for (var building : List.of(hq, hqBank, cache)) {
                building.setSector(hqSector);
                victim.getBuildings().add(building);
                hqSector.getBuildings().add(building);
            }
            otherBank.setSector(otherSector);
            victim.getBuildings().add(otherBank);
            otherSector.getBuildings().add(otherBank);

            Equipment cachedEquipment = new Equipment("Butin cache", 50, 0, 0, 0, 0,
                    Set.of(UnitClass.TIREUR), EquipmentCategory.FIREARM);
            em.persist(cachedEquipment);
            EquipmentStack cacheStack = new EquipmentStack(cachedEquipment);
            cacheStack.setPlayer(victim);
            cache.getStoredEquipments().add(cacheStack);
            hqBank.setStoredMoney(0);

            Unit hqUnit = new Unit(0.0, UnitClass.TIREUR);
            hqUnit.setPlayerId(victim.getId());
            hqSector.addUnit(hqUnit);
            Unit otherUnit = new Unit(0.0, UnitClass.TIREUR);
            otherUnit.setPlayerId(victim.getId());
            otherSector.addUnit(otherUnit);

            Sector transitSector = board.getSector(36);
            Unit transitUnit = new Unit(8.0, UnitClass.TIREUR);
            transitUnit.setPlayerId(victim.getId());
            transitSector.addUnit(transitUnit);

            Vehicle vehicle = new Vehicle(VehicleType.VTT_LEGER, victim.getId());
            vehicle.setSector(hqSector);
            hqSector.getVehicles().add(vehicle);
            em.persist(vehicle);
            Unit pilot = new Unit(0.0, UnitClass.PILOTE_DESTRUCTEUR);
            pilot.setPlayerId(victim.getId());
            hqSector.addUnit(pilot);
            vehicle.assignPilot(pilot);

            Equipment equipment = new Equipment("Arme élimination", 100, 0, 0, 0, 0,
                    Set.of(UnitClass.TIREUR), EquipmentCategory.FIREARM);
            em.persist(equipment);
            victim.addEquipmentToStack(equipment, 1);
            victim.addResource("Auramite", 5);
            victim.getStats().setMoney(1000.0);
            victim.getStats().setStartingMoneyRemaining(50.0);
            victim.getStats().setTotalIncome(500.0);
            victim.getStats().setTotalAtk(100.0);
            victim.getStats().setGlobalPower(999.0);
            victim.getStats().setTotalDef(250.0);
            victim.getStats().setTotalEquipmentValue(400.0);
            victim.getStats().setTotalVehiclesValue(300.0);
            victim.eliminate(cause, 1, null);
            em.flush();

            Alliance alliance = allianceRepository.save(Alliance.create(victim.getId(), ally.getId(), 1));
            AllianceProposal proposal = proposalRepository.save(AllianceProposal.create(
                    ProposalKind.ALLIANCE, victim.getId(), ally.getId(), 1));
            ExchangeOffer offer = exchangeOfferRepository.save(new ExchangeOffer(
                    victim.getId(), ally.getId(), 100.0, 1, 2));
            pendingCaptureRepository.save(PendingCapture.create(board.getId(), HQ_SECTOR, 1,
                    List.of(victim.getId(), ally.getId())));
            pendingCaptureRepository.save(PendingCapture.create(board.getId(), 39, 1,
                    List.of(victim.getId())));
            em.flush();
            return new World(victim.getId(), killer.getId(), ally.getId(), alliance.getId(),
                    proposal.getId(), offer.getId());
        });
    }

    private void apply(World world, EliminationCause cause, Long killerId) {
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
            Player victim = playerRepository.findById(world.victimId()).orElseThrow();
            victim.eliminate(cause, 1, null);
            eliminationService.apply(board, victim, killerId, 1);
        });
    }

    @Test
    @DisplayName("QG capturé : tous les secteurs au tueur, bâtiments du QG pillés puis supprimés")
    void headquartersCaptureTransfersAllSectorsAndLootsHeadquartersBuildings() {
        World world = seed(EliminationCause.HEADQUARTERS_CAPTURED);

        apply(world, EliminationCause.HEADQUARTERS_CAPTURED, world.killerId());

        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            em.clear();
            Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
            assertEquals(world.killerId(), board.getSector(HQ_SECTOR).getOwnerId());
            assertEquals(world.killerId(), board.getSector(OTHER_SECTOR).getOwnerId());
            assertTrue(board.getSectorsByOwner(world.victimId()).isEmpty());
            assertTrue(board.getSector(HQ_SECTOR).getBuildings().isEmpty(),
                    "QG, Banque et Cache du secteur du QG sont supprimés après pillage");
            assertTrue(board.getSector(OTHER_SECTOR).getBuildings().isEmpty(),
                    "Les bâtiments hors secteur du QG s'évaporent");
            assertTrue(board.getSector(HQ_SECTOR).getUnits().isEmpty());
            assertTrue(board.getSector(HQ_SECTOR).getVehicles().isEmpty());
            assertTrue(board.getSector(OTHER_SECTOR).getUnits().isEmpty());
            assertTrue(board.getSector(36).getUnits().isEmpty(),
                    "Une unité en transit est retirée de son secteur");
            assertEquals(0.0, board.getSector(36).getStats().getTotalAtk(),
                    "Les statistiques du secteur de transit sont recalculées");

            Player victim = playerRepository.findById(world.victimId()).orElseThrow();
            assertTrue(victim.isEliminated());
            assertEquals(world.killerId(), victim.getEliminatedByPlayerId());
            assertTrue(victim.getEquipments().isEmpty());
            assertTrue(victim.getResources().isEmpty());
            assertEquals(0.0, victim.getStats().getMoney());
            assertEquals(0.0, victim.getStats().getTotalIncome());
            assertEquals(0.0, victim.getStats().getTotalAtk());
            assertEquals(0.0, victim.getStats().getGlobalPower());
            assertEquals(0.0, victim.getStats().getTotalDef());
            assertEquals(0.0, victim.getStats().getTotalEquipmentValue());
            assertEquals(0.0, victim.getStats().getTotalVehiclesValue());
            assertEquals(0.0, victim.getStats().getStartingMoneyRemaining());
            assertTrue(victim.getBuildings().isEmpty());

            Player killer = playerRepository.findById(world.killerId()).orElseThrow();
            assertEquals(750.0, killer.getStats().getMoney(), 0.001,
                    "La banque du QG est pillée avant suppression");
            assertTrue(killer.getEquipments().stream()
                            .anyMatch(stack -> "Butin cache".equals(stack.getEquipment().getName())
                                    && stack.getQuantity() == 1),
                    "La cache du QG est pillée avant suppression");

            assertFalse(allianceRepository.findById(world.allianceId()).orElseThrow().isActive());
            assertEquals(ProposalStatus.WITHDRAWN,
                    proposalRepository.findById(world.proposalId()).orElseThrow().getStatus());
            assertEquals(ExchangeOfferStatus.CANCELLED,
                    exchangeOfferRepository.findById(world.offerId()).orElseThrow().getStatus());
            PendingCapture capture = pendingCaptureRepository.findByResolvedFalseOrderByIdAsc().stream()
                    .filter(pending -> pending.getSectorNumber() == HQ_SECTOR)
                    .findFirst()
                    .orElseThrow();
            assertFalse(capture.candidateIds().contains(world.victimId()),
                    "Le joueur éliminé est retiré des candidats de capture");
            assertTrue(capture.candidateIds().contains(world.allyId()),
                    "Les autres candidats de la capture alliée sont conservés");
            assertTrue(pendingCaptureRepository.findByResolvedFalseOrderByIdAsc().stream()
                            .noneMatch(pending -> pending.getSectorNumber() == 39),
                    "Une capture dont l'éliminé était l'unique candidat est supprimée");
        });
    }

    @Test
    @DisplayName("Tueur éliminé dans le même tour : aucun secteur ne lui revient")
    void eliminatedKillerDoesNotInheritSectors() {
        World world = seed(EliminationCause.MAIN_CHARACTER_KILLED);
        new TransactionTemplate(txManager).executeWithoutResult(status ->
                playerRepository.findById(world.killerId()).orElseThrow()
                        .eliminate(EliminationCause.MAIN_CHARACTER_KILLED, 1));

        apply(world, EliminationCause.MAIN_CHARACTER_KILLED, world.killerId());

        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            em.clear();
            Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
            assertNull(board.getSector(HQ_SECTOR).getOwnerId(),
                    "Un tueur éliminé ne récupère pas le secteur du QG");
            assertNull(board.getSector(OTHER_SECTOR).getOwnerId());
        });
    }

    @Test
    @DisplayName("Personnage tué : le secteur du QG revient au tueur même s'il appartenait à un tiers")
    void mainCharacterDeathGrantsHeadquartersSectorEvenWhenUnowned() {
        World world = seed(EliminationCause.MAIN_CHARACTER_KILLED);
        new TransactionTemplate(txManager).executeWithoutResult(status -> boardRepository.findAll().stream()
                .findFirst().orElseThrow().getSector(HQ_SECTOR)
                .setOwnerAndColor(world.allyId(), "#222222"));

        apply(world, EliminationCause.MAIN_CHARACTER_KILLED, world.killerId());

        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            em.clear();
            Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
            assertEquals(world.killerId(), board.getSector(HQ_SECTOR).getOwnerId(),
                    "Le tueur récupère le secteur du QG même s'il n'était pas celui de la victime");
            assertTrue(board.getSector(HQ_SECTOR).getBuildings().isEmpty());
            assertNull(board.getSector(OTHER_SECTOR).getOwnerId());
        });
    }

    @Test
    @DisplayName("Personnage tué : le tueur récupère le secteur du QG nu, les autres secteurs sont neutralisés")
    void mainCharacterDeathTransfersOnlyBareHeadquartersSector() {
        World world = seed(EliminationCause.MAIN_CHARACTER_KILLED);

        apply(world, EliminationCause.MAIN_CHARACTER_KILLED, world.killerId());

        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            em.clear();
            Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
            assertEquals(world.killerId(), board.getSector(HQ_SECTOR).getOwnerId());
            assertNull(board.getSector(OTHER_SECTOR).getOwnerId());
            assertTrue(board.getSector(HQ_SECTOR).getBuildings().isEmpty(),
                    "Le secteur du QG est récupéré nu");
            assertTrue(board.getSector(OTHER_SECTOR).getBuildings().isEmpty());
            assertTrue(board.getSector(HQ_SECTOR).getUnits().isEmpty());
            assertTrue(board.getSector(HQ_SECTOR).getVehicles().isEmpty());
            assertTrue(board.getSector(OTHER_SECTOR).getUnits().isEmpty());

            Player victim = playerRepository.findById(world.victimId()).orElseThrow();
            assertEquals(world.killerId(), victim.getEliminatedByPlayerId());
            assertTrue(victim.getBuildings().isEmpty());
        });
    }

    @Test
    @DisplayName("Personnage tué sans tueur désigné : tous les secteurs de la victime deviennent neutres")
    void mainCharacterDeathWithoutKillerNeutralizesAllSectors() {
        World world = seed(EliminationCause.MAIN_CHARACTER_KILLED);

        apply(world, EliminationCause.MAIN_CHARACTER_KILLED, null);

        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            em.clear();
            Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
            assertNull(board.getSector(HQ_SECTOR).getOwnerId());
            assertNull(board.getSector(OTHER_SECTOR).getOwnerId());
            assertTrue(board.getSector(HQ_SECTOR).getBuildings().isEmpty());
            assertNull(playerRepository.findById(world.victimId()).orElseThrow()
                    .getEliminatedByPlayerId());
        });
    }
}
