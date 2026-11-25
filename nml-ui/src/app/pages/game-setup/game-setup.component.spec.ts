import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { GameSetupComponent } from './game-setup.component';
import { ApiService } from '../../services/api.service';
import { AdminService } from '../../services/admin.service';
import { ActiveBoardService } from '../../services/active-board.service';
import { InstanceService } from '../../services/instance.service';
import type { AdminPreparationStatus, Board, GameInstance, Player } from '../../models';

const statuses: AdminPreparationStatus[] = [
  {
    playerId: 1,
    name: 'Alice',
    accountLinked: true,
    sectorCount: 2,
    characterPlaced: true,
    buildingsPlaced: 3,
    buildingsTotal: 3,
    unitsTotal: 100,
    unitsUnclassified: 0,
    ready: false,
  },
  {
    playerId: 2,
    name: 'Bob',
    accountLinked: true,
    sectorCount: 2,
    characterPlaced: true,
    buildingsPlaced: 3,
    buildingsTotal: 3,
    unitsTotal: 100,
    unitsUnclassified: 0,
    ready: true,
  },
];

const boardFixture = {
  id: 3,
  name: 'Demo tour 0',
  sectors: {
    1: { number: 1, ownerId: 1 },
    2: { number: 2, ownerId: null },
    3: { number: 3, ownerId: null },
  },
} as unknown as Board;
const playersFixture = [
  {
    id: 1,
    name: 'Alice',
    userId: null,
    sectors: [{ number: 2 }, { number: 1 }],
    buildings: [{ id: 1, sectorNumber: null }],
    character: null,
  } as unknown as Player,
];

const instance: GameInstance = {
  id: 3,
  name: 'Demo tour 0',
  currentTurn: 0,
  playerCount: 2,
  playerId: null,
  playerName: null,
  eliminated: false,
  role: 'ADMIN',
  participant: false,
};

