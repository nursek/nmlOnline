package com.mg.nmlonline.domain.model.player;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Statistiques du joueur - Classe Embeddable pour JPA
 */
@Embeddable
@Data
@NoArgsConstructor
public class PlayerStats {
    @Column(name = "money")
    private double money = 0.0; // Argent dans le compte en banque du joueur

    @Column(name = "total_income")
    private double totalIncome = 0.0 ; // Représente le revenu quotidien : somme des revenus de chaque secteur possédé

    @Column(name = "total_vehicles_value")
    private double totalVehiclesValue = 0.0; // Valeur totale des véhicules possédés par le joueur

    @Column(name = "total_equipment_value")
    private double totalEquipmentValue = 0.0; // Valeur totale des équipements possédés par le joueur

    // Global stats for ranking
    @Column(name = "total_offensive_power")
    private double totalOffensivePower = 0.0;

    @Column(name = "total_defensive_power")
    private double totalDefensivePower = 0.0;

    @Column(name = "global_power")
    private double globalPower = 0.0;

    @Column(name = "total_economy_power")
    private double totalEconomyPower = 0.0; // Puissance économique totale du joueur : totalIncome + totalEquipmentValue + argent + vehiclesValue

    // Total stats for Battle v0.5
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
