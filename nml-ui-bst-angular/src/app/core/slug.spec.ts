import { slugify } from './slug';

describe('slugify', () => {
  it('passe en minuscules et remplace les espaces par des tirets', () => {
    expect(slugify('Pistolet 9mm')).toBe('pistolet-9mm');
  });

  it('supprime les accents', () => {
    expect(slugify('Poing américain')).toBe('poing-americain');
    expect(slugify('Matraque télescopique')).toBe('matraque-telescopique');
    expect(slugify('Hache de bûcheron')).toBe('hache-de-bucheron');
  });

  it('remplace les crochets et espaces par un seul tiret', () => {
    expect(slugify('Mini machine gun [CM] [MP]')).toBe('mini-machine-gun-cm-mp');
    expect(slugify('Mini machine gun [CM]')).toBe('mini-machine-gun-cm');
  });

  it('gère les apostrophes', () => {
    expect(slugify("Fusil d'assaut")).toBe('fusil-d-assaut');
    expect(slugify('Fusil à impulsion électromagnétique')).toBe(
      'fusil-a-impulsion-electromagnetique',
    );
  });

  it('trim les tirets en début/fin', () => {
    expect(slugify('--test--')).toBe('test');
  });

  it('renvoie une chaîne vide pour une entrée vide', () => {
    expect(slugify('')).toBe('');
  });
});
