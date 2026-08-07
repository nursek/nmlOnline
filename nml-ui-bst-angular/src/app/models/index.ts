export * from './auth.model';
export * from './player.model';
export * from './unit.model';
export * from './equipment.model';
export * from './sector.model';
export * from './vehicle.model';
export * from './building.model';
export * from './board.model';
export * from './shop.model';
export * from './movement.model';
export * from './movement-resolution.model';

/** Minimal Spring Data page wrapper — only `content` is ever read. */
export interface PageResult<T> {
  content: T[];
}
