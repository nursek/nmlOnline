import { slugify } from './slug';

export interface LocalizedEquipment {
  name: string;
  displayNames?: Record<string, string> | null;
}

export function equipmentLabel(equipment: LocalizedEquipment, race?: string | null): string {
  return (race ? equipment.displayNames?.[race] : undefined) ?? equipment.name;
}

/** Vignette de la race d'abord, repli sur le fichier commun. */
export function equipmentImageCandidates(
  equipment: LocalizedEquipment,
  race?: string | null,
): string[] {
  const file = slugify(equipment.name);
  const shared = `assets/shop/equipment/${file}.png`;
  return race ? [`assets/shop/equipment/${race.toLowerCase()}/${file}.png`, shared] : [shared];
}
