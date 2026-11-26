import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { VehicleCrewModalComponent } from './vehicle-crew-modal.component';
import { VehicleImageService } from '../../core/vehicle-image.service';
import type { Sector, Vehicle } from '../../models';

describe('VehicleCrewModalComponent', () => {
  const vehicle = {
    id: 5,
    displayName: 'VTT léger',
    vehicleType: 'VTT_LEGER',
    capacity: 2,
    pilotId: 10,
    passengerIds: [11],
  } as Vehicle;
  const sector = { number: 5, army: [], characters: [], vehicles: [vehicle] } as unknown as Sector;
  const images = {
    image: jest.fn(() => ({ src: '/vtt.webp', srcset: '/vtt-2x.webp 2x' })),
    onError: jest.fn(),
  };
  const dialogRef = { close: jest.fn() };

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        { provide: VehicleImageService, useValue: images },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { vehicle, sector, playerId: 1 } },
      ],
    });
  });

  function component(): VehicleCrewModalComponent {
    return TestBed.runInInjectionContext(() => new VehicleCrewModalComponent());
  }

  it('expose l’image, le pilote et les sièges restants', () => {
    const page = component();

    expect(page.vehicleImage()?.src).toBe('/vtt.webp');
    expect(page.pilotId()).toBe(10);
    expect(page.seatsLeft()).toBe(1);
    expect(page.isPassenger(11)).toBe(true);
  });

  it('retire le pilote des passagers et respecte la capacité', () => {
    const page = component();

    page.selectPilot(11);
    expect(page.pilotId()).toBe(11);
    expect(page.passengerIds().has(11)).toBe(false);

    page.togglePassenger(12, true);
    expect(page.seatsLeft()).toBe(1);
    page.togglePassenger(13, true);
    expect(page.seatsLeft()).toBe(0);
    page.togglePassenger(14, true);
    expect(page.passengerIds().has(14)).toBe(false);
    page.togglePassenger(12, false);
    page.togglePassenger(14, true);
    expect(page.passengerIds().has(14)).toBe(true);
  });

  it('valide l’équipage ou annule', () => {
    const page = component();
    page.selectPilot(12);
    page.togglePassenger(11, true);

    page.confirm();
    expect(dialogRef.close).toHaveBeenCalledWith({ pilotId: 12, passengerIds: [11] });

    page.cancel();
    expect(dialogRef.close).toHaveBeenLastCalledWith(null);
  });

  it('signale une vignette cassée', () => {
    component().onImgError();
    expect(images.onError).toHaveBeenCalledWith('VTT_LEGER');
  });
});
