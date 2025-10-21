package com.mg.nmlonline.domain.model.unit;

import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.mg.nmlonline.domain.model.unit.UnitType.*;

/**
 * Represents a unit with various attributes for combat.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Unit {
    // ===== CONSTANTES =====
    private static final double INJURED_STAT_MULTIPLIER = 0.5;
    private static final int MIN_EXPERIENCE_FOR_SECOND_CLASS = 5;

    // ===== IDENTIFIANT UNIQUE =====
    private static int nextId = 1;
    private int id;

    // ===== INFORMATIONS DE BASE =====
    private String name;
    private int number = 0; // Numéro de l'unité dans l'armée (ex: BRUTE n°1, n°2, etc.)
    private double experience = 0.0;
    private UnitType type;
    private List<UnitClass> classes;

    // ===== ÉTAT DE L'UNITÉ =====
    private boolean isInjured = false;

    // ===== STATISTIQUES DE BASE =====
    // Stats de base de l'unité (modifiées par le type et l'état blessé)
    private double attack;
    private double defense;

    // ===== STATISTIQUES CALCULÉES =====
    // Stats calculées à partir des équipements (sans bonus du joueur)
    private double pdf; // Points de dégâts à distance
    private double pdc; // Points de dégâts au corps à corps
    private double armor; // Armure (points de vie supplémentaires)
    private double evasion; // Chance d'esquive en %

    // ===== ÉQUIPEMENTS =====
    private List<Equipment> equipments;

    public Unit(double experience, String name, UnitClass primaryClass) {
        this.id = nextId++;
        this.name = name;
        this.experience = experience;
        this.type = UnitType.getTypeByExperience((int) experience);
        this.classes = new ArrayList<>();
        this.classes.add(primaryClass);

        this.attack = type.getBaseAttack();
        this.defense = type.getBaseDefense();

        this.equipments = new ArrayList<>();

        recalculateBaseStats();
    }

    // Recalcule les statistiques de base (sans bonus joueur)
    public void recalculateBaseStats() {
        double statMultiplier = isInjured ? INJURED_STAT_MULTIPLIER : 1.0;

        this.attack = type.getBaseAttack() * statMultiplier;
        this.defense = type.getBaseDefense() * statMultiplier;
        this.pdf = calculateEquipmentPdf();
        this.pdc = calculateEquipmentPdc();
        this.armor = calculateEquipmentArmor();
        this.evasion = calculateEquipmentEvasion();
    }

    /**
     * Applique les bonus du joueur sur les statistiques de l'unité.
     * À appeler après recalculateBaseStats() pour ajouter les bonus globaux du joueur.
     * TODO: À revoir lors de l'implémentation du système de combat complet
     */
    public void applyPlayerBonuses(double attackBonus, double defenseBonus, double pdfBonus,
                                   double pdcBonus, double armorBonus, double evasionBonus) {
        this.attack *= (1.0 + attackBonus);
        this.defense *= (1.0 + defenseBonus);
        this.pdf *= (1.0 + pdfBonus);
        this.pdc *= (1.0 + pdcBonus);
        this.armor *= (1.0 + armorBonus);
        this.evasion -= (evasionBonus * 10);
    }

    private double calculateEquipmentPdf() {
        double totalPdf = 0;
        for (Equipment equipment : equipments) {
            if (isEquipmentCompatible(equipment)) {
                totalPdf += attack * (equipment.getPdfBonus() / 100.0);
            }
        }
        return totalPdf;
    }

    private double calculateEquipmentPdc() {
        double totalPdc = 0;
        for (Equipment equipment : equipments) {
            if (isEquipmentCompatible(equipment)) {
                totalPdc += attack * (equipment.getPdcBonus() / 100.0);
            }
        }
        return totalPdc;
    }

    private double calculateEquipmentArmor() {
        double totalArmor = 0;
        for (Equipment equipment : equipments) {
            if (isEquipmentCompatible(equipment)) {
                totalArmor += defense * (equipment.getArmBonus() / 100.0);
            }
        }
        return totalArmor;
    }

    private double calculateEquipmentEvasion() {
        double totalEvasion = 0;
        for (Equipment equipment : equipments) {
            if (isEquipmentCompatible(equipment)) {
                totalEvasion += equipment.getEvasionBonus();
            }
        }
        return totalEvasion;
    }

    private boolean isEquipmentCompatible(Equipment equipment) {
        return classes.stream()
                .anyMatch(unitClass -> equipment.getCompatibleClasses().contains(unitClass));
    }

    // ===== GESTION DE L'EXPÉRIENCE ET DE L'ÉVOLUTION =====

    /**
     * Fait gagner de l'expérience à l'unité et déclenche une évolution si nécessaire.
     * @param exp Montant d'expérience à ajouter
     */
    public void gainExperience(double exp) {
        this.experience += exp;
        UnitType newType = UnitType.getTypeByExperience((int) experience);
        if (newType != this.type) {
            evolve(newType);
        }
    }

    private void evolve(UnitType newType) {
        this.type = newType;
        recalculateBaseStats(); // Recalcule avec les nouvelles stats de base
    }

    // ===== GESTION DES CLASSES =====

    /**
     * Vérifie si l'unité peut obtenir une seconde classe.
     * Les LARBIN et VOYOU ne peuvent avoir qu'une seule classe.
     * Les autres unités peuvent avoir une seconde classe à partir de 5 d'expérience.
     * @return true si une seconde classe peut être ajoutée
     */
    public boolean canAddSecondClass() {
        long effectiveClassCount = classes.size();
        if (type == UnitType.LARBIN || type == UnitType.VOYOU) {
            return effectiveClassCount < 1;
        }
        return effectiveClassCount <= 1 && experience >= MIN_EXPERIENCE_FOR_SECOND_CLASS;
    }

    /**
     * Ajoute une seconde classe à l'unité si possible.
     * @param secondClass Classe à ajouter
     */
    public void addSecondClass(UnitClass secondClass) {
        if (canAddSecondClass() && !classes.contains(secondClass)) {
            classes.add(secondClass);
            recalculateBaseStats();
        } else {
            System.out.println("Impossible d'ajouter la classe : " + secondClass);
        }
    }

    // ===== GESTION DES ÉQUIPEMENTS =====

    /**
     * Vérifie si l'unité peut équiper un équipement donné.
     * Prend en compte les limites par type d'équipement et la compatibilité.
     * @param equipment Équipement à vérifier
     * @return true si l'équipement peut être ajouté
     */
    public boolean canEquip(Equipment equipment) {
        if (!isEquipmentCompatible(equipment)) {
            return false;
        }

        EquipmentCategory category = equipment.getCategory();
        long currentCount = equipments.stream()
                .filter(e -> e.getCategory() == category)
                .count();

        return switch (category) {
            case FIREARM -> currentCount < type.getMaxFirearms();
            case MELEE -> currentCount < type.getMaxMeleeWeapons();
            case DEFENSIVE -> currentCount < type.getMaxDefensiveEquipment();
        };
    }

    public boolean addEquipment(Equipment equipment) {
        if (canEquip(equipment)) {
            equipments.add(equipment);
            recalculateBaseStats();
            return true;
        }
        return false;
    }

    public boolean removeEquipment(Equipment equipment) {
        if (equipment == null) return false;
        boolean removed = equipments.remove(equipment);
        if (removed) {
            recalculateBaseStats();
        }
        return removed;
    }

    /**
     * Obtient la liste des équipements d'une catégorie spécifique.
     * @param category Catégorie d'équipement recherchée
     * @return Liste des équipements de cette catégorie
     */
    public List<Equipment> getEquipmentsByCategory(EquipmentCategory category) {
        return equipments.stream()
                .filter(e -> e.getCategory() == category)
                .collect(Collectors.toList());
    }

    /**
     * Compte le nombre d'équipements d'une catégorie donnée.
     * @param category Catégorie à compter
     * @return Nombre d'équipements de cette catégorie
     */
    public long countEquipmentsByCategory(EquipmentCategory category) {
        return equipments.stream()
                .filter(e -> e.getCategory() == category)
                .count();
    }

    // ===== MÉTHODES UTILITAIRES POUR LE COMBAT =====

    /**
     * Calcule l'attaque totale de l'unité (ATK + PDF + PDC).
     * @return Total des points d'attaque
     */
    public double getTotalAttack() {
        return attack + pdf + pdc;
    }

    // Méthodes utilitaires pour le tri
    public double getTotalDefense() {
        return defense + armor;
    }

    // Méthodes de formatage pour l'affichage
    private String formatStat(double value) {
        // 2 décimales, supprime les zéros inutiles
        if (value == Math.floor(value)) {
            return String.valueOf((int) value);
        } else {
            return String.format("%.2f", value).replaceAll("0+$", "").replaceAll(",$", "");
        }
    }

    private String formatEvasion(double value) {
        // Esquive arrondie au chiffre du dessus (plafond)
        return String.valueOf((int) Math.ceil(value));
    }

    // Méthode toString pour affichage selon le format demandé
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Unique id: ").append(id).append(" - ");

        if (type == PERSONNAGE) {
            // Exemple de ligne : Character (100 Atk + 100 Pdf + 50 Pdc / 250 Def)
            sb.append(name);
            sb.append(" (");
            statsBuilder(sb, attack, pdf, pdc, defense, armor, evasion);
            sb.append(").");
        } else {
            if (isInjured) {
                sb.append("[X] ");
            }
            sb.append(classes.stream().map(c -> "(" + c.getCode() + ")").collect(Collectors.joining(" "))).append(" ");

            // Type et informations
            sb.append(name);
            sb.append(" n°").append(number);
            sb.append(" (").append(formatStat(experience)).append(" Exp) : ");

            // Équipements
            equipments.forEach(f -> sb.append(f.getName()).append(". "));
            if (equipments.isEmpty()) {
                sb.append("Aucun équipement. ");
            }

            // Statistiques avec formatage précis
            statsBuilder(sb, attack, pdf, pdc, defense, armor, evasion);
            sb.append(".");
        }


        return sb.toString();
    }

    private void statsBuilder(StringBuilder sb, double attack, double pdf, double pdc, double defense, double armor, double evasion) {
        sb.append(formatStat(attack)).append(" Atk");
        if (pdf > 0) sb.append(" + ").append(formatStat(pdf)).append(" Pdf");
        if (pdc > 0) sb.append(" + ").append(formatStat(pdc)).append(" Pdc");
        sb.append(" / ").append(formatStat(defense)).append(" Def");
        if (armor > 0) sb.append(" + ").append(formatStat(armor)).append(" Arm");
        if (evasion > 0) sb.append(". Esquive : ").append(formatEvasion(evasion)).append(" %");
    }
}