package com.mg.nmlonline.model.player;

import lombok.Data;

@Data
public class PlayerStats {
    private double money = 0.0; // Argent dans le compte en banque du joueur
    private double totalIncome = 0.0 ; // Représente le revenu quotidien : somme des revenus de chaque secteur possédé
    private double totalVehiclesValue = 0.0; // Valeur totale des véhicules possédés par le joueur
    private double totalEquipmentValue = 0.0; // Valeur totale des équipements possédés par le joueur
    private double totalMilitaryPower = 0.0; // Puissance militaire totale du joueur, calculée à partir des armées des secteurs
    private double totalEconomyPower = 0.0; // Puissance économique totale du joueur : totalIncome + totalEquipmentValue + argent + vehiclesValue

    /*
    Voici comment sont modifiées les stats du joueur :

    Money : à chaque achat du joueur dans la boutique, échanges, etc.
    TotalIncome : recalculé à chaque ajout ou retrait de secteur donc en début de tour.
    TotalVehiclesValue : recalculé à chaque ajout ou retrait d'un véhicule dans l'armée du joueur.
    TotalEquipmentValue : recalculé à chaque ajout ou retrait d'un équipement dans l'inventaire du joueur.

    Les stats suivantes sont les stats pour les classements, ainsi calculées en fin de tour. Possiblement faire un appel et maj les 4 stats car EconomyPower se base sur celles-ci
    TotalMilitaryPower : recalculé à chaque ajout ou retrait d'une unité dans l'armée d'un secteur.
    TotalEconomyPower : recalculé à chaque modification des autres stats
     */
}
