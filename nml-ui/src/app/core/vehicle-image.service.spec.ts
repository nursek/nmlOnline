import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { VehicleImageService } from './vehicle-image.service';
import { PlayerService } from '../services/player.service';

describe('VehicleImageService', () => {
  let service: VehicleImageService;
  const player = signal<{ race: string | null } | null>(null);

  beforeEach(() => {
    player.set(null);
    TestBed.configureTestingModule({
      providers: [VehicleImageService, { provide: PlayerService, useValue: { player } }],
    });
    service = TestBed.inject(VehicleImageService);
  });

  it('mappe le type vers la vignette et replie après une erreur', () => {
    expect(service.image('VTT_LEGER')?.src).toBe('/assets/_opt/shop/vehicles/vtt_leger.320.webp');

    service.onError('VTT_LEGER');

    expect(service.image('VTT_LEGER')).toBeNull();
  });

  it('préfère la vignette de la race, repli sur la commune', () => {
    player.set({ race: 'ORKS' });

    expect(service.image('VTT_LEGER')?.src).toBe(
      '/assets/_opt/shop/vehicles/orks/vtt_leger.320.webp',
    );

    service.onError('VTT_LEGER');

    expect(service.image('VTT_LEGER')?.src).toBe('/assets/_opt/shop/vehicles/vtt_leger.320.webp');
  });
});
