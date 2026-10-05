package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.building.Building;
import com.mg.nmlonline.domain.model.building.BuildingType;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.infrastructure.repository.BuildingRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.infrastructure.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Place les unités non déployées au QG à la fin du tour.
 *
 * <p>Volontairement sans dépendance à {@link TurnService} : appelé par lui, une
 * dépendance croisée créerait un cycle Spring. QG détruit/capturé = partie perdue,
 * aucun repli n'est prévu.
 */
@Service
public class ReserveUnitPlacer {

    private final UnitRepository unitRepository;
    private final BuildingRepository buildingRepository;
    private final PlayerRepository playerRepository;

    public ReserveUnitPlacer(UnitRepository unitRepository, BuildingRepository buildingRepository,
                             PlayerRepository playerRepository) {
        this.unitRepository = unitRepository;
        this.buildingRepository = buildingRepository;
        this.playerRepository = playerRepository;
    }

    @Transactional
    public int placeAllAtHeadquarters() {
        int placed = 0;
        // Verrou joueur AVANT de lire ses unités : sinon un placement concurrent est écrasé par un snapshot périmé.
        for (Long playerId : unitRepository.findPlayerIdsWithReserveUnits().stream().sorted().toList()) {
            playerRepository.findByIdForUpdate(playerId);
            Sector headquarters = findHeadquartersSector(playerId);
            if (headquarters == null) {
                continue;
            }
            for (Unit unit : unitRepository.findByPlayerIdAndSectorIsNull(playerId)) {
                headquarters.addUnit(unit);
                placed++;
            }
        }
        return placed;
    }

    private Sector findHeadquartersSector(Long playerId) {
        return buildingRepository
                .findByPlayerIdAndBuildingTypeAndIsDestroyedFalse(playerId, BuildingType.HEADQUARTERS)
                .stream()
                .filter(building -> !building.isCaptured())
                .map(Building::getSector)
                .filter(sector -> sector != null)
                .findFirst()
                .orElse(null);
    }
}
