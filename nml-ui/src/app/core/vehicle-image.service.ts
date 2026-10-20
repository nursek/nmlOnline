import { Injectable, signal } from '@angular/core';
import { optimizedImage } from './optimized-image';
import type { OptimizedImage } from './optimized-image';

@Injectable({ providedIn: 'root' })
export class VehicleImageService {
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

  // ponytail: toLowerCase() et non slugify() — le nom d'énum Java (VTT_LEGER) garde ses underscores.
  private candidate(vehicleType: string): string | null {
    if (!vehicleType) return null;
    const url = `assets/shop/vehicles/${vehicleType.toLowerCase()}.png`;
    return this.broken().has(url) ? null : url;
  }
}
