import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PodiumComponent } from './podium.component';
import { AuthService } from '../../services/auth.service';
import { InstanceService } from '../../services/instance.service';
import { PlayerDirectoryService } from '../../services/player-directory.service';
import { GamePodium, PodiumPlace } from '../../models';

function place(overrides: Partial<PodiumPlace> = {}): PodiumPlace {
  return {
    place: 1,
    playerId: 1,
    playerName: 'nursek',
    race: 'ORKS',
    characterName: 'Ratcatcher',
    eliminatedTurn: null,
    eliminationCause: null,
    definitive: false,
    killerPlayerId: null,
    killerName: null,
    kills: 2,
    assists: 1,
    alive: true,
    victims: ['lurio'],
    assistedVictims: [],
    ...overrides,
  };
}

const PODIUM: GamePodium = {
  finished: true,
  finalTurn: 7,
  finishedAt: '2026-11-13T20:14:00Z',
  totalPlayers: 3,
  activePlayers: 1,
  posthumous: false,
  championAdjusted: false,
  lastGazetteTurn: null,
  champion: place(),
  places: [
    place(),
    place({
      place: 2,
      playerId: 2,
      playerName: 'lurio',
      alive: false,
      eliminatedTurn: 7,
      eliminationCause: 'MAIN_CHARACTER_KILLED',
      definitive: true,
      killerName: 'nursek',
      kills: 1,
      assists: 0,
      victims: ['trazyn'],
    }),
    place({
      place: 3,
      playerId: 3,
      playerName: 'trazyn',
      alive: false,
      eliminatedTurn: 6,
      eliminationCause: 'HEADQUARTERS_CAPTURED',
      definitive: false,
      killerName: 'lurio',
      kills: 0,
      assists: 0,
      victims: [],
    }),
  ],
};

const POSTHUMOUS: GamePodium = {
  ...PODIUM,
  activePlayers: 0,
  posthumous: true,
  champion: place({
    place: 1,
    playerId: 2,
    playerName: 'lurio',
    alive: false,
    eliminatedTurn: 7,
    eliminationCause: 'MAIN_CHARACTER_KILLED',
    definitive: true,
    killerName: 'nursek',
  }),
  places: [
    place({
      place: 1,
      playerId: 2,
      playerName: 'lurio',
      alive: false,
      eliminatedTurn: 7,
      eliminationCause: 'MAIN_CHARACTER_KILLED',
      definitive: true,
      killerName: 'nursek',
    }),
    place({
      place: 2,
      playerId: 3,
      playerName: 'trazyn',
      alive: false,
      eliminatedTurn: 7,
      eliminationCause: 'HEADQUARTERS_CAPTURED',
      definitive: false,
      killerName: 'lurio',
      kills: 0,
      assists: 0,
      victims: [],
    }),
  ],
};

describe('PodiumComponent', () => {
  const isAdmin = signal(false);
  const selectedRole = signal<string | null>('PLAYER');
  let http: HttpTestingController;

  beforeEach(async () => {
    isAdmin.set(false);
    selectedRole.set('PLAYER');
    await TestBed.configureTestingModule({
      imports: [PodiumComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { isAdmin } },
        { provide: InstanceService, useValue: { selectedRole } },
        { provide: PlayerDirectoryService, useValue: { colorMap: signal(new Map()) } },
        { provide: MatSnackBar, useValue: { open: jest.fn() } },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  it('affiche le classement complet et les causes lore', async () => {
    const fixture = TestBed.createComponent(PodiumComponent);
    fixture.detectChanges();
    http.expectOne('/api/game/podium').flush(PODIUM);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.champion-name')?.textContent).toContain('nursek');
    expect(fixture.nativeElement.querySelectorAll('tbody tr').length).toBe(3);
    expect(fixture.nativeElement.textContent).toContain('Mort définitive');
    expect(fixture.nativeElement.textContent).toContain('QG conquis — retour possible');
    expect(fixture.nativeElement.querySelector('.admin-card')).toBeNull();
  });

  it('affiche le bloc MJ et désigne un champion posthume', async () => {
    isAdmin.set(true);
    const fixture = TestBed.createComponent(PodiumComponent);
    fixture.detectChanges();
    http.expectOne('/api/game/podium').flush(POSTHUMOUS);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.admin-card')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Vainqueur posthume');

    fixture.componentInstance.selectedWinnerId.set(3);
    const pending = fixture.componentInstance.adjustWinner();
    const put = http.expectOne('/api/admin/game/winner');
    expect(put.request.method).toBe('PUT');
    expect(put.request.body).toEqual({ playerId: 3 });
    put.flush({});
    await pending;
    TestBed.tick();
    http.expectOne('/api/game/podium').flush(POSTHUMOUS);
    await fixture.whenStable();
  });
});
