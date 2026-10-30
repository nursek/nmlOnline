package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.CombatEntity;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.infrastructure.repository.GameCharacterRepository;
import com.mg.nmlonline.infrastructure.repository.MovementOrderRepository;
import com.mg.nmlonline.infrastructure.repository.UnitRepository;
import com.mg.nmlonline.infrastructure.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

// Les occupants restent dans les listes du secteur (stats) ; MovementService les déplace avec le véhicule.
@Service
@Transactional
public class VehicleCrewService {

    private final UnitRepository unitRepository;
    private final GameCharacterRepository characterRepository;
    private final VehicleRepository vehicleRepository;
    private final MovementOrderRepository movementOrderRepository;
    private final TurnService turnService;

    public VehicleCrewService(UnitRepository unitRepository,
                              GameCharacterRepository characterRepository,
                              VehicleRepository vehicleRepository,
                              MovementOrderRepository movementOrderRepository,
                              TurnService turnService) {
        this.unitRepository = unitRepository;
        this.characterRepository = characterRepository;
        this.vehicleRepository = vehicleRepository;
        this.movementOrderRepository = movementOrderRepository;
        this.turnService = turnService;
    }

    public CombatEntity findEntity(Long id) {
        GameCharacter character = characterRepository.findById(id).orElse(null);
        if (character != null) {
            return character;
        }
        return unitRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Combattant introuvable : " + id));
    }

    /** {@code false} quand l'équipage demandé est celui déjà en place : aucun journal ni action à enregistrer. */
    public boolean updateCrew(Player player, Vehicle vehicle, Long pilotId, List<Long> passengerIds,
                              Long prevPilotId, List<Long> prevPassengerIds) {
        List<Long> passengers = passengerIds != null ? passengerIds : List.of();
        if (passengers.stream().distinct().count() != passengers.size()) {
            throw new IllegalArgumentException("Un même passager est présent deux fois.");
        }
        if (pilotId != null && passengers.contains(pilotId)) {
            throw new IllegalArgumentException("Le pilote ne peut pas être passager.");
        }
        if (passengers.size() > vehicle.getCapacity()) {
            throw new IllegalArgumentException(
                    "Capacité du véhicule dépassée (" + vehicle.getCapacity() + " passagers).");
        }
        if (Objects.equals(prevPilotId, pilotId)
                && new HashSet<>(prevPassengerIds).equals(new HashSet<>(passengers))) {
            return false;
        }

        List<Long> targeted = new ArrayList<>(passengers);
        if (pilotId != null) {
            targeted.add(pilotId);
        }
        validateCrewCandidates(player, vehicle, targeted, prevPilotId, prevPassengerIds);

        applyCrew(vehicle, pilotId, passengers);
        return true;
    }

    public void applyCrew(Vehicle vehicle, Long pilotId, List<Long> passengerIds) {
        vehicle.disembarkAll();

        if (pilotId != null) {
            CombatEntity pilot = findEntity(pilotId);
            if (pilot.isDestroyed()) {
                throw new IllegalStateException("Le pilote #" + pilotId + " est détruit.");
            }
            if (!vehicle.assignPilot(pilot)) {
                throw new IllegalArgumentException("L'unité #" + pilotId + " n'a pas la classe pilote (P).");
            }
        }

        for (Long passengerId : passengerIds) {
            CombatEntity passenger = findEntity(passengerId);
            if (passenger.isDestroyed()) {
                throw new IllegalStateException("Le passager #" + passengerId + " est détruit.");
            }
            if (!vehicle.embark(passenger)) {
                throw new IllegalArgumentException(
                        "Capacité du véhicule dépassée (" + vehicle.getCapacity() + " passagers).");
            }
        }
    }

    private void validateCrewCandidates(Player player, Vehicle vehicle, List<Long> ids,
                                        Long prevPilotId, List<Long> prevPassengerIds) {
        if (ids.isEmpty()) {
            return;
        }
        List<Long> alreadyOrdered = movementOrderRepository
                .findPendingEntityIds(turnService.getCurrentTurn(), ids);
        if (!alreadyOrdered.isEmpty()) {
            throw new IllegalArgumentException(
                    "Entité(s) déjà engagée(s) dans un ordre en attente : " + alreadyOrdered + ".");
        }
        Set<Long> currentCrew = new HashSet<>(prevPassengerIds);
        if (prevPilotId != null) {
            currentCrew.add(prevPilotId);
        }
        Sector sector = vehicle.getSector();
        for (Long id : ids) {
            CombatEntity entity = findEntity(id);
            if (!player.getId().equals(entity.getPlayerId())) {
                throw new SecurityException("L'entité #" + id + " n'appartient pas au joueur.");
            }
            if (entity.isDestroyed()) {
                throw new IllegalStateException("L'entité #" + id + " est détruite.");
            }
            if (!currentCrew.contains(id)) {
                if (vehicleRepository.findByPilot_Id(id)
                        .filter(other -> !Objects.equals(other.getId(), vehicle.getId()))
                        .isPresent()) {
                    throw new IllegalStateException("L'entité #" + id + " pilote déjà un autre véhicule.");
                }
                if (vehicleRepository.findByPassengers_Id(id)
                        .filter(other -> !Objects.equals(other.getId(), vehicle.getId()))
                        .isPresent()) {
                    throw new IllegalStateException("L'entité #" + id + " est déjà dans un autre véhicule.");
                }
            }
            boolean inVehicleSector = entity.getSector() != null
                    && entity.getSector().getNumber() == sector.getNumber()
                    && entity.getSector().getBoard() != null
                    && Objects.equals(entity.getSector().getBoard().getId(), sector.getBoard().getId());
            if (!inVehicleSector) {
                throw new IllegalArgumentException(
                        "L'entité #" + id + " n'est pas dans le secteur " + sector.getNumber() + ".");
            }
        }
    }
}
