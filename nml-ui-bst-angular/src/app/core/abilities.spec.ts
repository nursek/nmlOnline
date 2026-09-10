import { ABILITIES_BY_PLAYER, abilitiesFor } from './abilities';

describe('abilitiesFor', () => {
  it('résout le lot du compte insensiblement à la casse (clé = dossier d’assets)', () => {
    expect(abilitiesFor('Lurio')).toBe(ABILITIES_BY_PLAYER['lurio']);
  });

  it('retombe sur le lot par défaut pour un compte sans capacités définies', () => {
    const fallback = abilitiesFor('compte-inconnu');

    expect(fallback).toBe(abilitiesFor('aucun-compte'));
    expect(fallback.character.bonus).toBeNull();
    expect(fallback.description).toBeTruthy();
  });

  it('retombe sur le lot par défaut sans nom de joueur', () => {
    expect(abilitiesFor(null).territory.name).toBe('Capacité de territoire');
  });

  it('expose une capacité par emplacement pour chaque compte connu', () => {
    for (const [account, abilities] of Object.entries(ABILITIES_BY_PLAYER)) {
      expect(abilities.character.kind).toBe('character');
      expect(abilities.army.kind).toBe('army');
      expect(abilities.territory.kind).toBe('territory');
      expect(abilities.character.bonus).toBeTruthy();
      expect(abilities.territory.bonus).toBeTruthy();
      expect(abilities.description).toBeTruthy();
      expect(account).toBe(account.toLowerCase());
    }
  });
});
