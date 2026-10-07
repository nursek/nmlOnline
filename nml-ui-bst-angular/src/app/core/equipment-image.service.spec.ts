import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { EquipmentImageService } from './equipment-image.service';
import { PlayerService } from '../services/player.service';

describe('EquipmentImageService', () => {
  const player = signal<{ race: string | null } | null>({ race: 'NECRONS' });
  let service: EquipmentImageService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [EquipmentImageService, { provide: PlayerService, useValue: { player } }],
    });
    service = TestBed.inject(EquipmentImageService);
  });

  it('préfixe le dossier de la race puis replie sur le fichier commun', () => {
    expect(service.image({ name: 'Gauss Blaster' })?.src).toBe(
      '/assets/_opt/shop/equipment/necrons/gauss-blaster.320.webp',
    );

    service.onError({ name: 'Gauss Blaster' });

    expect(service.image({ name: 'Gauss Blaster' })?.src).toBe(
      '/assets/_opt/shop/equipment/gauss-blaster.320.webp',
    );
  });

  it("ne masque pas les autres vignettes quand une image de la race manque", () => {
    service.onError({ name: 'Voltaic Staff' });

    expect(service.image({ name: 'Gauss Blaster' })?.src).toBe(
      '/assets/_opt/shop/equipment/necrons/gauss-blaster.320.webp',
    );
  });
});
