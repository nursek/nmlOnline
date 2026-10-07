import type {
  Equipment,
  UnitCartItem,
  UnitCatalogEntry,
  UnitClass,
  VehicleTypeInfo,
} from '../../models';
import { equipmentCategoryLabel, unitClassLabel, UNIT_CLASS_ORDER, vehicleTargetLabel } from '../../core/labels';
import { equipmentLabel } from '../../core/equipment-label';

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

export function matchesEquipmentSearch(
  equipment: Equipment,
  search: string,
  race?: string | null,
): boolean {
  return equipmentLabel(equipment, race).toLowerCase().includes(search);
}

export function sortVehiclesByCost(items: VehicleTypeInfo[]): VehicleTypeInfo[] {
  return [...items].sort((a, b) => a.cost - b.cost);
}

// ponytail: tous les appelants filtrent déjà `> 0` (chips + résumé), donc pas
// de branche négative ici — si on veut afficher des malus, retirer les gardes
// des appelants et reintroduire le signe conditionnel.
function formatNumber(value: number): string {
  return value === Math.floor(value) ? String(value) : value.toFixed(1);
}

function formatPercent(value: number): string {
  return `+${formatNumber(value)} %`;
}

export function equipmentBonusSummary(eq: Equipment): string {
  const parts: string[] = [];
  if (eq.pdfBonus > 0) parts.push(`${formatPercent(eq.pdfBonus)} Pdf`);
  if (eq.pdcBonus > 0) parts.push(`${formatPercent(eq.pdcBonus)} Pdc`);
  if (eq.armBonus > 0) parts.push(`${formatPercent(eq.armBonus)} Arm`);
  if (eq.evasionBonus > 0) parts.push(`${formatPercent(eq.evasionBonus)} Esquive`);
  if ((eq.vehicleBonus ?? 0) > 0) {
    parts.push(`${formatPercent(eq.vehicleBonus ?? 0)} vs ${vehicleTargetLabel(eq.vehicleBonusTarget)}`);
  }
  return parts.join(' ; ');
}

/** « Flensing Claw (Arme de corps-à-corps) : +20 % Pdc. 100 ₡. » */
export function equipmentSummary(eq: Equipment, label: string = eq.name): string {
  const bonuses = equipmentBonusSummary(eq);
  const core = `${label} (${equipmentCategoryLabel(eq.category)})`;
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

/** PILOTE_DESTRUCTEUR n'a pas d'effet exposé par UnitClassDto : seul texte en dur. */
export function unitClassBonusSummary(uc: UnitClass): string {
  const parts: string[] = [];
  if (uc.maxMovementHops > 1) {
    parts.push(`Peut parcourir ${uc.maxMovementHops} secteurs par tour`);
  }
  const reduction = Math.max(uc.damageReductionPdf ?? 0, uc.damageReductionPdc ?? 0);
  if (reduction > 0) {
    parts.push(`Réduit de ${formatNumber(reduction * 100)} % les dégâts PdF et PdC reçus`);
  }
  if ((uc.criticalChance ?? 0) > 0) {
    const chance = formatNumber((uc.criticalChance ?? 0) * 100);
    const multiplier = formatNumber(uc.criticalMultiplier ?? 1).replace('.', ',');
    parts.push(`Critique : ${chance} % de chances, dégâts ×${multiplier}`);
  }
  if (uc.name === 'PILOTE_DESTRUCTEUR') {
    parts.push(
      'Tire en priorité sur les véhicules ; obligatoire pour piloter un véhicule avec une unité',
    );
  }
  return parts.length > 0 ? `${parts.join('. ')}.` : 'Aucun effet de combat.';
}

/** Le quota est par type, toutes classes confondues : le panier déjà rempli le consomme. */
export function unitCartQuantityForType(cart: UnitCartItem[], typeName: string): number {
  return cart
    .filter((line) => line.unitType.name === typeName)
    .reduce((sum, line) => sum + line.quantity, 0);
}

/** Saisies pas encore au panier, clés `type#classe` (cf. `unitCartKey`) : preview du quota. */
export function unitPendingQuantityForType(
  quantities: Record<string, number>,
  typeName: string,
): number {
  return Object.entries(quantities)
    .filter(([key]) => key.startsWith(`${typeName}#`))
    .reduce((sum, [, quantity]) => sum + quantity, 0);
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

export function unitImageUrl(
  entry: Pick<UnitCatalogEntry, 'name'>,
  unitClass: Pick<UnitClass, 'name'>,
  faction: string,
  broken: ReadonlySet<string>,
): string {
  if (!faction) return '';
  const base = `assets/${faction}/units/${entry.name.toLowerCase()}`;
  if (!broken.has(`unit:${entry.name}:${unitClass.name}`)) {
    return `${base}/${unitClass.name.toLowerCase()}.png`;
  }
  return broken.has(`unit:${entry.name}`) ? '' : `${base}/portrait.png`;
}
