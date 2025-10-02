package com.mg.nmlonline.entity.equipment;

import com.mg.nmlonline.entity.unit.UnitClass;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// Factory pour créer les équipements prédéfinis
@Slf4j
public class EquipmentFactory {

    private EquipmentFactory() {
        // Constructeur privé pour empêcher l'instanciation
    }

    public static Equipment createFromName(String name) {
        try (BufferedReader br = new BufferedReader(new java.io.InputStreamReader(
                Objects.requireNonNull(EquipmentFactory.class.getClassLoader().getResourceAsStream("equipments.csv"))))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] fields = line.split(";");
                if (fields[0].equalsIgnoreCase(name)) {
                    String[] classNames = fields[6].split(",");
                    Set<UnitClass> classes = Arrays.stream(classNames)
                            .map(String::trim)
                            .map(UnitClass::valueOf)
                            .collect(Collectors.toSet());
                    return new Equipment(
                            fields[0],
                            Integer.parseInt(fields[1]),
                            Double.parseDouble(fields[2]),
                            Double.parseDouble(fields[3]),
                            Double.parseDouble(fields[4]),
                            Double.parseDouble(fields[5]),
                            classes,
                            EquipmentCategory.valueOf(fields[7])
                    );
                }
            }
        } catch (IOException e) {
            log.error("Error while reading equipments.csv");
        }
        return null;
    }

    public static Equipment createFromEnum(EquipmentType equipmentType) {
        return createFromName(equipmentType.getDisplayName());
    }
}