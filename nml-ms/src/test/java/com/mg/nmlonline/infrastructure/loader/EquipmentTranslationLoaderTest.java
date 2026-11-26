package com.mg.nmlonline.infrastructure.loader;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.domain.model.player.PlayerRace;
import com.mg.nmlonline.infrastructure.repository.EquipmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EmbeddedPostgresTest
@Transactional
@DisplayName("CsvDataLoader — traductions et modificateurs de visée")
class EquipmentTranslationLoaderTest {

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private CsvDataLoader csvDataLoader;

    @Test
    @DisplayName("un fichier par race alimente displayNames, chaque libellé suivant le vrai nom technique")
    void shouldLoadTranslationsByRace() {
        Equipment gaussBlaster = equipmentRepository.findByName("Pistolet-mitrailleur").orElseThrow();
        assertEquals("Shoota Dakka-Dakka", gaussBlaster.getDisplayNames().get(PlayerRace.ORKS));
        assertEquals("Éclateur gauss", gaussBlaster.getDisplayNames().get(PlayerRace.NECRONS));
        assertEquals("L'éclateur gauss arrache la matière atome par atome à chaque salve.",
                gaussBlaster.getDisplayDescriptions().get(PlayerRace.NECRONS));
        assertEquals(1, gaussBlaster.getDisplayDescriptions().size());

        Equipment nightmareShroud = equipmentRepository.findByName("Tenue légère en fibre chauffante").orElseThrow();
        assertEquals("Linceul de cauchemars", nightmareShroud.getDisplayNames().get(PlayerRace.NECRONS));
        assertEquals("Un linceul vivant qui nourrit les cauchemars de l'adversaire.",
                nightmareShroud.getDisplayDescriptions().get(PlayerRace.NECRONS));

        Equipment phylactery = equipmentRepository.findByName("Tenue ultra légère").orElseThrow();
        assertEquals("Rokette dorsale", phylactery.getDisplayNames().get(PlayerRace.ORKS));
        assertEquals("Spectres Canopteks", phylactery.getDisplayNames().get(PlayerRace.NECRONS));

        Equipment sempiternalWeave = equipmentRepository.findByName("Gilet pare-balles moyen").orElseThrow();
        assertEquals("Un exosquelette de métal vivant tissé sous la peau, qui se referme sur chaque impact.",
                sempiternalWeave.getDisplayDescriptions().get(PlayerRace.NECRONS));
    }

    @Test
    @DisplayName("le rechargement des CSV préserve les traductions d'un équipement créé par l'admin")
    void shouldPreserveAdminTranslations() {
        Equipment admin = new Equipment("Arme Admin", 100, 0, 0, 0, 0,
                Set.of(), EquipmentCategory.FIREARM);
        admin.setDisplayNames(new HashMap<>(Map.of(PlayerRace.NECRONS, "Libellé Admin")));
        admin.setDisplayDescriptions(new HashMap<>(Map.of(PlayerRace.NECRONS, "Description Admin")));
        equipmentRepository.save(admin);

        csvDataLoader.run();

        assertEquals("Libellé Admin", equipmentRepository.findByName("Arme Admin").orElseThrow()
                .getDisplayNames().get(PlayerRace.NECRONS));
        assertEquals("Description Admin", equipmentRepository.findByName("Arme Admin").orElseThrow()
                .getDisplayDescriptions().get(PlayerRace.NECRONS));
        assertEquals("Éclateur gauss", equipmentRepository.findByName("Pistolet-mitrailleur").orElseThrow()
                .getDisplayNames().get(PlayerRace.NECRONS));
    }

    @Test
    @DisplayName("les deux fusils réduisent le malus de visée, le gilet l'augmente pour l'adversaire")
    void shouldLoadSniperAimMalus() {
        Equipment shredder = equipmentRepository.findByName("Fusil de sniper lourd").orElseThrow();
        assertEquals(10, shredder.getAimMalusReduction());
        assertEquals(0, shredder.getEnemyAimMalus());

        Equipment exterminator = equipmentRepository.findByName("Fusil de sniper de combat").orElseThrow();
        assertEquals(15, exterminator.getAimMalusReduction());

        Equipment gilet = equipmentRepository.findByName("Gilet de camouflage optique").orElseThrow();
        assertEquals(0, gilet.getAimMalusReduction());
        assertEquals(15, gilet.getEnemyAimMalus());
    }

}
