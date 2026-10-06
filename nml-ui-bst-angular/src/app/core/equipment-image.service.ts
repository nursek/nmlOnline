import { inject, Injectable, signal } from '@angular/core';
import { equipmentImageCandidates } from './equipment-label';
import type { LocalizedEquipment } from './equipment-label';
import { PlayerService } from '../services/player.service';

@Injectable({ providedIn: 'root' })
export class EquipmentImageService {
  private readonly playerService = inject(PlayerService);
  private readonly broken = signal(new Set<string>());

  url(equipment: LocalizedEquipment): string {
    const race = this.playerService.player()?.race;
    return equipmentImageCandidates(equipment, race).find((url) => !this.broken().has(url)) ?? '';
  }

  onError(equipment: LocalizedEquipment): void {
    const url = this.url(equipment);
    if (!url) return;
    this.broken.update((set) => new Set(set).add(url));
  }
}
