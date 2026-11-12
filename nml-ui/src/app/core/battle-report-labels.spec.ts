import { highlightIcon, highlightTone, kindLabel, sideLabel } from './battle-report-labels';

describe('battle report labels', () => {
  it('mappe les points clés sur des icônes et des tons', () => {
    expect(highlightIcon('CHARACTER_LOST')).toBe('heart_broken');
    expect(highlightIcon('HEADQUARTERS_CAPTURED')).toBe('flag');
    expect(highlightIcon('CRIT')).toBe('bolt');
    expect(highlightIcon('INCONNU')).toBe('info');
    expect(highlightTone('HEAVY_LOSSES')).toBe('heavy_losses');
  });

  it('nomme les camps et les types de combat', () => {
    expect(sideLabel('ATTAQUANT')).toBe('Attaquant');
    expect(sideLabel('DEFENSEUR')).toBe('Défenseur');
    expect(sideLabel('CAMP_2')).toBe('Camp 2');
    expect(kindLabel('ASSAUT')).toBe('Assaut');
    expect(kindLabel('IMPASSE')).toBe('Impasse mexicaine');
    expect(kindLabel('TIR')).toBe('Tir à distance');
    expect(kindLabel(null)).toBe('Combat');
  });
});
