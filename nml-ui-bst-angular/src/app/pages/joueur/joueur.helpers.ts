import type {
  Building,
  EquipmentStack,
  GameCharacter,
  Player,
  Sector,
  Unit,
  Vehicle,
} from '../../models';
import { equipmentCategoryLabel, unitClassLabel, UNIT_CLASS_ORDER } from '../../core/labels';

/**
 * Calculs de la fiche joueur côté client : les PlayerStats/SectorStats en base
 * ne sont recalculées qu'à l'import ou au combat, pas après un achat ou un
 * changement d'équipement — les totaux ici somment les entités fraîches.
 */

export interface ForcesTotals {
  atk: number;
  pdf: number;
  pdc: number;
  def: number;
  armor: number;
}

export interface SectorForces {
  sector: Sector;
  units: Unit[];
  buildings: Building[];
  character: GameCharacter | null;
  vehicles: Vehicle[];
  totals: ForcesTotals;
  empty: boolean;
}

export interface PlayerForces {
  sectors: SectorForces[];
  totals: ForcesTotals;
  offensive: number;
  defensive: number;
  militaryTotal: number;
  globalPower: number;
}

export interface TroopSummary {
  type: string;
  experience: number;
  count: number;
}

/** Équipements d'une même catégorie, à l'intérieur d'une classe compatible. */
export interface EquipmentCategorySubGroup {
  key: string;
  label: string;
  stacks: EquipmentStack[];
}

export interface EquipmentClassGroup {
  key: string;
  label: string;
  count: number;
  categories: EquipmentCategorySubGroup[];
}

/** Classe d'un équipement : la première compatible, ou « Sans classe ». */
const classKeyOf = (stack: EquipmentStack): string =>
  stack.equipment.compatibleClass?.[0]?.name ?? 'AUCUNE';

const classLabelOf = (key: string): string =>
  key === 'AUCUNE' ? 'Sans classe' : unitClassLabel(key);

/** Valeur d'un stock possédé : « 3 x 850 = 2550 ₡ ». */
export function equipmentStackCost(stack: EquipmentStack): string {
  const total = num(stack.quantity) * num(stack.equipment.cost);
  return `${num(stack.quantity)} x ${num(stack.equipment.cost)} = ${total} ₡`;
}

const classRank = (key: string): number => UNIT_CLASS_ORDER[key] ?? Number.MAX_SAFE_INTEGER;

/** Ordre d'affichage des catégories (même que la boutique : mêlée → arme à feu → défensif). */
const EQUIPMENT_CATEGORY_ORDER: readonly string[] = ['MELEE', 'FIREARM', 'DEFENSIVE'];

export interface EconomyBreakdown {
  money: number;
  income: number;
  equipmentValue: number;
  vehicleValue: number;
  total: number;
}

const num = (value: number | null | undefined): number => value ?? 0;

/** Entité non attribuée (playerId null) : conservée plutôt que masquée. */
const isOwned = (entityPlayerId: number | null | undefined, playerId: number | null): boolean =>
  entityPlayerId == null || playerId == null || entityPlayerId === playerId;

const emptyTotals = (): ForcesTotals => ({ atk: 0, pdf: 0, pdc: 0, def: 0, armor: 0 });

const addTo = (
  totals: ForcesTotals,
  atk: number,
  pdf: number,
  pdc: number,
  def: number,
  armor: number,
): ForcesTotals => ({
  atk: totals.atk + atk,
  pdf: totals.pdf + pdf,
  pdc: totals.pdc + pdc,
  def: totals.def + def,
  armor: totals.armor + armor,
});

const sumUnits = (units: Unit[]): ForcesTotals =>
  units.reduce(
    (t, u) => addTo(t, num(u.attack), num(u.pdf), num(u.pdc), num(u.defense), num(u.armor)),
    emptyTotals(),
  );

/** Revenu par tour : somme des incomes des secteurs (recalculé, frais). */
export function incomeTotal(sectors: Sector[]): number {
  return sectors.reduce((sum, s) => sum + num(s.income), 0);
}

/** Stat affichable : valeur + libellé + séparateur à afficher devant. */
export interface StatToken {
  value: number;
  label: string;
  separator: string;
}

type StatEntry = readonly [value: number, label: string];

