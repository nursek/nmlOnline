package com.mg.nmlonline.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.board.Resource;
import com.mg.nmlonline.domain.model.equipment.EquipmentFactory;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Service pour importer un Board depuis un fichier JSON
 */
@Service
public class BoardImportService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Importe un Board depuis un fichier JSON
     */
    public Board importBoardFromJson(String filePath) throws IOException {
        BoardDTO dto = objectMapper.readValue(new File(filePath), BoardDTO.class);
        return importBoard(dto, null);
    }

    /**
     * Importe un Board depuis un fichier JSON dans un Board existant
     */
    public Board importBoardFromJson(String filePath, Board existingBoard) throws IOException {
        BoardDTO dto = objectMapper.readValue(new File(filePath), BoardDTO.class);
        return importBoard(dto, existingBoard);
    }

    private Board importBoard(BoardDTO dto, Board existingBoard) {
        Board board = existingBoard != null ? existingBoard : new Board();

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

                // Ajouter la ressource si présente
                if (sectorDto.resource != null && !sectorDto.resource.isEmpty()) {
                    Resource res = new Resource(sectorDto.resource, 1000.0);
                    sector.setResource(res);
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

    private Unit createUnitFromDTO(UnitDTO unitDto) {
        // Une unité sans classe est inutilisable
        if (unitDto.classes == null || unitDto.classes.isEmpty()) {
            System.err.println("Impossible de créer une unité sans classe - unité ignorée");
            return null;
        }

        Unit unit = new Unit(
            unitDto.experience,
            UnitClass.valueOf(unitDto.classes.get(0))
        );
        unit.setType(unitDto.type);

        // Ajouter la deuxième classe si présente
        if (unitDto.classes.size() > 1) {
            unit.addSecondClass(UnitClass.valueOf(unitDto.classes.get(1)));
        }

        // Ajouter les équipements
        if (unitDto.equipments != null) {
            for (String equipmentName : unitDto.equipments) {
                unit.addEquipment(EquipmentFactory.createFromName(equipmentName));
            }
        }

        return unit;
    }

    // ===== DTOs pour Jackson =====

    public static class BoardDTO {
        public String name;
        public List<SectorDTO> sectors;
    }

    public static class SectorDTO {
        public int number;
        public String name;
        public double income;
        public String resource;
        public List<UnitDTO> army;
        public List<Integer> neighbors;
    }

    public static class UnitDTO {
        public int id;
        public com.mg.nmlonline.domain.model.unit.UnitType type;
        public List<String> classes;
        public double experience;
        public List<String> equipments;
    }
}

