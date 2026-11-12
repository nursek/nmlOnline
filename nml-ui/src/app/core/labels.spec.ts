import { equipmentCategoryLabel, sniperActionLabel, unitClassLabel } from './labels';

describe('labels (libellés FR)', () => {
  it("traduit les catégories d'équipement connues", () => {
    expect(equipmentCategoryLabel('FIREARM')).toBe('Arme à feu');
    expect(equipmentCategoryLabel('MELEE')).toBe('Arme de corps-à-corps');
    expect(equipmentCategoryLabel('DEFENSIVE')).toBe('Équipement défensif');
  });

  it("traduit les classes d'unité connues", () => {
    expect(unitClassLabel('PILOTE_DESTRUCTEUR')).toBe('Pilote destructeur');
    expect(unitClassLabel('LEGER')).toBe('Léger');
    expect(unitClassLabel('SNIPER')).toBe('Sniper');
  });

  it('replie sur la valeur brute pour une clé inconnue', () => {
    expect(equipmentCategoryLabel('UNKNOWN')).toBe('UNKNOWN');
    expect(unitClassLabel('WHATEVER')).toBe('WHATEVER');
  });

  it("traduit les actions sniper et replie sur la valeur brute", () => {
    expect(sniperActionLabel('TIR_PRECISION')).toBe('Tir de précision');
    expect(sniperActionLabel('SURVEILLANCE')).toBe('Surveillance');
    expect(sniperActionLabel('AUTRE')).toBe('AUTRE');
    expect(sniperActionLabel(null)).toBe('');
  });

  it('renvoie une chaîne vide pour null/undefined', () => {
    expect(equipmentCategoryLabel(null)).toBe('');
    expect(equipmentCategoryLabel(undefined)).toBe('');
    expect(unitClassLabel(null)).toBe('');
    expect(unitClassLabel(undefined)).toBe('');
  });
});