/**
 * Stats non nulles d'une entité, dans l'ordre offensif → défensif :
 * « 120 Atk + 390 Pdf / 120 Def + 392.5 Arm ». Les stats à 0 sont masquées,
 * le séparateur « / » marque le passage offensif → défensif.
 */
export function statLine(
  offensive: readonly StatEntry[],
  defensive: readonly StatEntry[],
): StatToken[] {
  const off = offensive.filter(([value]) => value !== 0);
  const def = defensive.filter(([value]) => value !== 0);

  return [...off, ...def].map(([value, label], i) => ({
    value,
    label,
    separator: i === 0 ? '' : i === off.length && off.length > 0 ? ' / ' : ' + ',
  }));
}

export function unitStats(u: Unit): StatToken[] {
  return statLine(
    [
      [num(u.attack), 'Atk'],
      [num(u.pdf), 'Pdf'],
      [num(u.pdc), 'Pdc'],
    ],
    [
      [num(u.defense), 'Def'],
      [num(u.armor), 'Arm'],
    ],
  );
}

export function characterStats(c: GameCharacter): StatToken[] {
  return statLine(
    [
      [num(c.baseAttack), 'Atk'],
      [num(c.basePdf), 'Pdf'],
      [num(c.basePdc), 'Pdc'],
    ],
    [
      [num(c.baseDefense), 'Def'],
      [num(c.baseArmor), 'Arm'],
    ],
  );
}

export function buildingStats(b: Building): StatToken[] {
  return statLine([[num(b.attack), 'Atk']], [[num(b.defense), 'Def']]);
}

export function vehicleStats(v: Vehicle): StatToken[] {
  return statLine([[num(v.pdf), 'Pdf']], [[num(v.defense), 'Def']]);
}

export function totalsStats(totals: ForcesTotals): StatToken[] {
  return statLine(
    [
      [totals.atk, 'Atk'],
      [totals.pdf, 'Pdf'],
      [totals.pdc, 'Pdc'],
    ],
    [
      [totals.def, 'Def'],
      [totals.armor, 'Arm'],
    ],
  );
}

/** Codes de classes d'une unité, ex. « L » ou « LM ». */
export function unitClassCodes(u: Unit): string {
  return [...(u.classes ?? [])]
    .map((c) => c.code)
    .sort()
    .join('');
}

/** Équipements portés par une unité, ex. « Pistolet, Gilet » ou « Aucun équipement ». */
export function unitEquipmentLabel(u: Unit): string {
  const names = [...(u.equipments ?? [])].map((e) => e.name).sort();
  return names.length > 0 ? names.join(', ') : 'Aucun équipement';
}

/** Forces du joueur présentes dans un secteur : entités filtrées par propriétaire. */
export function sectorForces(sector: Sector, playerId: number | null): SectorForces {
  const units = (sector.army ?? [])
    .filter((u) => isOwned(u.playerId, playerId))
    .sort((a, b) => {
      const byExp = num(b.experience) - num(a.experience);
      if (byExp !== 0) return byExp;
      const byType = (a.type?.name ?? '').localeCompare(b.type?.name ?? '');
      if (byType !== 0) return byType;
      return num(a.number) - num(b.number);
    });
  const buildings = (sector.buildings ?? []).filter((b) => isOwned(b.playerId, playerId));
  const character =
    sector.character && isOwned(sector.character.playerId, playerId) ? sector.character : null;
  const vehicles = (sector.vehicles ?? []).filter((v) => isOwned(v.playerId, playerId));

  let totals = sumUnits(units);
  for (const b of buildings) {
    totals = addTo(totals, num(b.attack), 0, 0, num(b.defense), 0);
  }
  if (character) {
    totals = addTo(
      totals,
      num(character.baseAttack),
      num(character.basePdf),
      num(character.basePdc),
      num(character.baseDefense),
      num(character.baseArmor),
    );
  }
  for (const v of vehicles) {
    totals = addTo(totals, 0, num(v.pdf), 0, num(v.defense), 0);
  }

  return {
    sector,
    units,
    buildings,
    character,
    vehicles,
    totals,
    empty: units.length === 0 && buildings.length === 0 && !character && vehicles.length === 0,
  };
}

