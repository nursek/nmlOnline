package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.domain.service.PlayerImportService.PlayerDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

import static org.junit.jupiter.api.Assertions.*;

/** Non-régression : les sections character et buildings (historiquement absentes) doivent être relisibles par l'import. */
@EmbeddedPostgresTest
@DisplayName("AdminService — Export character & buildings")
class AdminExportImportTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private PlayerRepository playerRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("export produit une section character et buildings, relisible par l'import")
    void exportCharacterAndBuildingsRoundTrip() throws Exception {
        Player nursek = playerRepository.findByName("nursek")
                .orElseThrow(() -> new AssertionError("Le joueur nursek doit être importé au démarrage (profil test)"));

        Map<String, Object> exported = adminService.exportPlayer(nursek.getId());

        Object characterObj = exported.get("character");
        assertNotNull(characterObj, "La section character doit être exportée");
        assertInstanceOf(Map.class, characterObj);
        @SuppressWarnings("unchecked")
        Map<String, Object> character = (Map<String, Object>) characterObj;
        assertEquals("Ratcatcher", character.get("name"));
        assertTrue(character.containsKey("sectorNumber"), "La section character doit porter sectorNumber");
        assertEquals(2, ((Number) character.get("sectorNumber")).intValue(),
                "Le personnage doit être affecté au secteur 2");
        assertEquals(300.0, ((Number) character.get("baseAttack")).doubleValue());
        assertEquals(250.0, ((Number) character.get("baseDefense")).doubleValue());
        assertEquals(100.0, ((Number) character.get("basePdf")).doubleValue());
        assertEquals(0.0, ((Number) character.get("basePdc")).doubleValue());
        assertEquals(0.0, ((Number) character.get("baseArmor")).doubleValue());
        assertEquals(0.0, ((Number) character.get("baseEvasion")).doubleValue());

        Object buildingsObj = exported.get("buildings");
        assertNotNull(buildingsObj, "La section buildings doit être exportée");
        assertInstanceOf(List.class, buildingsObj);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> buildings = (List<Map<String, Object>>) buildingsObj;
        assertEquals(3, buildings.size());
        var types = buildings.stream().map(b -> b.get("type")).toList();
        assertTrue(types.contains("HEADQUARTERS"));
        assertTrue(types.contains("WEAPON_CACHE"));
        assertTrue(types.contains("BANK"));
        for (Map<String, Object> b : buildings) {
            assertTrue(b.containsKey("sectorNumber"), "Chaque bâtiment doit porter sectorNumber");
            assertEquals(2, ((Number) b.get("sectorNumber")).intValue(),
                    "Chaque bâtiment doit être affecté au secteur 2");
        }

        String json = objectMapper.writeValueAsString(exported);
        PlayerDTO dto = new ObjectMapper().readValue(json, PlayerDTO.class);
        assertEquals("nursek", dto.name);
        assertNotNull(dto.character, "L'import doit retrouver la section character");
        @SuppressWarnings("unchecked")
        Map<String, Object> charParsed = objectMapper.convertValue(dto.character, Map.class);
        assertEquals("Ratcatcher", charParsed.get("name"));
        assertEquals(300.0, ((Number) charParsed.get("baseAttack")).doubleValue());
        assertEquals(250.0, ((Number) charParsed.get("baseDefense")).doubleValue());
        assertNotNull(dto.buildings, "L'import doit retrouver la section buildings");
        assertEquals(3, dto.buildings.size());
    }
}
