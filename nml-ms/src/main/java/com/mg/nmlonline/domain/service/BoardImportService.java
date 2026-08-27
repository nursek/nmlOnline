package com.mg.nmlonline.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.unit.UnitType;
import com.mg.nmlonline.infrastructure.repository.EquipmentRepository;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service pour importer un Board depuis un fichier JSON
 */
@Service
public class BoardImportService {

    private static final Logger logger = LoggerFactory.getLogger(BoardImportService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EquipmentRepository equipmentRepository;

    // Cache d'Equipment pour éviter les requêtes multiples
    private final Map<String, Equipment> equipmentCache = new HashMap<>();

    public BoardImportService(EquipmentRepository equipmentRepository) {
        this.equipmentRepository = equipmentRepository;
    }

    /**
     * Importe un Board depuis un contenu JSON.
     * @Transactional(readOnly) : la session reste ouverte pour Hibernate.initialize
     * sur les Equipment chargés via getEquipmentByName (privé, appelé en self-invocation).
     */
    @Transactional(readOnly = true)
    public Board importBoardFromJson(String jsonContent) throws IOException {
        BoardDTO dto = objectMapper.readValue(jsonContent, BoardDTO.class);
        return importBoard(dto);
    }

    private Board importBoard(BoardDTO dto) {
        Board board = new Board();

        // Importer les métadonnées de la carte
        if (dto.name != null) {
            board.setName(dto.name);
        }
        if (dto.mapImageUrl != null) {
            board.setMapImageUrl(dto.mapImageUrl);
        }
        if (dto.svgOverlayUrl != null) {
            board.setSvgOverlayUrl(dto.svgOverlayUrl);
        }

        if (dto.sectors != null && !dto.sectors.isEmpty()) {
            for (SectorDTO sectorDto : dto.sectors) {
                // Vérifier si le secteur existe déjà
                Sector sector = board.getSector(sectorDto.number);

                if (sector == null) {
                    // Créer un nouveau secteur
                    sector = new Sector(sectorDto.number, sectorDto.name);
                    board.addSector(sector);
                } else {
                    // Mettre à jour le secteur existant
                    sector.setName(sectorDto.name);
                }

                sector.setIncome(sectorDto.income);

                // Ajouter les coordonnées si présentes
                if (sectorDto.x != null) {
                    sector.setX(sectorDto.x);
                }
                if (sectorDto.y != null) {
                    sector.setY(sectorDto.y);
                }

                // Ajouter la ressource si présente
                if (sectorDto.resource != null && !sectorDto.resource.isEmpty()) {
                    sector.setResourceName(sectorDto.resource);
                }

                // Importer les unités
                // Note: On ne peut pas clear() car getArmy() peut retourner une liste non modifiable
                // On ajoute simplement les unités (le secteur peut déjà avoir des unités)
                if (sectorDto.army != null) {
                    for (UnitDTO unitDto : sectorDto.army) {
                        Unit unit = createUnitFromDTO(unitDto);
                        if (unit != null) { // Only add unit if it was created successfully
                            sector.addUnit(unit);
                        }
                    }
                }

                // Ajouter les voisins
                // Note: On ne peut pas clear() car getNeighbors() retourne une liste non modifiable
                // On ajoute simplement les voisins
                if (sectorDto.neighbors != null) {
                    for (Integer neighbor : sectorDto.neighbors) {
                        sector.addNeighbor(neighbor);
                    }
                }
            }
        }

        return board;
    }

    /**
     * Crée une Unit à partir des champs communs des DTOs d'import (classes, type, expérience).
     * Retourne null si aucune classe n'est fournie (une unité sans classe est inutilisable).
     * Si type est null, le type déduit de l'expérience par le constructeur est conservé.
     */
    public static Unit createUnit(List<String> classes, UnitType type, double experience) {
        if (classes == null || classes.isEmpty()) {
            return null;
        }

        Unit unit = new Unit(experience, UnitClass.valueOf(classes.getFirst()));
        if (type != null) {
            unit.setType(type);
        }

        // Ajouter la deuxième classe si présente
        if (classes.size() > 1) {
            unit.addSecondClass(UnitClass.valueOf(classes.get(1)));
        }

        return unit;
    }

    private Unit createUnitFromDTO(UnitDTO unitDto) {
        Unit unit = createUnit(unitDto.classes, unitDto.type, unitDto.experience);
        if (unit == null) {
            logger.warn("Impossible de créer une unité sans classe - unité ignorée");
            return null;
        }

        // Ajouter les équipements (depuis la BDD, pas de création)
        if (unitDto.equipments != null) {
            for (String equipmentName : unitDto.equipments) {
                Equipment equipment = getEquipmentByName(equipmentName);
                if (equipment != null) {
                    unit.addEquipment(equipment);
                }
            }
        }

        return unit;
    }

    /**
     * Récupère un Equipment depuis le cache ou la BDD.
     * Les Equipment sont pré-chargés via CSV, on ne crée jamais de nouveaux Equipment ici.
     */
    private Equipment getEquipmentByName(String equipmentName) {
        // 1. Vérifier le cache
        if (equipmentCache.containsKey(equipmentName)) {
            return equipmentCache.get(equipmentName);
        }

        // 2. Chercher en BDD
        Optional<Equipment> existingEquipment = equipmentRepository.findByName(equipmentName);
        if (existingEquipment.isPresent()) {
            Equipment eq = existingEquipment.get();
            // compatibleClasses est LAZY : on l'initialise maintenant (session ouverte)
            // car les Equipment sont mis en cache et réutilisés hors session (imports multi-étapes).
            Hibernate.initialize(eq.getCompatibleClasses());
            equipmentCache.put(equipmentName, eq);
            return eq;
        }

        // L'equipment n'existe pas - c'est une erreur
        logger.warn("Équipement '{}' non trouvé en BDD (vérifier data.sql)", equipmentName);
        return null;
    }

    // ===== DTOs pour Jackson =====

    public static class BoardDTO {
        public String name;
        public String mapImageUrl;
        public String svgOverlayUrl;
        public List<SectorDTO> sectors;
    }

    public static class SectorDTO {
        public int number;
        public String name;
        public double income;
        public String resource;
        public List<UnitDTO> army;
        public List<Integer> neighbors;
        public Integer x;
        public Integer y;
    }

    public static class UnitDTO {
        public int id;
        public com.mg.nmlonline.domain.model.unit.UnitType type;
        public List<String> classes;
        public double experience;
        public List<String> equipments;
    }
}

