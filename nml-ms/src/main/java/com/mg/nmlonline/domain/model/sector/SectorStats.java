package com.mg.nmlonline.domain.model.sector;

import lombok.Data;

@Data
public class SectorStats {
    // Statistiques globales du secteur
    private double totalAtk = 0.0;
    private double totalPdf = 0.0;
    private double totalPdc = 0.0;
    private double totalDef = 0.0;
    private double totalArmor = 0.0;

    // Global stats
    private double totalOffensive = 0.0;
    private double totalDefensive = 0.0;
    private double globalStats = 0.0;
}
