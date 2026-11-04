import { inject, Injectable, signal } from '@angular/core';
import { optimizedImage } from './optimized-image';
import type { OptimizedImage } from './optimized-image';
import { PlayerService } from '../services/player.service';

@Injectable({ providedIn: 'root' })
export class VehicleImageService {
  private readonly playerService = inject(PlayerService);
  private readonly broken = signal(new Set<string>());

  image(vehicleType: string): OptimizedImage | null {
    const url = this.candidate(vehicleType);
    return url ? optimizedImage(url) : null;
  }

  onError(vehicleType: string): void {
    const url = this.candidate(vehicleType);
    if (!url) return;
    this.broken.update((set) => new Set(set).add(url));
  }

  /** Vignette de la race d'abord, repli sur le fichier commun. */
  private candidate(vehicleType: string): string | null {
    if (!vehicleType) return null;
    // ponytail: toLowerCase() et non slugify() — le nom d'énum Java (VTT_LEGER) garde ses underscores.
    const file = `${vehicleType.toLowerCase()}.png`;
    const race = this.playerService.player()?.race?.toLowerCase();
    const candidates = race
      ? [`assets/shop/vehicles/${race}/${file}`, `assets/shop/vehicles/${file}`]
      : [`assets/shop/vehicles/${file}`];
    return candidates.find((url) => !this.broken().has(url)) ?? null;
  }
}
