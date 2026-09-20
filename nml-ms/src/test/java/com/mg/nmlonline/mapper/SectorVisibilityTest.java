package com.mg.nmlonline.mapper;

import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Visibilité secteur : détail réservé au propriétaire et à l'allié avec troupe sur place")
class SectorVisibilityTest {

    private static final Long ME = 1L;
    private static final Long ALLY = 2L;

    private Sector sectorOwnedBy(Long ownerId) {
        Sector sector = new Sector(1, "Secteur");
        sector.setOwnerId(ownerId);
        return sector;
    }

    private void addUnit(Sector sector, Long playerId) {
        Unit unit = new Unit(5.0, UnitClass.ELEMENTAIRE);
        unit.setPlayerId(playerId);
        unit.setSector(sector);
        sector.getArmy().add(unit);
    }

    private SectorMapper.Visibility visibility() {
        return new SectorMapper.Visibility(ME, Set.of(ALLY));
    }

    @Test
    @DisplayName("Propriétaire : détail complet")
    void ownerSeesDetail() {
        assertTrue(visibility().canSeeDetail(sectorOwnedBy(ME)));
    }

    @Test
    @DisplayName("Allié avec troupe sur place : détail complet")
    void allyWithTroopSeesDetail() {
        Sector sector = sectorOwnedBy(ALLY);
        addUnit(sector, ME);
        assertTrue(visibility().canSeeDetail(sector));
    }

    @Test
    @DisplayName("Allié sans troupe : vue restreinte")
    void allyWithoutTroopRestricted() {
        assertFalse(visibility().canSeeDetail(sectorOwnedBy(ALLY)));
    }

    @Test
    @DisplayName("Non-allié avec troupe : vue restreinte")
    void nonAllyWithTroopRestricted() {
        Sector sector = sectorOwnedBy(3L);
        addUnit(sector, ME);
        assertFalse(visibility().canSeeDetail(sector));
    }

    @Test
    @DisplayName("Secteur neutre avec troupe : vue restreinte")
    void neutralWithTroopRestricted() {
        Sector sector = sectorOwnedBy(null);
        addUnit(sector, ME);
        assertFalse(visibility().canSeeDetail(sector));
    }
}
