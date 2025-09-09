package com.mg.nmlonline.model.player;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Structure pour gérer les bonus/malus d'un joueur
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerBonuses {
    private double attackBonusPercent = 0.0;
    private double defenseBonusPercent = 0.0;
    private double pdfBonusPercent = 0.0;
    private double pdcBonusPercent = 0.0;
    private double armorBonusPercent = 0.0;
    private double evasionBonusPercent = 0.0;

    /**
     * Vérifie si au moins un bonus est appliqué
     */
    public boolean hasAnyBonus() {
        return attackBonusPercent != 0 || defenseBonusPercent != 0 ||
                pdfBonusPercent != 0 || pdcBonusPercent != 0 ||
                armorBonusPercent != 0 || evasionBonusPercent != 0;
    }

    /**
     * Affiche les bonus non nuls
     */
    public void displayBonuses() {
        if (attackBonusPercent != 0) System.out.printf("  Attaque : %+.1f%%%n", attackBonusPercent * 100);
        if (defenseBonusPercent != 0) System.out.printf("  Défense : %+.1f%%%n", defenseBonusPercent * 100);
        if (pdfBonusPercent != 0) System.out.printf("  PDF : %+.1f%%%n", pdfBonusPercent * 100);
        if (pdcBonusPercent != 0) System.out.printf("  PDC : %+.1f%%%n", pdcBonusPercent * 100);
        if (armorBonusPercent != 0) System.out.printf("  Armure : %+.1f%%%n", armorBonusPercent * 100);
        if (evasionBonusPercent != 0) System.out.printf("  Esquive : %+.1f%%%n", evasionBonusPercent * 100);
    }
}