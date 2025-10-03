package com.mg.nmlonline.entity.player;

import lombok.Data;

@Data
public class PlayerStats {
    private double money = 0.0; // Argent dans le compte en banque du joueur
    private double totalIncome = 0.0 ; // Représente le revenu quotidien : somme des revenus de chaque secteur possédé
    private double totalVehiclesValue = 0.0; // Valeur totale des véhicules possédés par le joueur
    private double totalEquipmentValue = 0.0; // Valeur totale des équipements possédés par le joueur

    // Global stats for ranking
    private double totalOffensivePower = 0.0;
    private double totalDefensivePower = 0.0;
    private double globalPower = 0.0;
    private double totalEconomyPower = 0.0; // Puissance économique totale du joueur : totalIncome + totalEquipmentValue + argent + vehiclesValue

    // Total stats for Battle v0.5
    private double totalAtk = 0.0;
    private double totalPdf = 0.0;
    private double totalPdc = 0.0;
    private double totalDef = 0.0;
    private double totalArmor = 0.0;
}
