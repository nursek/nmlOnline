import { TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';
import { JoueurComponent } from './joueur.component';
import { reserveVehicleGroups } from './joueur.helpers';
import { PlayerService } from '../../services/player.service';
import { PlayerActionsService } from '../../services/player-actions.service';
import { ActiveBoardService } from '../../services/active-board.service';
import { PlayerDirectoryService } from '../../services/player-directory.service';
import { AuthService } from '../../services/auth.service';
import { ApiService } from '../../services/api.service';
import { AllianceComponent } from '../alliance/alliance.component';
import { ExchangeDialogComponent } from './exchange-dialog.component';
import { Vehicle, Sector } from '../../models';
import { UnitDetailDialogComponent } from './unit-detail-dialog.component';
import { SellResourceDialogComponent } from './sell-resource-dialog.component';
import { AutoEquipDialogComponent } from './auto-equip-dialog.component';

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
    pilotId: null,
    pilotName: null,
    passengerIds: [],
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
    vehicles: [vehicle(1), vehicle(2, 99)],
    ownerId: 1,
    boardId: 10,
    color: '#fff',
    resource: null,
    neighbors: [],
    x: 0,
    y: 0,
  };
}

describe('JoueurComponent — déploiement véhicule', () => {
  let placeVehicleSpy: jest.SpyInstance;
  let openDialogSpy: jest.Mock;
  const snackBar = { open: jest.fn() };
  const routeParams = new BehaviorSubject(convertToParamMap({}));

  beforeEach(async () => {
    placeVehicleSpy = jest.fn().mockResolvedValue(vehicle(42));
    openDialogSpy = jest.fn(() => ({ afterClosed: () => of(sector()) }));
    routeParams.next(convertToParamMap({}));

    await TestBed.configureTestingModule({
      imports: [JoueurComponent],
      providers: [
        {
          provide: MatDialog,
          useValue: { open: openDialogSpy },
        },
        { provide: MatSnackBar, useValue: snackBar },
        {
          provide: ActivatedRoute,
          useValue: { queryParamMap: routeParams.asObservable() },
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
            vehicles: () => [{ ...vehicle(1), sectorNumber: 5 }],
            vehiclesLoading: () => false,
            reserveUnits: () => [],
            reserveUnitsLoading: () => false,
            currentTurn: () => 3,
            loadCurrent: jest.fn(),
            loadVehicles: jest.fn(),
            loadReserveUnits: jest.fn(),
            loadCurrentTurn: jest.fn(),
            placeVehicle: placeVehicleSpy,
            placeUnit: jest.fn().mockResolvedValue(null),
            setVehicleCrew: jest.fn().mockResolvedValue(vehicle(1)),
            moveBuilding: jest.fn().mockResolvedValue(true),
          },
        },
        {
          provide: AuthService,
          useValue: { user: () => ({ id: 1, username: 'tester' }), initialized: () => true },
        },
        {
          provide: PlayerActionsService,
          useValue: {
            actions: () => [],
            loading: () => false,
            error: () => null,
            hasActions: () => false,
            loadActions: jest.fn(),
            undoFrom: jest.fn().mockResolvedValue(true),
            undoAll: jest.fn().mockResolvedValue(true),
            harvest: jest.fn().mockResolvedValue(true),
          },
        },
        { provide: ApiService, useValue: {} },
        { provide: ActiveBoardService, useValue: { sectors: () => [], board: () => null } },
        { provide: PlayerDirectoryService, useValue: { players: () => [] } },
      ],
    })
      .overrideComponent(JoueurComponent, {
        remove: { imports: [MatDialogModule] },
        add: { imports: [] },
      })
      .compileComponents();
  });

  it('ouvre le détail d’unité, la vente, l’échange et l’auto-équipement', () => {
    const fixture = TestBed.createComponent(JoueurComponent);
    fixture.detectChanges();
    const comp = fixture.componentInstance;
    const unit = { id: 1, number: 1 } as never;
    const s = sector();

    comp.openUnitDialog(unit, s);
    expect(openDialogSpy).toHaveBeenCalledWith(
      UnitDetailDialogComponent,
      expect.objectContaining({ data: { unit, sectorNumber: 5, sectorName: 'Quartier 5' } }),
    );

    comp.openSellDialog({ id: 7, name: 'Bois', quantity: 2 } as never);
    expect(openDialogSpy).toHaveBeenCalledWith(SellResourceDialogComponent, expect.anything());

    comp.openExchangeDialog();
    expect(openDialogSpy).toHaveBeenCalledWith(
      ExchangeDialogComponent,
      expect.objectContaining({ data: expect.objectContaining({ playerId: 1 }) }),
    );

    comp.openAutoEquip();
    expect(openDialogSpy).toHaveBeenCalledWith(AutoEquipDialogComponent, expect.anything());

    comp.openCacheEquipments(null);
    expect(openDialogSpy).toHaveBeenCalledTimes(4);
  });

  it('déplace un bâtiment après sélection du secteur', async () => {
    const fixture = TestBed.createComponent(JoueurComponent);
    fixture.detectChanges();
    const comp = fixture.componentInstance;
    const open = jest.spyOn(
      (comp as unknown as { snackBar: { open: () => void } }).snackBar,
      'open',
    );

    comp.openBuildingMove({ id: 9, displayName: 'Banque' } as never);
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(open).toHaveBeenCalledWith('Bâtiment déplacé', 'Fermer', { duration: 3000 });
  });

  it('met à jour puis vide l’équipage d’un véhicule', async () => {
    openDialogSpy.mockReturnValueOnce({ afterClosed: () => of({ pilotId: 2, passengerIds: [3] }) });
    const fixture = TestBed.createComponent(JoueurComponent);
    fixture.detectChanges();
    const comp = fixture.componentInstance;
    const open = jest.spyOn(
      (comp as unknown as { snackBar: { open: () => void } }).snackBar,
      'open',
    );
    const v = vehicle(1);
    const s = sector();

    comp.openVehicleCrew(v, s);
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(open).toHaveBeenCalledWith('Équipage mis à jour', 'Fermer', { duration: 3000 });

    comp.clearVehicleCrew(v);
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(open).toHaveBeenCalledWith('Véhicule vidé', 'Fermer', { duration: 3000 });
  });

  it('récolte seulement avec des secteurs, annule les actions', async () => {
    const fixture = TestBed.createComponent(JoueurComponent);
    fixture.detectChanges();
    const comp = fixture.componentInstance;
    const open = jest.spyOn(
      (comp as unknown as { snackBar: { open: () => void } }).snackBar,
      'open',
    );

    comp.harvest('HARVEST_MONEY', []);
    expect(open).not.toHaveBeenCalled();

    comp.harvest('HARVEST_MONEY', [5]);
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(open).toHaveBeenCalledWith('Récolte effectuée', 'Fermer', { duration: 3000 });

    comp.undoAll();
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(open).toHaveBeenCalledWith('Actions annulées', 'Fermer', { duration: 3000 });
  });

  it('appelle placeVehicle quand la modale renvoie un secteur', () => {
    const fixture = TestBed.createComponent(JoueurComponent);
    fixture.detectChanges();
    const comp = fixture.componentInstance;

    comp.openVehiclePlacementModal(reserveVehicleGroups([vehicle(42)])[0]);

    expect(placeVehicleSpy).toHaveBeenCalledTimes(1);
    const [vehicleId, boardId, sectorNumber] = placeVehicleSpy.mock.calls[0];
    expect(vehicleId).toBe(42);
    expect(boardId).toBe(10);
    expect(sectorNumber).toBe(5);
  });

  it('ne compte comme déployés que les véhicules du joueur', () => {
    const fixture = TestBed.createComponent(JoueurComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.vehicleSummaries()).toEqual([
      { vehicleType: 'TANK', displayName: 'Tank de combat', count: 1 },
    ]);
  });

  it("ouvre la fenêtre d'alliance depuis un lien de notification", () => {
    routeParams.next(convertToParamMap({ alliance: '1' }));
    TestBed.createComponent(JoueurComponent);

    expect(openDialogSpy).toHaveBeenCalledWith(AllianceComponent, expect.anything());
  });

  it("ouvre la fenêtre d'échange depuis un lien de notification", () => {
    routeParams.next(convertToParamMap({ echange: '1' }));
    TestBed.createComponent(JoueurComponent);

    expect(openDialogSpy).toHaveBeenCalledWith(ExchangeDialogComponent, expect.anything());
  });
});
