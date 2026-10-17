import { contrastColor, haloColor, DARK_TEXT, LIGHT_TEXT, MAP_THEME } from './carte.config';

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
