import type { Equipment, UnitCartItem, VehicleTypeInfo } from '../../models';
import { equipmentCategoryLabel, unitClassLabel, UNIT_CLASS_ORDER } from '../../core/labels';

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

export function compareEquipments(a: Equipment, b: Equipment): number {
  const byClass = classRank(a) - classRank(b);
  if (byClass !== 0) return byClass;
  const byCategory = categoryRank(a.category) - categoryRank(b.category);
  if (byCategory !== 0) return byCategory;
  return a.cost - b.cost;
}

export function sortEquipments(items: Equipment[]): Equipment[] {
  return [...items].sort(compareEquipments);
}

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

export function equipmentBonusSummary(eq: Equipment): string {
  const parts: string[] = [];
  if (eq.pdfBonus > 0) parts.push(`${formatPercent(eq.pdfBonus)} Pdf`);
  if (eq.pdcBonus > 0) parts.push(`${formatPercent(eq.pdcBonus)} Pdc`);
  if (eq.armBonus > 0) parts.push(`${formatPercent(eq.armBonus)} Arm`);
  if (eq.evasionBonus > 0) parts.push(`${formatPercent(eq.evasionBonus)} Esquive`);
  return parts.join(' ; ');
}

/** « Flensing Claw (Arme de corps-à-corps) : +20 % Pdc. 100 ₡. » */
export function equipmentSummary(eq: Equipment): string {
  const bonuses = equipmentBonusSummary(eq);
  const core = `${eq.name} (${equipmentCategoryLabel(eq.category)})`;
  const tail = bonuses ? ` : ${bonuses}.` : '.';
  return `${core}${tail} ${eq.cost} ₡.`;
}

/** « VTT léger (Véhicule) : 50 Def. 4000 ₡. » */
export function vehicleSummary(vt: VehicleTypeInfo): string {
  const parts: string[] = [];
  if (vt.basePdf > 0) parts.push(`${vt.basePdf} Pdf`);
  parts.push(`${vt.baseDefense} Def`);
  return `${vt.displayName} (Véhicule) : ${parts.join(' ; ')}. ${vt.cost} ₡.`;
}

export function equipmentClassLabel(eq: Equipment): string {
  return unitClassLabel(eq.compatibleClass?.[0]?.name);
}

/** Le quota est par type, toutes classes confondues : le panier déjà rempli le consomme. */
export function unitCartQuantityForType(cart: UnitCartItem[], typeName: string): number {
  return cart
    .filter((line) => line.unitType.name === typeName)
    .reduce((sum, line) => sum + line.quantity, 0);
}

export function unitQuotaRemaining(
  maxPerTurn: number,
  purchasedThisTurn: number,
  inCart: number,
): number {
  return Math.max(0, maxPerTurn - purchasedThisTurn - inCart);
}

/** Quantité proposée bornée au quota restant ; 0 = quota épuisé, l'appelant n'ajoute rien. */
export function clampUnitQuantity(requested: number, remaining: number): number {
  if (remaining <= 0) return 0;
  return Math.max(1, Math.min(Math.trunc(requested) || 1, remaining));
}
