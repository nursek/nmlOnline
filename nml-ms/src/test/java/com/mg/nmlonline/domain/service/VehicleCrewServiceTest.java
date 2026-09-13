package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
import com.mg.nmlonline.infrastructure.repository.GameCharacterRepository;
import com.mg.nmlonline.infrastructure.repository.UnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("VehicleCrewService — pilote + passagers sans quitter le secteur")
class VehicleCrewServiceTest {

    @Mock
    UnitRepository unitRepository;

    @Mock
    GameCharacterRepository characterRepository;

    @InjectMocks
    VehicleCrewService service;

    private static final Long JOUEUR = 1L;
    private static final Long PILOTE_ID = 201L;
    private static final Long AUTRE_ID = 202L;
    private static final Long PILOTE_2_ID = 203L;
    private static final Long PASSAGER_2_ID = 204L;
    private static final Long PERSONNAGE_ID = 301L;

    private Sector secteur;
    private Vehicle vehicule;
    private Unit pilote;
    private Unit autre;
    private Unit pilote2;
    private GameCharacter personnage;

    @BeforeEach
    void setUp() {
        Board board = new Board();
        secteur = new Sector(3, "Base");
        board.addSector(secteur);

        vehicule = new Vehicle(VehicleType.VTT_LEGER, JOUEUR);
        vehicule.setId(50L);
        vehicule.setSector(secteur);
        secteur.getVehicles().add(vehicule);

        pilote = unite(PILOTE_ID, UnitClass.PILOTE_DESTRUCTEUR);
        autre = unite(AUTRE_ID, UnitClass.LEGER);
        pilote2 = unite(PILOTE_2_ID, UnitClass.PILOTE_DESTRUCTEUR);
        unite(PASSAGER_2_ID, UnitClass.LEGER);

        personnage = new GameCharacter("Capitaine", 10, 10, 10, 10, 0, 0);
        personnage.setId(PERSONNAGE_ID);
        personnage.setPlayerId(JOUEUR);
        personnage.setSector(secteur);
        secteur.getCharacters().add(personnage);

        lenient().when(unitRepository.findById(PILOTE_ID)).thenReturn(Optional.of(pilote));
        lenient().when(unitRepository.findById(AUTRE_ID)).thenReturn(Optional.of(autre));
        lenient().when(unitRepository.findById(PILOTE_2_ID)).thenReturn(Optional.of(pilote2));
        lenient().when(characterRepository.findById(PERSONNAGE_ID)).thenReturn(Optional.of(personnage));
    }

    private Unit unite(Long id, UnitClass classe) {
        Unit u = new Unit(5.0, classe);
        u.setId(id);
        u.setPlayerId(JOUEUR);
        u.setSector(secteur);
        secteur.getArmy().add(u);
        return u;
    }

    @Test
    @DisplayName("Pilote classe P + passager sans quitter l'armée du secteur")
    void shouldBoardPilotAndPassengerWithoutLeavingSector() {
        service.applyCrew(vehicule, PILOTE_ID, List.of(AUTRE_ID));

        assertSame(pilote, vehicule.getPilot());
        assertEquals(1, vehicule.getPassengerCount());
        assertTrue(secteur.getArmy().contains(pilote), "Le pilote reste compté dans le secteur");
        assertTrue(secteur.getArmy().contains(autre), "Le passager reste compté dans le secteur");
        assertSame(secteur, autre.getSector());
    }

    @Test
    @DisplayName("Un personnage peut être pilote")
    void shouldBoardCharacterPilot() {
        service.applyCrew(vehicule, PERSONNAGE_ID, List.of(autre.getId()));

        assertSame(personnage, vehicule.getPilot());
        assertEquals(1, vehicule.getPassengerCount());
        assertTrue(secteur.getCharacters().contains(personnage));
    }

    @Test
    @DisplayName("Une unité sans classe P ne peut pas être pilote")
    void shouldRejectUnitWithoutPilotClass() {
        IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class,
                () -> service.applyCrew(vehicule, AUTRE_ID, List.of()));

        assertTrue(erreur.getMessage().contains("pilote"));
    }

    @Test
    @DisplayName("Capacité du type de véhicule respectée")
    void shouldRejectOverCapacity() {
        Vehicle tourelle = new Vehicle(VehicleType.TOURELLE, JOUEUR);
        tourelle.setId(51L);
        tourelle.setSector(secteur);

        assertThrows(IllegalArgumentException.class,
                () -> service.applyCrew(tourelle, null, List.of(AUTRE_ID, PASSAGER_2_ID)));
    }

    @Test
    @DisplayName("Remplacer l'équipage ne modifie pas les unités du secteur")
    void shouldReplaceCrewWithoutTouchingUnits() {
        service.applyCrew(vehicule, PILOTE_ID, List.of(AUTRE_ID));
        service.applyCrew(vehicule, PILOTE_2_ID, List.of(PILOTE_ID));

        assertSame(pilote2, vehicule.getPilot());
        assertEquals(1, vehicule.getPassengerCount());
        assertSame(pilote, vehicule.getPassengers().getFirst());
        assertTrue(secteur.getArmy().contains(pilote));
        assertTrue(secteur.getArmy().contains(autre));
        assertTrue(secteur.getArmy().contains(pilote2));
    }
}
