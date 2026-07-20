package com.mg.nmlonline.domain.model.sector;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Statistiques d'un secteur - Classe Embeddable pour JPA
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class SectorStats {
    @Column(name = "sector_total_atk")
    private double totalAtk = 0.0;

    @Column(name = "sector_total_pdf")
    private double totalPdf = 0.0;

    @Column(name = "sector_total_pdc")
    private double totalPdc = 0.0;

    @Column(name = "sector_total_def")
    private double totalDef = 0.0;

    @Column(name = "sector_total_armor")
    private double totalArmor = 0.0;

    @Column(name = "sector_total_offensive")
    private double totalOffensive = 0.0;

    @Column(name = "sector_total_defensive")
    private double totalDefensive = 0.0;

    @Column(name = "sector_global_stats")
    private double globalStats = 0.0;
}
