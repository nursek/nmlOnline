import { TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { of } from 'rxjs';
import { JoueurComponent } from './joueur.component';
import { PlayerService } from '../../services/player.service';
import { AuthService } from '../../services/auth.service';
import { ApiService } from '../../services/api.service';
import { Vehicle, Sector } from '../../models';

function vehicle(id: number, playerId = 1): Vehicle {
  return {
    id,
    playerId,
    vehicleType: 'TANK',
    displayName: 'Tank de combat',
    pdf: 125,
    defense: 250,
    isDestroyed: false,
    speed: 1,
    capacity: 0,
    passengerCount: 0,
    hasPilot: false,
    sectorNumber: null,
    boardId: null,
  };
}

function sector(): Sector {
  return {
    number: 5,
    name: 'Quartier 5',
    income: 2000,
    army: [],
    stats: undefined,
    buildings: [],
    character: null,
    vehicles: [],
    ownerId: 1,
    boardId: 10,
    color: '#fff',
    resource: null,
    neighbors: [],
    x: 0,
    y: 0,
    vehicles: [vehicle(1), vehicle(2, 99)],
  };
}

describe('JoueurComponent — déploiement véhicule', () => {
  let placeVehicleSpy: jest.SpyInstance;

  beforeEach(async () => {
    placeVehicleSpy = jest.fn().mockResolvedValue(vehicle(42));

    await TestBed.configureTestingModule({
      imports: [JoueurComponent],
      providers: [
        {
          provide: MatDialog,
          useValue: {
            open: () => ({ afterClosed: () => of(sector()) }) as unknown as MatDialogRef<unknown>,
          },
        },
        {
          provide: PlayerService,
          useValue: {
            player: () => ({
              id: 1,
              sectors: [sector()],
              equipments: [],
              resources: [],
              buildings: [],
              character: null,
              stats: {
                money: 100000,
                totalIncome: 2000,
                totalVehiclesValue: 0,
                totalEquipmentValue: 0,
                totalOffensivePower: 0,
                totalDefensivePower: 0,
                globalPower: 0,
                totalEconomyPower: 0,
                totalAtk: 0,
                totalPdf: 0,
                totalPdc: 0,
                totalDef: 0,
                totalArmor: 0,
              },
            }),
            loading: () => false,
            error: () => null,
            undeployedVehicles: () => [vehicle(42)],
            vehiclesLoading: () => false,
            currentTurn: () => 3,
            loadCurrent: jest.fn(),
            loadVehicles: jest.fn(),
            loadCurrentTurn: jest.fn(),
            placeVehicle: placeVehicleSpy,
          },
        },
        { provide: AuthService, useValue: { user: () => ({ id: 1, username: 'tester' }) } },
        { provide: ApiService, useValue: {} },
      ],
    })
      .overrideComponent(JoueurComponent, {
        remove: { imports: [MatDialogModule] },
        add: { imports: [] },
      })
      .compileComponents();
  });

  it('appelle placeVehicle quand la modale renvoie un secteur', () => {
    const fixture = TestBed.createComponent(JoueurComponent);
    fixture.detectChanges();
    const comp = fixture.componentInstance;

    comp.openPlacementModal(vehicle(42));

    expect(placeVehicleSpy).toHaveBeenCalledTimes(1);
    const [vehicleId, boardId, sectorNumber] = placeVehicleSpy.mock.calls[0];
    expect(vehicleId).toBe(42);
    expect(boardId).toBe(10);
    expect(sectorNumber).toBe(5);
  });

  it('ne compte comme déployés que les véhicules du joueur', () => {
    const fixture = TestBed.createComponent(JoueurComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.deployedVehicleCount()).toBe(1);
  });
});
