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
@DisplayName("CsvDataLoader — traductions d'équipement")
class EquipmentTranslationLoaderTest {

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private CsvDataLoader csvDataLoader;

    @Test
    @DisplayName("un fichier par race alimente displayNames, chaque libellé suivant le vrai nom technique")
    void shouldLoadTranslationsByRace() {
        Equipment gaussBlaster = equipmentRepository.findByName("Gauss Blaster").orElseThrow();
        assertEquals("Shoota Dakka-Dakka", gaussBlaster.getDisplayNames().get(PlayerRace.ORKS));
        assertEquals("Éclateur gauss", gaussBlaster.getDisplayNames().get(PlayerRace.NECRONS));
        assertEquals("L'éclateur gauss arrache la matière atome par atome à chaque salve.",
                gaussBlaster.getDisplayDescriptions().get(PlayerRace.NECRONS));
        assertEquals(1, gaussBlaster.getDisplayDescriptions().size());

        Equipment nightmareShroud = equipmentRepository.findByName("Nightmare Shroud").orElseThrow();
        assertEquals("Linceul de cauchemars", nightmareShroud.getDisplayNames().get(PlayerRace.NECRONS));
        assertEquals("Un linceul vivant qui nourrit les cauchemars de l'adversaire.",
                nightmareShroud.getDisplayDescriptions().get(PlayerRace.NECRONS));

        Equipment phylactery = equipmentRepository.findByName("Phylactery").orElseThrow();
        assertEquals("Guenilles renforcées", phylactery.getDisplayNames().get(PlayerRace.ORKS));
        assertEquals("Spectres Canopteks", phylactery.getDisplayNames().get(PlayerRace.NECRONS));
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
        assertEquals("Éclateur gauss", equipmentRepository.findByName("Gauss Blaster").orElseThrow()
                .getDisplayNames().get(PlayerRace.NECRONS));
    }
}
