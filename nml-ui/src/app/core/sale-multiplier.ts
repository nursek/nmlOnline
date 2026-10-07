/**
 * Multiplicateur de vente des ressources — réplique frontend de
 * ResourceService.SALE_MULTIPLIERS (backend). Synchronisation manuelle
 * si le tableau Java change (ponytail: règles de jeu, peu mutable).
 */
export const SALE_MULTIPLIERS = [1.0, 3.0, 6.0, 9.0, 13.0, 19.5, 24.5, 33.0, 45.0] as const;

export function saleMultiplier(quantity: number): number {
  if (quantity <= 0) return 0;
  if (quantity >= SALE_MULTIPLIERS.length) return SALE_MULTIPLIERS[SALE_MULTIPLIERS.length - 1];
  return SALE_MULTIPLIERS[quantity - 1];
}

export function saleValue(baseValue: number, quantity: number): number {
  return baseValue * saleMultiplier(quantity);
}
