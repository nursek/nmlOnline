package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service pour gérer les unités d'un joueur.
 * Gère les transferts, l'équipement et les opérations sur les unités.
 */
@Service
public class UnitManagementService {

    /**
     * Transfère une unité d'un secteur à un autre.
     *
     * @param unit L'unité à transférer
     * @param fromSectorNumber Numéro du secteur source
     * @param toSectorNumber Numéro du secteur destination
     * @param board Le plateau de jeu
     * @return true si le transfert a réussi
     */
    public boolean transferUnitBetweenSectors(Unit unit, int fromSectorNumber, int toSectorNumber, Board board) {
        if (unit == null || board == null) {
            return false;
        }

        Sector fromSector = board.getSector(fromSectorNumber);
        Sector toSector = board.getSector(toSectorNumber);

        if (fromSector != null && toSector != null && fromSector.removeUnit(unit)) {
            toSector.addUnit(unit);
            return true;
        }
        return false;
    }

    /**
     * Équipe une unité avec un équipement depuis l'inventaire du joueur.
     *
     * @param sectorNumber Numéro du secteur contenant l'unité
     * @param unitId ID de l'unité
     * @param equipmentName Nom de l'équipement
     * @param player Le joueur propriétaire
     * @param board Le plateau de jeu
     * @return true si l'équipement a réussi
     */
    public boolean equipToUnit(int sectorNumber, int unitId, String equipmentName, Player player, Board board) {
        Equipment equipment = player.getEquipmentByString(equipmentName);
        if (equipment == null) return false;
        return equipToUnit(sectorNumber, unitId, equipment, player, board);
    }

    /**
     * Équipe une unité avec un équipement depuis l'inventaire du joueur.
     *
     * @param sectorNumber Numéro du secteur contenant l'unité
     * @param unitId ID de l'unité
     * @param equipment L'équipement à ajouter
     * @param player Le joueur propriétaire
     * @param board Le plateau de jeu
     * @return true si l'équipement a réussi
     */
    public boolean equipToUnit(int sectorNumber, int unitId, Equipment equipment, Player player, Board board) {
        if (player == null || board == null || equipment == null) {
            return false;
        }

        // Trouver le secteur
        Sector sector = board.getSector(sectorNumber);
        if (sector == null) return false;

        // Vérifier que le secteur appartient au joueur
        if (!sector.getOwnerId().equals(player.getId())) {
            System.out.println("Le secteur n'appartient pas au joueur");
            return false;
        }

        // Trouver l'unité
        Unit unit = sector.getUnitById(unitId);
        if (unit == null) return false;

        // Vérifier la disponibilité de l'équipement
        if (player.isEquipmentUnavailable(equipment)) {
            System.out.println("Équipement non disponible : " + equipment.getName());
            return false;
        }

        // Équiper l'unité
        boolean equipped = unit.addEquipment(equipment);
        if (equipped) {
            player.decrementEquipmentAvailability(equipment);
            player.setTotalEquipmentValue();
            System.out.println("✓ " + equipment.getName() + " équipé sur " + unit.getType().name() + " n°" + unit.getNumber());
            return true;
        }

        System.out.println("Impossible d'équiper : " + equipment.getName());
        return false;
    }

