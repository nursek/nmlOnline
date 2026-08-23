/**
 * Carte de libellés français pour les énumérations backend brutes
 * (EquipmentCategory, UnitClass) exposées telles quelles par les DTO.
 * Repli : valeur brute si la clé est inconnue (évite un écran blanc).
 */

export const EQUIPMENT_CATEGORY_LABELS: Readonly<Record<string, string>> = {
  FIREARM: 'Arme à feu',
  MELEE: 'Arme de corps-à-corps',
  DEFENSIVE: 'Équipement défensif',
};

export const UNIT_CLASS_LABELS: Readonly<Record<string, string>> = {
  LEGER: 'Léger',
  MASTODONTE: 'Mastodonte',
  TIREUR: 'Tireur',
  SNIPER: 'Sniper',
  PILOTE_DESTRUCTEUR: 'Pilote destructeur',
  ELEMENTAIRE: 'Élémentaire',
};

export function equipmentCategoryLabel(category: string | null | undefined): string {
  if (!category) return '';
  return EQUIPMENT_CATEGORY_LABELS[category] ?? category;
}

export function unitClassLabel(name: string | null | undefined): string {
  if (!name) return '';
  return UNIT_CLASS_LABELS[name] ?? name;
}
