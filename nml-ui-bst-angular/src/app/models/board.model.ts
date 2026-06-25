import type { Sector } from './sector.model';

// Types pour la Board (carte du jeu)
export interface Board {
  id: number;
  name: string;
  mapImageUrl: string | null;
  svgOverlayUrl: string | null;
  sectors: { [key: number]: Sector };
}
