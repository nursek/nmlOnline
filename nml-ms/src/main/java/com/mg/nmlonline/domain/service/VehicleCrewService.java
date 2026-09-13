package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.unit.CombatEntity;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.infrastructure.repository.GameCharacterRepository;
import com.mg.nmlonline.infrastructure.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Les occupants restent dans les listes du secteur (stats) ; MovementService les déplace avec le véhicule.
@Service
@Transactional
public class VehicleCrewService {

    private final UnitRepository unitRepository;
    private final GameCharacterRepository characterRepository;

    public VehicleCrewService(UnitRepository unitRepository, GameCharacterRepository characterRepository) {
        this.unitRepository = unitRepository;
        this.characterRepository = characterRepository;
    }

    public CombatEntity findEntity(Long id) {
        GameCharacter character = characterRepository.findById(id).orElse(null);
        if (character != null) {
            return character;
        }
        return unitRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Combattant introuvable : " + id));
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
}
