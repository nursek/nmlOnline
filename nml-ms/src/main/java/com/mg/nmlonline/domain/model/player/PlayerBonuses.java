package com.mg.nmlonline.domain.model.player;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Structure pour gérer les bonus/malus d'un joueur - Classe Embeddable pour JPA
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerBonuses {
    @Column(name = "attack_bonus_percent")
    private double attackBonusPercent = 0.0;

    @Column(name = "defense_bonus_percent")
    private double defenseBonusPercent = 0.0;

    @Column(name = "pdf_bonus_percent")
    private double pdfBonusPercent = 0.0;

    @Column(name = "pdc_bonus_percent")
    private double pdcBonusPercent = 0.0;

    @Column(name = "armor_bonus_percent")
    private double armorBonusPercent = 0.0;

    @Column(name = "evasion_bonus_percent")
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