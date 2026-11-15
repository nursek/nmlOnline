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
import type { AdminPreparationStatus, GameInstance } from '../../models';

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
          },
        },
        {
          provide: AdminService,
          useValue: { players: () => [], loading: () => false, reloadPlayers: jest.fn() },
        },
        {
          provide: ActiveBoardService,
          useValue: { board: () => null, loading: () => false, reload: jest.fn() },
        },
        {
          provide: InstanceService,
          useValue: { selected: () => instance, invalidate: jest.fn() },
        },
        { provide: MatDialog, useValue: { open: dialogOpen } },
        { provide: MatSnackBar, useValue: { open: jest.fn() } },
      ],
    })
      .overrideComponent(GameSetupComponent, { remove: { imports: [MatDialogModule] } })
      .compileComponents();
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