    /**
     * Remplace un équipement d'une unité par un nouveau.
     *
     * @param unit L'unité dont on veut changer l'équipement
     * @param oldEquipment L'équipement à retirer
     * @param newEquipment Le nouvel équipement à ajouter
     * @param player Le joueur propriétaire
     * @return true si le remplacement a réussi
     */
    public boolean replaceEquipment(Unit unit, Equipment oldEquipment, Equipment newEquipment, Player player) {
        if (unit == null || newEquipment == null || player == null) {
            return false;
        }

        // Vérifier que le nouvel équipement est disponible
        if (player.isEquipmentUnavailable(newEquipment)) {
            System.out.println("Équipement non disponible : " + newEquipment.getName());
            return false;
        }

        // Si un ancien équipement est spécifié, le retirer d'abord
        if (oldEquipment != null) {
            boolean removed = unit.removeEquipment(oldEquipment);
            if (removed) {
                player.incrementEquipmentAvailability(oldEquipment);
                System.out.println("Équipement retiré : " + oldEquipment.getName());
            } else {
                System.out.println("Impossible de retirer l'équipement : " + oldEquipment.getName());
                return false;
            }
        }

        // Équiper le nouvel équipement
        boolean equipped = unit.addEquipment(newEquipment);
        if (equipped) {
            player.decrementEquipmentAvailability(newEquipment);
            player.setTotalEquipmentValue();
            System.out.println("Nouvel équipement ajouté : " + newEquipment.getName());
            return true;
        } else {
            // Si l'équipement échoue, remettre l'ancien si on l'avait retiré
            if (oldEquipment != null) {
                unit.addEquipment(oldEquipment);
                player.decrementEquipmentAvailability(oldEquipment);
            }
            System.out.println("Impossible d'équiper : " + newEquipment.getName());
            return false;
        }
    }

    /**
     * Remplace automatiquement un équipement de même catégorie.
     *
     * @param unit L'unité dont on veut changer l'équipement
     * @param newEquipment Le nouvel équipement à ajouter
     * @param player Le joueur propriétaire
     * @return true si le remplacement a réussi
     */
    public boolean replaceEquipmentByCategory(Unit unit, Equipment newEquipment, Player player) {
        if (unit == null || newEquipment == null || player == null) {
            return false;
        }

        EquipmentCategory category = newEquipment.getCategory();

        // Vérifier si l'unité a atteint la limite pour cette catégorie
        long currentCount = unit.countEquipmentsByCategory(category);
        int maxAllowed = switch (category) {
            case FIREARM -> unit.getType().getMaxFirearms();
            case MELEE -> unit.getType().getMaxMeleeWeapons();
            case DEFENSIVE -> unit.getType().getMaxDefensiveEquipment();
        };

        // Si la limite est atteinte, retirer le premier équipement de cette catégorie
        Equipment oldEquipment = null;
        if (currentCount >= maxAllowed) {
            List<Equipment> equipmentsOfCategory = unit.getEquipmentsByCategory(category);
            if (!equipmentsOfCategory.isEmpty()) {
                oldEquipment = equipmentsOfCategory.getFirst();
            }
        }

        return replaceEquipment(unit, oldEquipment, newEquipment, player);
    }

    /**
     * Retourne la liste des équipements compatibles avec une unité donnée.
     *
     * @param unit L'unité pour laquelle vérifier la compatibilité
     * @param player Le joueur propriétaire
     * @return Liste des équipements compatibles disponibles
     */
    public List<Equipment> getCompatibleEquipments(Unit unit, Player player) {
        if (unit == null || player == null) {
            return List.of();
        }

        return player.getEquipments().stream()
                .filter(stack -> stack.getAvailable() > 0)
                .map(stack -> stack.getEquipment())
                .filter(unit::canEquip)
                .toList();
    }

    /**
     * Retourne les équipements compatibles filtrés par catégorie.
     *
     * @param unit L'unité pour laquelle vérifier
     * @param category La catégorie d'équipement recherchée
     * @param player Le joueur propriétaire
     * @return Liste des équipements compatibles de cette catégorie
     */
    public List<Equipment> getCompatibleEquipmentsByCategory(Unit unit, EquipmentCategory category, Player player) {
        if (unit == null || category == null || player == null) {
            return List.of();
        }

        return player.getEquipments().stream()
                .filter(stack -> stack.getAvailable() > 0)
                .map(stack -> stack.getEquipment())
                .filter(eq -> eq.getCategory() == category)
                .filter(unit::canEquip)
                .toList();
    }
}

