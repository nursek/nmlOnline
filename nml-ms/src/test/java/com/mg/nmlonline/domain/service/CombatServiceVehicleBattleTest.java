package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.domain.model.equipment.VehicleBonusTarget;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
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

import static org.junit.jupiter.api.Assertions.*;

@EmbeddedPostgresTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("CombatService — véhicules combattants")
class CombatServiceVehicleBattleTest {

    @Autowired
    private CombatService combatService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private PlatformTransactionManager txManager;

    private record World(Long attackerId, Long defenderId, int sectorNumber, Long vehicleId, Long pilotId) {
    }

    private World seed() {
        return new TransactionTemplate(txManager).execute(status -> {
            Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
            Sector sector = board.getAllSectors().stream()
                    .filter(s -> s.isNeutral() && s.getArmySize() == 0
                            && s.getBuildings().isEmpty() && s.getCharacters().isEmpty()
                            && s.getVehicles().isEmpty())
                    .findFirst().orElseThrow();

            Player attacker = new Player("AttaquantVeh");
            playerRepository.save(attacker);
            Player defender = new Player("DefenseurVeh");
            playerRepository.save(defender);
            em.flush();

            Equipment gauss = new Equipment("Gauss Cannon (test)", 3400, 80, 0, 0, 0,
                    Set.of(UnitClass.PILOTE_DESTRUCTEUR), EquipmentCategory.FIREARM);
            gauss.setVehicleBonus(100);
            gauss.setVehicleBonusTarget(VehicleBonusTarget.GROUND);
            em.persist(gauss);

            Unit shooter = new Unit(5.0, UnitClass.PILOTE_DESTRUCTEUR);
            shooter.setPlayerId(attacker.getId());
            shooter.addEquipment(gauss);
            sector.addUnit(shooter);

            Equipment armor = new Equipment("Armure pilote", 100, 0, 0, 200, 0,
                    Set.of(UnitClass.PILOTE_DESTRUCTEUR), EquipmentCategory.DEFENSIVE);
            em.persist(armor);
            Unit pilot = new Unit(8.0, UnitClass.PILOTE_DESTRUCTEUR);
            pilot.setPlayerId(defender.getId());
            pilot.addEquipment(armor);

            Vehicle vehicle = new Vehicle(VehicleType.VTT_LEGER, defender.getId());
            vehicle.setSector(sector);
            em.persist(vehicle);
            sector.getVehicles().add(vehicle);
            vehicle.assignPilot(pilot);
            sector.addUnit(pilot);

            em.flush();
            return new World(attacker.getId(), defender.getId(), sector.getNumber(),
                    vehicle.getId(), pilot.getId());
        });
    }

    @Test
    @DisplayName("Véhicule détruit : épave conservée, équipage débarqué et vivant, perte de type VEHICLE")
    void destroyedVehicleLeavesWreckAndDisembarksCrew() {
        World w = seed();

        CombatService.SectorBattleResult r = new TransactionTemplate(txManager).execute(status -> {
            Player attacker = playerRepository.findById(w.attackerId()).orElseThrow();
            Player defender = playerRepository.findById(w.defenderId()).orElseThrow();
            Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
            return combatService.simulateSectorBattle(List.of(attacker), List.of(defender), board, w.sectorNumber());
        });

        assertTrue(r.success());
        assertEquals(1, r.defenderCasualties().size(), "Seule l'épave est perdue côté défenseur : le pilote survit");
        assertInstanceOf(Vehicle.class, r.defenderCasualties().getFirst());
        assertTrue(r.casualtyDetails().stream()
                .anyMatch(casualty -> "VEHICLE".equals(casualty.category())
                        && "VTT léger".equals(casualty.label())));

        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            Vehicle vehicle = em.find(Vehicle.class, w.vehicleId());
            assertNotNull(vehicle, "L'épave reste en base (pas de DELETE)");
            assertTrue(vehicle.isDestroyed());
            assertNull(vehicle.getPilot(), "Le pilote est détaché de l'épave");
            assertEquals(0.0, vehicle.getDefense());

            Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
            Sector sector = board.getSector(w.sectorNumber());
            assertTrue(sector.getVehicles().stream()
                    .anyMatch(v -> v.getId().equals(w.vehicleId())), "L'épave reste dans le secteur");

            Unit pilot = em.find(Unit.class, w.pilotId());
            assertNotNull(pilot, "Le pilote débarqué survit");
            assertFalse(pilot.isDestroyed());
            assertTrue(sector.getUnits().stream().anyMatch(u -> u.getId().equals(w.pilotId())),
                    "Le pilote reste une unité du secteur");
        });
    }
}
