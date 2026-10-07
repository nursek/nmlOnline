import { saleMultiplier, saleValue, SALE_MULTIPLIERS } from './sale-multiplier';

describe('sale-multiplier', () => {
  it('renvoie le multiplicateur attendu pour chaque palier (1→9)', () => {
    expect(saleMultiplier(1)).toBe(1.0);
    expect(saleMultiplier(2)).toBe(3.0);
    expect(saleMultiplier(3)).toBe(6.0);
    expect(saleMultiplier(4)).toBe(9.0);
    expect(saleMultiplier(5)).toBe(13.0);
    expect(saleMultiplier(6)).toBe(19.5);
    expect(saleMultiplier(7)).toBe(24.5);
    expect(saleMultiplier(8)).toBe(33.0);
    expect(saleMultiplier(9)).toBe(45.0);
  });

  it('plafonne au dernier multiplicateur au-delà de 9', () => {
    expect(saleMultiplier(10)).toBe(45.0);
    expect(saleMultiplier(100)).toBe(45.0);
  });

  it('renvoie 0 pour une quantité nulle ou négative', () => {
    expect(saleMultiplier(0)).toBe(0);
    expect(saleMultiplier(-3)).toBe(0);
  });

  it('saleValue = baseValue × multiplicateur', () => {
    expect(saleValue(600, 1)).toBe(600);
    expect(saleValue(600, 2)).toBe(1800);
    expect(saleValue(600, 9)).toBe(27000);
    expect(saleValue(600, 10)).toBe(27000);
  });

  it('saleValue gère baseValue 0', () => {
    expect(saleValue(0, 5)).toBe(0);
  });

  it('SALE_MULTIPLIERS a 9 entrées', () => {
    expect(SALE_MULTIPLIERS).toHaveLength(9);
  });
});