/** Forces du joueur sur tous ses secteurs + puissance globale = (off + def) / 2. */
export function playerForces(sectors: Sector[], playerId: number | null): PlayerForces {
  const sectorForceList = sectors
    .map((s) => sectorForces(s, playerId))
    .sort((a, b) => num(a.sector.number) - num(b.sector.number));

  const totals = sectorForceList.reduce(
    (t, sf) =>
      addTo(t, sf.totals.atk, sf.totals.pdf, sf.totals.pdc, sf.totals.def, sf.totals.armor),
    emptyTotals(),
  );
  const offensive = totals.atk + totals.pdf + totals.pdc;
  const defensive = totals.def + totals.armor;

  return {
    sectors: sectorForceList,
    totals,
    offensive,
    defensive,
    militaryTotal: offensive + defensive,
    globalPower: (offensive + defensive) / 2,
  };
}

/** Résumé des troupes par type ET expérience, triées par exp décroissante : « 100 LARBIN (8 Exp) ». */
export function troopSummaries(sectors: Sector[], playerId: number | null): TroopSummary[] {
  const byKey = new Map<string, TroopSummary>();
  for (const s of sectors) {
    for (const u of s.army ?? []) {
      if (!isOwned(u.playerId, playerId)) continue;
      const type = u.type?.name ?? 'UNITÉ';
      const experience = num(u.experience);
      const key = `${type}#${experience}`;
      const entry = byKey.get(key);
      if (entry) entry.count++;
      else byKey.set(key, { type, experience, count: 1 });
    }
  }

  return Array.from(byKey.values()).sort((a, b) => {
    if (a.experience !== b.experience) return b.experience - a.experience;
    return a.type.localeCompare(b.type);
  });
}

/**
 * Équipements du joueur groupés par classe compatible (ordre Léger → Élémentaire,
 * « Sans classe » en fin), chaque classe étant découpée par catégorie dans l'ordre
 * boutique (mêlée → arme à feu → défensif) ; coût croissant puis nom dans chaque catégorie.
 */
export function equipmentByClass(stacks: EquipmentStack[]): EquipmentClassGroup[] {
  const byClass = new Map<string, EquipmentStack[]>();
  for (const stack of stacks) {
    const key = classKeyOf(stack);
    const bucket = byClass.get(key);
    if (bucket) bucket.push(stack);
    else byClass.set(key, [stack]);
  }

  return Array.from(byClass.entries())
    .sort(([a], [b]) => classRank(a) - classRank(b) || a.localeCompare(b))
    .map(([key, items]) => {
      const categories = groupByCategory(items);
      return {
        key,
        label: classLabelOf(key),
        count: categories.reduce((n, c) => n + c.stacks.length, 0),
        categories,
      };
    });
}

/** Sous-groupes par catégorie d'une liste d'équipements d'une même classe compatible. */
function groupByCategory(stacks: EquipmentStack[]): EquipmentCategorySubGroup[] {
  const byCategory = new Map<string, EquipmentStack[]>();
  for (const stack of stacks) {
    const key = stack.equipment.category;
    const bucket = byCategory.get(key);
    if (bucket) bucket.push(stack);
    else byCategory.set(key, [stack]);
  }

  const ordered = [
    ...EQUIPMENT_CATEGORY_ORDER.filter((key) => byCategory.has(key)),
    ...Array.from(byCategory.keys())
      .filter((key) => !EQUIPMENT_CATEGORY_ORDER.includes(key))
      .sort(),
  ];

  return ordered.map((key) => ({
    key,
    label: equipmentCategoryLabel(key),
    stacks: [...(byCategory.get(key) ?? [])].sort(
      (a, b) =>
        a.equipment.cost - b.equipment.cost || a.equipment.name.localeCompare(b.equipment.name),
    ),
  }));
}

/**
 * Décomposition de la puissance économique (même formule que le backend :
 * money + revenu + valeur des équipements + valeur des véhicules).
 */
export function economyBreakdown(player: Player, income: number): EconomyBreakdown {
  const money = num(player.stats?.money);
  const equipmentValue = num(player.stats?.totalEquipmentValue);
  const vehicleValue = num(player.stats?.totalVehiclesValue);
  return {
    money,
    income,
    equipmentValue,
    vehicleValue,
    total: money + income + equipmentValue + vehicleValue,
  };
}
