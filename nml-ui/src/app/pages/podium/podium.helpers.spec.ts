import type { PodiumPlace } from '../../models';
import { causeLabel, championLabel, medalEmoji, medalLabel } from './podium.helpers';

function place(overrides: Partial<PodiumPlace> = {}): PodiumPlace {
  return {
    place: 2,
    playerId: 2,
    playerName: 'lurio',
    race: 'ORKS',
    characterName: 'Ratcatcher',
    eliminatedTurn: 5,
    eliminationCause: 'MAIN_CHARACTER_KILLED',
    definitive: true,
    killerPlayerId: 1,
    killerName: 'nursek',
    kills: 1,
    assists: 0,
    alive: false,
    victims: ['trazyn'],
    assistedVictims: [],
    ...overrides,
  };
}

describe('podium.helpers', () => {
  it('distingue mort définitive, défaite et survivant', () => {
    expect(causeLabel(place())).toBe('Mort définitive');
    expect(causeLabel(place({ definitive: false, eliminationCause: 'HEADQUARTERS_CAPTURED' }))).toBe(
      'QG conquis — retour possible',
    );
    expect(causeLabel(place({ alive: true, definitive: false }))).toBe('—');
  });

  it('nomme le champion selon sa survie', () => {
    expect(championLabel(place({ alive: true }))).toBe('Dernier survivant');
    expect(championLabel(place({ alive: false }))).toBe('Vainqueur posthume');
    expect(championLabel(null)).toBe('');
  });

  it('donne les libellés de médailles', () => {
    expect(medalLabel(1)).toBe('Or');
    expect(medalLabel(2)).toBe('Argent');
    expect(medalLabel(3)).toBe('Bronze');
    expect(medalLabel(4)).toBe('4ᵉ place');
    expect(medalEmoji(1)).toBe('🥇');
    expect(medalEmoji(4)).toBe('•');
  });
});
