import { nextSelectedAllianceId } from './alliance.helpers';

function alliance(id: number) {
  return { id, allyPlayerId: id + 100, allyName: `Allié ${id}`, createdTurn: 1 };
}

describe('nextSelectedAllianceId', () => {
  it('conserve l’alliance courante si elle existe encore', () => {
    expect(nextSelectedAllianceId([alliance(1), alliance(2)], 2)).toBe(2);
  });

  it('recale sur la première alliance quand la courante a disparu', () => {
    expect(nextSelectedAllianceId([alliance(3), alliance(4)], 2)).toBe(3);
  });

  it('ne sélectionne rien sans alliance', () => {
    expect(nextSelectedAllianceId([], 2)).toBeNull();
  });
});
