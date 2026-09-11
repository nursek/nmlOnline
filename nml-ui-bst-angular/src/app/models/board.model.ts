import type { Sector } from './sector.model';

export interface Board {
  id: number;
  name: string;
  mapImageUrl: string | null;
  svgOverlayUrl: string | null;
  sectors: { [key: number]: Sector };
}
