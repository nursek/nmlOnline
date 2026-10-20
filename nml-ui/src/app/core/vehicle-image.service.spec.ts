import { TestBed } from '@angular/core/testing';
import { VehicleImageService } from './vehicle-image.service';

describe('VehicleImageService', () => {
  let service: VehicleImageService;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [VehicleImageService] });
    service = TestBed.inject(VehicleImageService);
  });

  it('mappe le type vers la vignette et replie après une erreur', () => {
    expect(service.image('VTT_LEGER')?.src).toBe('/assets/_opt/shop/vehicles/vtt_leger.320.webp');

    service.onError('VTT_LEGER');

    expect(service.image('VTT_LEGER')).toBeNull();
  });
});
