import { contrastColor, haloColor, DARK_TEXT, LIGHT_TEXT } from './carte.config';

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
