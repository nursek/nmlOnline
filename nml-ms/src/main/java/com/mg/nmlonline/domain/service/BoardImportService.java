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

@Service
public class BoardImportService {

    private static final Logger logger = LoggerFactory.getLogger(BoardImportService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EquipmentRepository equipmentRepository;

    private final Map<String, Equipment> equipmentCache = new HashMap<>();

    public BoardImportService(EquipmentRepository equipmentRepository) {
        this.equipmentRepository = equipmentRepository;
    }

    /** Session readOnly ouverte pour Hibernate.initialize sur les Equipment (self-invocation de getEquipmentByName). */
    @Transactional(readOnly = true)
    public Board importBoardFromJson(String jsonContent) throws IOException {
        BoardDTO dto = objectMapper.readValue(jsonContent, BoardDTO.class);
        return importBoard(dto);
    }

    private Board importBoard(BoardDTO dto) {
        Board board = new Board();

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
                Sector sector = board.getSector(sectorDto.number);

                if (sector == null) {
                    sector = new Sector(sectorDto.number, sectorDto.name);
                    board.addSector(sector);
                } else {
                    sector.setName(sectorDto.name);
                }

                sector.setIncome(sectorDto.income);

                if (sectorDto.x != null) {
                    sector.setX(sectorDto.x);
                }
                if (sectorDto.y != null) {
                    sector.setY(sectorDto.y);
                }

                if (sectorDto.resource != null && !sectorDto.resource.isEmpty()) {
                    sector.setResourceName(sectorDto.resource);
                }

                // Pas de clear() : getArmy() peut retourner une liste non modifiable ; on ajoute seulement.
                if (sectorDto.army != null) {
                    for (UnitDTO unitDto : sectorDto.army) {
                        Unit unit = createUnitFromDTO(unitDto);
                        if (unit != null) {
                            sector.addUnit(unit);
                        }
                    }
                }

                // Pas de clear() : getNeighbors() retourne une liste non modifiable ; on ajoute seulement.
                if (sectorDto.neighbors != null) {
                    for (Integer neighbor : sectorDto.neighbors) {
                        sector.addNeighbor(neighbor);
                    }
                }
            }
        }

        return board;
    }

    /** Retourne null si aucune classe fournie ; si type est null, conserve celui déduit de l'expérience par le constructeur. */
    public static Unit createUnit(List<String> classes, UnitType type, double experience) {
        if (classes == null || classes.isEmpty()) {
            return null;
        }

        Unit unit = new Unit(experience, UnitClass.valueOf(classes.getFirst()));
        if (type != null) {
            unit.setType(type);
        }

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

        // Équipements : lookup BDD uniquement, pas de création.
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

    /** Lookup cache/BDD (Equipment pré-chargés via CSV, jamais créés ici). */
    private Equipment getEquipmentByName(String equipmentName) {
        if (equipmentCache.containsKey(equipmentName)) {
            return equipmentCache.get(equipmentName);
        }

        Optional<Equipment> existingEquipment = equipmentRepository.findByName(equipmentName);
        if (existingEquipment.isPresent()) {
            Equipment eq = existingEquipment.get();
            // compatibleClasses est LAZY : initialiser maintenant car l'Equipment est mis en cache et réutilisé hors session.
            Hibernate.initialize(eq.getCompatibleClasses());
            equipmentCache.put(equipmentName, eq);
            return eq;
        }

        logger.warn("Équipement '{}' non trouvé en BDD (vérifier data.sql)", equipmentName);
        return null;
    }

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

