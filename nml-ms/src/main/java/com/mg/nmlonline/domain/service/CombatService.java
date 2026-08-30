package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.building.Building;
import com.mg.nmlonline.domain.model.building.BuildingType;
import com.mg.nmlonline.domain.model.building.Headquarters;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.CombatEntity;
import com.mg.nmlonline.domain.model.unit.EntityCategory;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.battle.Battle;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CombatService {

    private static final Logger logger = LoggerFactory.getLogger(CombatService.class);

    @Autowired
    private PlayerStatsService playerStatsService;

    @Autowired
    private BuildingService buildingService;

    @Autowired
    private EntityManager em;

    public Optional<Sector> findSectorWithArmy(Player player, Board board) {
        if (player == null || board == null) {
            return Optional.empty();
        }

        List<Sector> sectorsWithArmy = playerStatsService.getSectorsWithCombatEntities(player, board);
        return sectorsWithArmy.stream().findFirst();
    }

    /**
     * Combat sur un secteur donné : unités + personnages + bâtiments des deux camps co-localisés
     * (véhicules exclus, bâtiments détruits/capturés neutralisés). Pertes unités supprimées via
     * em.remove (Sector.army sans orphanRemoval — docs/jpa-pitfalls.md §1 + V6__sector_army_fk_cascade.sql) ;
     * personnages supprimés via Player.character orphanRemoval ; bâtiments marqués détruits, jamais DELETE.
     */
    public SectorBattleResult simulateSectorBattle(Player attacker, Player defender, Board board, int sectorNumber) {
        if (attacker == null || defender == null || board == null) {
            return failedResult("Paramètres invalides");
        }
        Sector sector = board.getSector(sectorNumber);
        if (sector == null) {
            return failedResult("Secteur inexistant : " + sectorNumber);
        }

        playerStatsService.updateCombatStats(attacker, board);
        playerStatsService.updateCombatStats(defender, board);

        // Ordre [personnages, QG, Cache, Banque, unités] : getLast() fait mourir les unités d'abord,
        // puis Banque → Cache → QG ; le personnage ne tombe qu'en dernier (sa chute = fin de partie).
        List<CombatEntity> attackerFighters = collectBattleParticipants(sector, attacker.getId());
        List<CombatEntity> defenderFighters = collectBattleParticipants(sector, defender.getId());

        if (attackerFighters.isEmpty() || defenderFighters.isEmpty()) {
            return failedResult("Aucune entité combattante au secteur " + sectorNumber
                    + " (attaquant=" + attackerFighters.size() + ", défenseur=" + defenderFighters.size() + ")");
        }

        // Snapshot des IDs avant combat pour identifier les pertes après coup.
        Set<Long> beforeIds = new HashSet<>();
        attackerFighters.forEach(u -> beforeIds.add(u.getId()));
        defenderFighters.forEach(u -> beforeIds.add(u.getId()));

        Battle battle = new Battle();
        battle.classicCombatConfiguration(attacker, defender, attackerFighters, defenderFighters);

        // Réconciliation : les pertes (retirées des listes de travail par Battle) sortent de sector.getArmy() pour em.remove. Survivants = mêmes références, stats déjà mutées.
        Set<Long> survivorIds = new HashSet<>();
        attackerFighters.forEach(u -> survivorIds.add(u.getId()));
        defenderFighters.forEach(u -> survivorIds.add(u.getId()));

        List<CombatEntity> casualties = new ArrayList<>();
        boolean attackerCharacterLost = false;
        boolean defenderCharacterLost = false;

        for (Unit unit : new ArrayList<>(sector.getUnits())) {
            if (isCasualty(unit, beforeIds, survivorIds)) {
                // Sector.army sans orphanRemoval (docs/jpa-pitfalls.md §1, V6) : em.remove cascade vers Unit.unitEquipments (cascade=ALL) → DELETE propre. Retrait mémoire pour cohérence de sector.getUnits().
                em.remove(unit);
                sector.getUnits().remove(unit);
                casualties.add(unit);
            }
        }

        for (GameCharacter character : new ArrayList<>(sector.getCharacters())) {
            if (isCasualty(character, beforeIds, survivorIds)) {
                detachCharacterFromVehicles(character);
                Player owner = attacker.getId().equals(character.getPlayerId()) ? attacker : defender;
                // player_id/character_id : détacher le personnage (FK players.character_id nulle d'abord, sinon FK bloque le DELETE).
                if (owner.getCharacter() != null && character.getId().equals(owner.getCharacter().getId())) {
                    owner.setCharacter(null); // @OneToOne orphanRemoval → DELETE au flush
                } else {
                    em.remove(character); // personnage non porté par Player.character
                }
                sector.getCharacters().remove(character);
                casualties.add(character);
                if (owner == attacker) {
                    attackerCharacterLost = true;
                } else {
                    defenderCharacterLost = true;
                }
            }
        }

        for (Building building : new ArrayList<>(sector.getBuildings())) {
            if (isCasualty(building, beforeIds, survivorIds)) {
                // Jamais DELETE : reste en BDD et dans sector.getBuildings(), stacks/ressources préservés,
                // Player.buildings intact (orphanRemoval — docs/jpa-pitfalls.md).
                if (building instanceof Headquarters headquarters) {
                    // QG détruit : hors service (armée immobilisée) ; reconstruction 75k si non capturé.
                    headquarters.destroy();
                } else {
                    building.setDestroyed(true);
                    building.recalculateBaseStats();
                }
                casualties.add(building);
            }
        }

        // Régénération post-bataille : bâtiments survivants reprennent leurs stats de base (annule le
        // reassign-zéro ; QG → 200). Personnages : offense/soak restaurés SANS soigner la défense
        // (régén +50 uniquement en fin de tour). Les unités gardent le comportement historique.
        for (CombatEntity entity : attackerFighters) {
            regenerateAfterBattle(entity);
        }
        for (CombatEntity entity : defenderFighters) {
            regenerateAfterBattle(entity);
        }

        sector.recalculateMilitaryPower();

        // Capture réservée à la victoire de l'attaquant, secteur par secteur.
        int capturedBuildings = 0;
        boolean defenderHeadquartersCaptured = false;
        if (battle.getWinner() != null && battle.getWinner().getId().equals(attacker.getId())) {
            int turn = board.getCurrentTurn();
            for (Building building : sector.getBuildings()) {
                if (!defender.getId().equals(building.getPlayerId()) || building.isCaptured()) {
                    continue;
                }
                switch (building.getBuildingType()) {
                    // Marqué capturé même détruit (arbitrage MJ) ; jamais captureHeadquarters.
                    case HEADQUARTERS -> {
                        building.onCapture(attacker.getId(), turn);
                        defenderHeadquartersCaptured = true;
                        capturedBuildings++;
                    }
                    // Un bâtiment détruit n'est pas capturable : seuls les intacts transfèrent.
                    case BANK -> {
                        if (!building.isDestroyed()) {
                            buildingService.captureBank(building.getId(), attacker.getId(), turn);
                            capturedBuildings++;
                        }
                    }
                    case WEAPON_CACHE -> {
                        if (!building.isDestroyed()) {
                            buildingService.captureWeaponCache(building.getId(), attacker.getId(), turn);
                            capturedBuildings++;
                        }
                    }
                }
            }
        }

        List<CombatEntity> attackerCasualties = casualties.stream()
                .filter(u -> attacker.getId().equals(u.getPlayerId()))
                .collect(Collectors.toList());
        List<CombatEntity> defenderCasualties = casualties.stream()
                .filter(u -> defender.getId().equals(u.getPlayerId()))
                .collect(Collectors.toList());
        List<CombatEntity> attackerInjured = attackerFighters.stream()
                .filter(CombatEntity::isInjured)
                .collect(Collectors.toList());
        List<CombatEntity> defenderInjured = defenderFighters.stream()
                .filter(CombatEntity::isInjured)
                .collect(Collectors.toList());

        logger.info("[Combat secteur {}] {} vs {}: {} pertes attaquant, {} pertes défenseur, {} bâtiments capturés, vainqueur: {}",
                sectorNumber, attacker.getName(), defender.getName(),
                attackerCasualties.size(), defenderCasualties.size(), capturedBuildings,
                battle.getWinner() != null ? battle.getWinner().getName() : "aucun");

        return new SectorBattleResult(true, "Bataille terminée au secteur " + sectorNumber,
                attackerCasualties, defenderCasualties, attackerInjured, defenderInjured, battle.getWinner(),
                capturedBuildings, attackerCharacterLost, defenderCharacterLost, defenderHeadquartersCaptured);
    }

    /** Filtrage par joueur ; véhicules exclus (listes unités/personnages/bâtiments uniquement). */
    private List<CombatEntity> collectBattleParticipants(Sector sector, Long playerId) {
        List<CombatEntity> fighters = new ArrayList<>();

        // Le personnage d'abord dans la liste = dernière cible de getLast().
        sector.getCharacters().stream()
                .filter(c -> playerId.equals(c.getPlayerId()))
                .filter(c -> !c.isDestroyed())
                .forEach(fighters::add);

        List<Building> buildings = sector.getBuildings().stream()
                .filter(b -> playerId.equals(b.getPlayerId()))
                .filter(b -> !b.isDestroyed() && !b.isCaptured())
                .toList();
        for (BuildingType type : List.of(BuildingType.HEADQUARTERS, BuildingType.WEAPON_CACHE, BuildingType.BANK)) {
            buildings.stream().filter(b -> b.getBuildingType() == type).forEach(fighters::add);
        }

        sector.getUnits().stream()
                .filter(u -> playerId.equals(u.getPlayerId()))
                .filter(u -> !u.isDestroyed())
                .forEach(fighters::add);

        return fighters;
    }

    private boolean isCasualty(CombatEntity entity, Set<Long> beforeIds, Set<Long> survivorIds) {
        return beforeIds.contains(entity.getId()) && !survivorIds.contains(entity.getId());
    }

    private void regenerateAfterBattle(CombatEntity entity) {
        if (entity.getEntityCategory() == EntityCategory.BUILDING) {
            entity.recalculateBaseStats();
        } else if (entity.getEntityCategory() == EntityCategory.CHARACTER) {
            ((GameCharacter) entity).regenerateAfterBattle();
        }
    }

    /** Détacher le personnage de tout véhicule (pilote ou passager) — sinon les FK pilot_id/vehicle_id bloquent le DELETE. */
    private void detachCharacterFromVehicles(GameCharacter character) {
        List<Vehicle> carrying = em.createQuery(
                        "select distinct v from Vehicle v left join v.passengers p " +
                                "where v.pilot.id = :characterId or p.id = :characterId", Vehicle.class)
                .setParameter("characterId", character.getId())
                .getResultList();
        for (Vehicle vehicle : carrying) {
            if (vehicle.getPilot() != null && character.getId().equals(vehicle.getPilot().getId())) {
                vehicle.removePilot();
            }
            vehicle.disembark(character);
        }
    }

    private SectorBattleResult failedResult(String message) {
        return new SectorBattleResult(false, message, List.of(), List.of(), List.of(), List.of(), null,
                0, false, false, false);
    }

        public record BattleResult(boolean success, String message, Player winner) {
            public BattleResult(boolean success, String message) {
                this(success, message, null);
            }

    }

        public record SectorBattleResult(boolean success, String message, List<CombatEntity> attackerCasualties,
                                         List<CombatEntity> defenderCasualties, List<CombatEntity> attackerInjured,
                                         List<CombatEntity> defenderInjured, Player winner, int capturedBuildings,
                                         boolean attackerCharacterLost, boolean defenderCharacterLost,
                                         boolean defenderHeadquartersCaptured) {

    }
}
