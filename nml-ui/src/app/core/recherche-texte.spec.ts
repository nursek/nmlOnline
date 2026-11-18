import { normaliserTexte, occurrences } from './recherche-texte';

describe('recherche-texte', () => {
  it('ignore accents et casse', () => {
    expect(normaliserTexte('Prométhéum')).toBe('prometheum');
    expect(occurrences('Le Prométhéum est rare', 'PROMETHEUM')).toEqual([{ debut: 3, fin: 13 }]);
  });

  it('rend les positions dans le texte original pour chaque occurrence', () => {
    expect(occurrences('Armure, armure', 'armure')).toEqual([
      { debut: 0, fin: 6 },
      { debut: 8, fin: 14 },
    ]);
  });

  it('ne trouve rien pour un terme vide', () => {
    expect(occurrences('Alliance', '   ')).toEqual([]);
  });
});
