import {
  assignPlayerColors,
  contrastColor,
  haloColor,
  DARK_TEXT,
  LIGHT_TEXT,
  MAP_THEME,
} from './carte.config';
import type { Player } from '../../models';

describe('contrastColor', () => {
  it('pose un texte sombre sur une couleur claire et clair sur une couleur sombre', () => {
    expect(contrastColor('#e8bb45')).toBe(DARK_TEXT);
    expect(contrastColor('#6f93c9')).toBe(DARK_TEXT);
    expect(contrastColor('#192d5a')).toBe(LIGHT_TEXT);
    expect(contrastColor('#060809')).toBe(LIGHT_TEXT);
  });

  it('accepte les hexadécimaux à trois chiffres', () => {
    expect(contrastColor('#fff')).toBe(DARK_TEXT);
    expect(contrastColor('#000')).toBe(LIGHT_TEXT);
  });
});

describe('haloColor', () => {
  it('prend toujours la couleur opposée au texte', () => {
    expect(haloColor('#e8bb45')).toBe(LIGHT_TEXT);
    expect(haloColor('#192d5a')).toBe(DARK_TEXT);
  });
});

describe('MAP_THEME', () => {
  it('expose des couleurs sur 6 chiffres, concaténables avec un canal alpha', () => {
    const colors = [...MAP_THEME.playerPalette, MAP_THEME.neutralColor];
    expect(colors.filter((c) => !/^#[0-9a-f]{6}$/i.test(c))).toEqual([]);
  });
});

describe('assignPlayerColors', () => {
  const player = (id: number | null): Player => ({
    id,
    name: `p${id}`,
    stats: {} as Player['stats'],
    equipments: [],
    resources: [],
    sectors: [],
    character: null,
    buildings: [],
  });

  it('associe la même couleur quel que soit l’ordre de l’API', () => {
    const a = assignPlayerColors([player(2), player(1)]);
    const b = assignPlayerColors([player(1), player(2)]);
    expect(a.get(1)).toBe(MAP_THEME.playerPalette[0]);
    expect(a.get(2)).toBe(MAP_THEME.playerPalette[1]);
    expect([...a.entries()]).toEqual([...b.entries()]);
  });

  it('ignore les joueurs sans id et boucle sur la palette', () => {
    const players = [player(null), ...Array.from({ length: 13 }, (_, i) => player(i + 1))];
    const colors = assignPlayerColors(players);
    expect(colors.size).toBe(13);
    expect(colors.get(13)).toBe(MAP_THEME.playerPalette[0]);
  });
});
