package com.mg.nmlonline.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.resource.PlayerResource;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitEquipment;
import com.mg.nmlonline.infrastructure.repository.ResourceRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Service d'administration pour la gestion avancée des joueurs.
 * Permet l'import/export JSON et la suppression complète.
 */
@Service
public class AdminService {

    private final PlayerImportService playerImportService;
    private final PlayerService playerService;
    private final BoardService boardService;
    private final ResourceRepository resourceRepository;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdminService(PlayerImportService playerImportService,
                        PlayerService playerService,
                        BoardService boardService,
                        ResourceRepository resourceRepository,
                        EntityManager entityManager) {
        this.playerImportService = playerImportService;
        this.playerService = playerService;
        this.boardService = boardService;
        this.resourceRepository = resourceRepository;
        this.entityManager = entityManager;
    }

    /**
     * Importe un joueur depuis un contenu JSON.
     * Si un joueur avec le même nom existe, il est supprimé au préalable.
     */
    @Transactional
    public Player importPlayer(String jsonContent) throws IOException {
        // Écrire dans un fichier temporaire (PlayerImportService lit depuis un fichier)
        Path tempFile = Files.createTempFile("admin-import-", ".json");
        Files.writeString(tempFile, jsonContent);

        try {
            // 1. Parser pour obtenir le nom
            Player player = playerImportService.importPlayerFromJson(tempFile.toString());
            if (player == null) {
                throw new IllegalArgumentException("Impossible de parser le JSON du joueur");
            }

            // 2. Supprimer l'ancien joueur si existant
            Player existing = playerService.findByName(player.getName());
            if (existing != null) {
                playerService.delete(existing.getId());
                entityManager.flush(); // Forcer le flush pour libérer la contrainte d'unicité
            }

            // 3. Créer le nouveau joueur
            player = playerService.create(player);

            // 4. Importer les équipements
            playerImportService.importEquipmentsToPlayer(tempFile.toString(), player);
            player = playerService.save(player);

            // 5. Importer les ressources
            playerImportService.importResourcesToPlayer(tempFile.toString(), player);
            player = playerService.save(player);

            // 6. Importer les secteurs dans le board
            Board board = boardService.getAllBoards().stream().findFirst().orElse(null);
            if (board != null) {
                playerImportService.importSectorsToBoard(tempFile.toString(), player, board);
                boardService.save(board);
                player = playerService.save(player);
            }

            // Vider le cache d'équipements
            playerImportService.clearEquipmentCache();

            return player;
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * Exporte un joueur au format JSON compatible avec l'import.
     */
    public Map<String, Object> exportPlayer(Long playerId) {
        Player player = playerService.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Joueur introuvable avec l'ID " + playerId));

        Board board = boardService.getAllBoards().stream().findFirst().orElse(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", player.getName());
        result.put("money", player.getStats().getMoney());

        // Ressources : { resourceId, quantity }
        List<Map<String, Object>> resources = new ArrayList<>();
        for (PlayerResource pr : player.getResources()) {
            Map<String, Object> res = new LinkedHashMap<>();
            var dbResource = resourceRepository.findByName(pr.getResourceName());
            res.put("resourceId", dbResource.map(com.mg.nmlonline.domain.model.resource.Resource::getId).orElse(null));
            res.put("quantity", pr.getQuantity());
            resources.add(res);
        }
        result.put("resources", resources);

        // Équipements : { name, quantity }
        List<Map<String, Object>> equipments = new ArrayList<>();
        for (var stack : player.getEquipments()) {
            Map<String, Object> eq = new LinkedHashMap<>();
            eq.put("name", stack.getEquipment().getName());
            eq.put("quantity", stack.getQuantity());
            equipments.add(eq);
        }
        result.put("equipments", equipments);

        // Secteurs avec armée
        List<Map<String, Object>> sectors = new ArrayList<>();
        if (board != null) {
            for (Sector sector : board.getAllSectors()) {
                if (player.getId().equals(sector.getOwnerId())) {
                    Map<String, Object> sMap = new LinkedHashMap<>();
                    sMap.put("sectorNumber", sector.getNumber());

                    List<Map<String, Object>> army = new ArrayList<>();
                    for (Unit unit : sector.getArmy()) {
                        Map<String, Object> uMap = new LinkedHashMap<>();
                        uMap.put("id", unit.getId());
                        uMap.put("type", unit.getType().name());
                        uMap.put("classes", unit.getClasses().stream()
                                .map(Enum::name).toList());
                        uMap.put("experience", unit.getExperience());

                        // Récupérer les noms d'équipements (persistés ou transients)
                        List<String> eqNames = new ArrayList<>();
                        List<UnitEquipment> unitEquipments = unit.getUnitEquipments();
                        if (unitEquipments != null && !unitEquipments.isEmpty()) {
                            for (UnitEquipment ue : unitEquipments) {
                                eqNames.add(ue.getEquipment().getName());
                            }
                        } else if (unit.getEquipments() != null) {
                            for (Equipment eq : unit.getEquipments()) {
                                eqNames.add(eq.getName());
                            }
                        }
                        uMap.put("equipments", eqNames);

                        if (unit.isInjured()) {
                            uMap.put("isInjured", true);
                        }
                        army.add(uMap);
                    }
                    sMap.put("army", army);
                    sectors.add(sMap);
                }
            }
        }
        result.put("sectors", sectors);

        return result;
    }

    /**
     * Supprime un joueur et réinitialise tous ses secteurs.
     */
    @Transactional
    public void deletePlayer(Long playerId) {
        if (!playerService.delete(playerId)) {
            throw new IllegalArgumentException("Joueur introuvable avec l'ID " + playerId);
        }
    }
}
