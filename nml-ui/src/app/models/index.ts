export * from './auth.model';
export * from './player.model';
export * from './unit.model';
export * from './equipment.model';
export * from './sector.model';
export * from './vehicle.model';
export * from './building.model';
export * from './board.model';
export * from './instance.model';
export * from './shop.model';
export * from './movement.model';
export * from './sniper-order.model';
export * from './movement-resolution.model';
export * from './turn-resolution.model';
export * from './turn-journal.model';
export * from './player-action.model';
export * from './battle-report.model';
export * from './dev-seed.model';
export * from './ranking.model';
export * from './podium.model';
export * from './gazette.model';
export * from './exchange-offer.model';
export * from './alliance.model';
export * from './game-statistics.model';
export * from './admin.model';

/** Minimal Spring Data page wrapper — `totalElements` détecte une liste tronquée par la taille de page. */
export interface PageResult<T> {
  content: T[];
  totalElements?: number;
}
