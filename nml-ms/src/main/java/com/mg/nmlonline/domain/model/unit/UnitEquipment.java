package com.mg.nmlonline.domain.model.unit;

import com.mg.nmlonline.domain.model.equipment.Equipment;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "UNIT_EQUIPMENTS")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "unit")
public class UnitEquipment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "unit_equipment_seq")
    @SequenceGenerator(name = "unit_equipment_seq", sequenceName = "unit_equipments_id_seq", allocationSize = 50)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    public UnitEquipment(Unit unit, Equipment equipment) {
        this.unit = unit;
        this.equipment = equipment;
    }
}

