import { slugify } from './slug';

describe('slugify', () => {
  it('passe en minuscules et remplace les espaces par des tirets', () => {
    expect(slugify('Pistolet 9mm')).toBe('pistolet-9mm');
  });

  it('supprime les accents', () => {
    expect(slugify('Flensing Claw')).toBe('flensing-claw');
    expect(slugify('Voidblade')).toBe('voidblade');
    expect(slugify('Hyperphase Glaive')).toBe('hyperphase-glaive');
  });

  it('trim les tirets en début/fin', () => {
    expect(slugify('--test--')).toBe('test');
  });

  it('renvoie une chaîne vide pour une entrée vide', () => {
    expect(slugify('')).toBe('');
  });
});
