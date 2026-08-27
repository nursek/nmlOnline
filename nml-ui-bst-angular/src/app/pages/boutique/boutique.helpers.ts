import type { Equipment, VehicleTypeInfo } from '../../models';
import { equipmentCategoryLabel, unitClassLabel } from '../../core/labels';

/**
 * Ordre de tri par défaut des classes d'unités (léger en tête, élémentaire à la fin).
 * Un équipement sans classe compatible est placé après les classes connues.
 */
export const UNIT_CLASS_ORDER: Readonly<Record<string, number>> = {
  LEGER: 0,
  MASTODONTE: 1,
  TIREUR: 2,
  SNIPER: 3,
  PILOTE_DESTRUCTEUR: 4,
  ELEMENTAIRE: 5,
};

/** Ordre des catégories d'équipement : mêlée → arme à feu → défensif. */
const CATEGORY_ORDER: Readonly<Record<string, number>> = {
  MELEE: 0,
  FIREARM: 1,
  DEFENSIVE: 2,
};

const classRank = (eq: Equipment): number => {
  const first = eq.compatibleClass?.[0]?.name;
  return first ? (UNIT_CLASS_ORDER[first] ?? Number.MAX_SAFE_INTEGER) : Number.MAX_SAFE_INTEGER;
};

const categoryRank = (category: string): number =>
  CATEGORY_ORDER[category] ?? Number.MAX_SAFE_INTEGER;

/**
 * Comparateur de tri par défaut : classe (léger→élémentaire), puis catégorie
 * (mêlée→arme à feu→défensif), puis coût croissant.
 */
export function compareEquipments(a: Equipment, b: Equipment): number {
  const byClass = classRank(a) - classRank(b);
  if (byClass !== 0) return byClass;
  const byCategory = categoryRank(a.category) - categoryRank(b.category);
  if (byCategory !== 0) return byCategory;
  return a.cost - b.cost;
}

/** Tri par défaut : voir `compareEquipments`. */
export function sortEquipments(items: Equipment[]): Equipment[] {
  return [...items].sort(compareEquipments);
}

/** Tri des véhicules par coût croissant. */
export function sortVehiclesByCost(items: VehicleTypeInfo[]): VehicleTypeInfo[] {
  return [...items].sort((a, b) => a.cost - b.cost);
}

// ponytail: tous les appelants filtrent déjà `> 0` (chips + résumé), donc pas
// de branche négative ici — si on veut afficher des malus, retirer les gardes
// des appelants et reintroduire le signe conditionnel.
function formatPercent(value: number): string {
  const n = value === Math.floor(value) ? String(value) : value.toFixed(1);
  return `+${n} %`;
}

/** Bonus d'un équipement formatés « ; »-séparés (uniquement bonus > 0). */
export function equipmentBonusSummary(eq: Equipment): string {
  const parts: string[] = [];
  if (eq.pdfBonus > 0) parts.push(`${formatPercent(eq.pdfBonus)} Pdf`);
  if (eq.pdcBonus > 0) parts.push(`${formatPercent(eq.pdcBonus)} Pdc`);
  if (eq.armBonus > 0) parts.push(`${formatPercent(eq.armBonus)} Arm`);
  if (eq.evasionBonus > 0) parts.push(`${formatPercent(eq.evasionBonus)} Esquive`);
  return parts.join(' ; ');
}

/**
 * Résumé compact d'un équipement :
 * « Poing américain (Arme de corps-à-corps) : +20 % Pdc. 100 ₡. »
 */
export function equipmentSummary(eq: Equipment): string {
  const bonuses = equipmentBonusSummary(eq);
  const core = `${eq.name} (${equipmentCategoryLabel(eq.category)})`;
  const tail = bonuses ? ` : ${bonuses}.` : '.';
  return `${core}${tail} ${eq.cost} ₡.`;
}

/** Résumé compact d'un type de véhicule : « VTT léger (Véhicule) : 50 Def. 4000 ₡. » */
export function vehicleSummary(vt: VehicleTypeInfo): string {
  const parts: string[] = [];
  if (vt.basePdf > 0) parts.push(`${vt.basePdf} Pdf`);
  parts.push(`${vt.baseDefense} Def`);
  return `${vt.displayName} (Véhicule) : ${parts.join(' ; ')}. ${vt.cost} ₡.`;
}

/** Libellé FR de la première classe compatible d'un équipement. */
export function equipmentClassLabel(eq: Equipment): string {
  return unitClassLabel(eq.compatibleClass?.[0]?.name);
}
