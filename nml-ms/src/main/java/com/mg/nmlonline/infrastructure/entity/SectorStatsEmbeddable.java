package com.mg.nmlonline.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
public class SectorStatsEmbeddable {

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

