package com.mg.nmlonline.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
public class PlayerBonusesEmbeddable {

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
}