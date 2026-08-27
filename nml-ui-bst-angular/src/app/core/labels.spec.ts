import { equipmentCategoryLabel, unitClassLabel } from './labels';

describe('labels (libellés FR)', () => {
  it("traduit les catégories d'équipement connues", () => {
    expect(equipmentCategoryLabel('FIREARM')).toBe('Arme à feu');
    expect(equipmentCategoryLabel('MELEE')).toBe('Arme de corps-à-corps');
    expect(equipmentCategoryLabel('DEFENSIVE')).toBe('Équipement défensif');
  });

  it("traduit les classes d'unité connues", () => {
    expect(unitClassLabel('PILOTE_DESTRUCTEUR')).toBe('Pilote destructeur');
    expect(unitClassLabel('LEGER')).toBe('Léger');
    expect(unitClassLabel('ELEMENTAIRE')).toBe('Élémentaire');
  });

  it('replie sur la valeur brute pour une clé inconnue', () => {
    expect(equipmentCategoryLabel('UNKNOWN')).toBe('UNKNOWN');
    expect(unitClassLabel('WHATEVER')).toBe('WHATEVER');
  });

  it('renvoie une chaîne vide pour null/undefined', () => {
    expect(equipmentCategoryLabel(null)).toBe('');
    expect(equipmentCategoryLabel(undefined)).toBe('');
    expect(unitClassLabel(null)).toBe('');
    expect(unitClassLabel(undefined)).toBe('');
  });
});
