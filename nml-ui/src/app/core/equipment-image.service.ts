import { inject, Injectable, signal } from '@angular/core';
import { equipmentImageCandidates } from './equipment-label';
import type { LocalizedEquipment } from './equipment-label';
import { optimizedImage } from './optimized-image';
import type { OptimizedImage } from './optimized-image';
import { PlayerService } from '../services/player.service';

@Injectable({ providedIn: 'root' })
export class EquipmentImageService {
  private readonly playerService = inject(PlayerService);
  private readonly broken = signal(new Set<string>());

  image(equipment: LocalizedEquipment): OptimizedImage | null {
    const url = this.candidate(equipment);
    return url ? optimizedImage(url) : null;
  }

  onError(equipment: LocalizedEquipment): void {
    const url = this.candidate(equipment);
    if (!url) return;
    this.broken.update((set) => new Set(set).add(url));
  }

  private candidate(equipment: LocalizedEquipment): string | null {
    const race = this.playerService.player()?.race;
    return equipmentImageCandidates(equipment, race).find((url) => !this.broken().has(url)) ?? null;
  }
}
