package com.mg.nmlonline.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
public class PlayerStatsEmbeddable {

    @Column(name = "money")
    private double money = 0.0;

    @Column(name = "total_income")
    private double totalIncome = 0.0;

    @Column(name = "total_vehicles_value")
    private double totalVehiclesValue = 0.0;

    @Column(name = "total_equipment_value")
    private double totalEquipmentValue = 0.0;

    @Column(name = "total_offensive_power")
    private double totalOffensivePower = 0.0;

    @Column(name = "total_defensive_power")
    private double totalDefensivePower = 0.0;

    @Column(name = "global_power")
    private double globalPower = 0.0;

    @Column(name = "total_economy_power")
    private double totalEconomyPower = 0.0;

    @Column(name = "total_atk")
    private double totalAtk = 0.0;

    @Column(name = "total_pdf")
    private double totalPdf = 0.0;

    @Column(name = "total_pdc")
    private double totalPdc = 0.0;

    @Column(name = "total_def")
    private double totalDef = 0.0;

    @Column(name = "total_armor")
    private double totalArmor = 0.0;
}