describe('GameSetupComponent — avertissement de démarrage', () => {
  let dialogOpen: jest.Mock;
  let adminStartGame: jest.Mock;
  const invalidateInstances = jest.fn();

  beforeEach(async () => {
    dialogOpen = jest.fn(() => ({ afterClosed: () => of(false) }));
    adminStartGame = jest.fn(() => of(void 0));

    await TestBed.configureTestingModule({
      imports: [GameSetupComponent],
      providers: [
        provideRouter([]),
        {
          provide: ApiService,
          useValue: {
            adminGetPreparationStatus: () => of(statuses),
            adminGetBoardAdmins: () => of([]),
            adminStartGame,
            adminCreateTemplatePlayer: jest.fn(() => of(void 0)),
            adminAssignStartingSectors: jest.fn(() => of(void 0)),
            adminGrantBoardAdmin: jest.fn(() => of(void 0)),
            adminRevokeBoardAdmin: jest.fn(() => of(void 0)),
            adminLinkAccount: jest.fn(() => of(void 0)),
            adminUnlinkAccount: jest.fn(() => of(void 0)),
            adminResetPreparation: jest.fn(() => of(void 0)),
          },
        },
        {
          provide: AdminService,
          useValue: {
            players: signal(playersFixture).asReadonly(),
            loading: signal(false).asReadonly(),
            reloadPlayers: jest.fn(),
          },
        },
        {
          provide: ActiveBoardService,
          useValue: {
            board: signal(boardFixture).asReadonly(),
            loading: signal(false).asReadonly(),
            reload: jest.fn(),
          },
        },
        {
          provide: InstanceService,
          useValue: { selected: () => instance, invalidate: invalidateInstances },
        },
        { provide: MatDialog, useValue: { open: dialogOpen } },
        { provide: MatSnackBar, useValue: { open: jest.fn() } },
      ],
    })
      .overrideComponent(GameSetupComponent, { remove: { imports: [MatDialogModule] } })
      .compileComponents();
  });

  it('calcule les éléments non placés, les secteurs triés et le propriétaire d’un spot', () => {
    const page = TestBed.createComponent(GameSetupComponent).componentInstance;
    const player = playersFixture[0];

    expect(page.unplacedCount(player)).toBe(1);
    expect(page.sectorNumbers(player)).toEqual([1, 2]);
    expect(page.spotOwner([1, 2])).toBe('Alice');
    expect(page.spotOwner([3])).toBe('Libre');
  });

  it('crée un joueur prêt depuis le dialogue et recharge les listes', async () => {
    dialogOpen.mockReturnValueOnce({
      afterClosed: () => of({ name: 'Alice', race: 'ORKS', sectorNumbers: [1] }),
    });
    const api = TestBed.inject(ApiService) as unknown as {
      adminCreateTemplatePlayer: jest.Mock;
    };
    const fixture = TestBed.createComponent(GameSetupComponent);

    await fixture.componentInstance.createTemplatePlayer();

    expect(api.adminCreateTemplatePlayer).toHaveBeenCalledWith({
      name: 'Alice',
      race: 'ORKS',
      sectorNumbers: [1],
    });
    fixture.destroy();
  });

  it('assigne les secteurs choisis puis retire un MJ confirmé', async () => {
    dialogOpen.mockReturnValue({ afterClosed: () => of([1, 2]) });
    const api = TestBed.inject(ApiService) as unknown as {
      adminAssignStartingSectors: jest.Mock;
      adminRevokeBoardAdmin: jest.Mock;
    };
    const fixture = TestBed.createComponent(GameSetupComponent);
    const page = fixture.componentInstance;

    await page.assignSectors(playersFixture[0]);
    expect(api.adminAssignStartingSectors).toHaveBeenCalledWith(1, [1, 2]);

    dialogOpen.mockReturnValue({ afterClosed: () => of(true) });
    await page.removeMj({ userId: 9, username: 'nursek' } as never);
    expect(api.adminRevokeBoardAdmin).toHaveBeenCalledWith(3, 9);
    fixture.destroy();
  });

  it('relie un compte choisi dans le sélecteur', async () => {
    dialogOpen.mockReturnValue({ afterClosed: () => of({ id: 9, username: 'user9' }) });
    const api = TestBed.inject(ApiService) as unknown as {
      adminLinkAccount: jest.Mock;
    };
    const fixture = TestBed.createComponent(GameSetupComponent);
    const player = { id: 1, name: 'Alice', userId: null } as never;

    await fixture.componentInstance.linkAccount(player);

    expect(api.adminLinkAccount).toHaveBeenCalledWith(1, 'user9');
    fixture.destroy();
  });

  it('délie un compte après confirmation', async () => {
    dialogOpen.mockReturnValue({ afterClosed: () => of(true) });
    const api = TestBed.inject(ApiService) as unknown as {
      adminUnlinkAccount: jest.Mock;
    };
    const fixture = TestBed.createComponent(GameSetupComponent);
    const player = { id: 1, name: 'Alice', userId: 9 } as never;

    await fixture.componentInstance.unlinkAccount(player);

    expect(api.adminUnlinkAccount).toHaveBeenCalledWith(1);
    fixture.destroy();
  });

  it('réinitialise le placement après confirmation et recharge les joueurs', async () => {
    dialogOpen.mockReturnValue({ afterClosed: () => of(true) });
    const api = TestBed.inject(ApiService) as unknown as {
      adminResetPreparation: jest.Mock;
    };
    const admin = TestBed.inject(AdminService) as unknown as { reloadPlayers: jest.Mock };
    const fixture = TestBed.createComponent(GameSetupComponent);

    await fixture.componentInstance.resetPreparation({ id: 1, name: 'Alice' } as never);

    expect(api.adminResetPreparation).toHaveBeenCalledWith(1);
    expect(admin.reloadPlayers).toHaveBeenCalled();
    fixture.destroy();
  });

  it('démarre la partie après confirmation, invalide la partie active et ouvre la gestion du tour', async () => {
    dialogOpen.mockReturnValue({ afterClosed: () => of(true) });
    const fixture = TestBed.createComponent(GameSetupComponent);

    // jsdom n'implémente pas la navigation : le contrat testé est l'appel API + l'invalidation.
    const consoleError = jest.spyOn(console, 'error').mockImplementation(() => undefined);
    await fixture.componentInstance.startGame().catch(() => undefined);
    consoleError.mockRestore();

    expect(adminStartGame).toHaveBeenCalled();
    expect(invalidateInstances).toHaveBeenCalled();
    fixture.destroy();
  });

  it('annonce un joueur complet non déclaré prêt sans parler de placement incomplet', async () => {
    const fixture = TestBed.createComponent(GameSetupComponent);

    await fixture.componentInstance.startGame();

    const message = dialogOpen.mock.calls[0][1].data.message as string;
    expect(message).toContain('1/2 prêts');
    expect(message).toContain('prêt non déclaré');
    expect(message).not.toContain('placement incomplet');
    expect(adminStartGame).not.toHaveBeenCalled();
    fixture.destroy();
  });
});
