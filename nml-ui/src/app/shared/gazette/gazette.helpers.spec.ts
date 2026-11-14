import { editionLabel, isUpdated, paragraphs, weatherLine } from './gazette.helpers';
import { GAZETTE_ISSUE, MASTHEAD } from './gazette.fixture';

describe('gazette.helpers', () => {
  it('découpe le corps en paragraphes non vides', () => {
    expect(paragraphs('Un.\n\nDeux.\n\n')).toEqual(['Un.', 'Deux.']);
    expect(paragraphs(null)).toEqual([]);
    expect(paragraphs('   ')).toEqual([]);
  });

  it('nomme l’édition selon le nombre d’éliminés', () => {
    expect(editionLabel({ ...GAZETTE_ISSUE, frontPage: [] })).toBe('Édition du soir');
    expect(editionLabel({ ...GAZETTE_ISSUE, frontPage: [GAZETTE_ISSUE.frontPage[0]] })).toBe(
      'Édition du soir',
    );
    expect(
      editionLabel({ ...GAZETTE_ISSUE, frontPage: [GAZETTE_ISSUE.frontPage[0], GAZETTE_ISSUE.frontPage[0]] }),
    ).toBe('Édition spéciale — double élimination');
    expect(
      editionLabel({
        ...GAZETTE_ISSUE,
        frontPage: [GAZETTE_ISSUE.frontPage[0], GAZETTE_ISSUE.frontPage[0], GAZETTE_ISSUE.frontPage[0]],
      }),
    ).toBe('Édition spéciale — triple élimination');
    expect(editionLabel({ ...GAZETTE_ISSUE, finalEdition: true })).toBe('Dernière édition');
  });

  it('résume la météo', () => {
    expect(weatherLine(MASTHEAD.weather)).toContain('14 °C');
    expect(weatherLine(MASTHEAD.weather)).toContain('vent 12 km/h NNE');
  });

  it('détecte une correction postérieure à la publication', () => {
    expect(isUpdated(GAZETTE_ISSUE)).toBe(false);
    expect(isUpdated({ ...GAZETTE_ISSUE, updatedAt: '2026-11-13T20:16:00Z' })).toBe(true);
    expect(isUpdated({ ...GAZETTE_ISSUE, publishedAt: null })).toBe(false);
  });
});
