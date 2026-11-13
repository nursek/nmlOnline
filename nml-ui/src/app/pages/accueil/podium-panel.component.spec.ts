import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { PodiumPanelComponent } from './podium-panel.component';
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

describe('PodiumPanelComponent', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PodiumPanelComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  it('affiche le champion et les trois marches', async () => {
    const fixture = TestBed.createComponent(PodiumPanelComponent);
    fixture.detectChanges();
    http.expectOne('/api/game/podium').flush(PODIUM);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.champion-name')?.textContent).toContain('nursek');
    expect(fixture.nativeElement.querySelectorAll('.mini-step').length).toBe(3);
    expect(fixture.nativeElement.textContent).toContain('Voir le podium complet');
  });

  it('affiche l’attente pendant la partie', async () => {
    const fixture = TestBed.createComponent(PodiumPanelComponent);
    fixture.detectChanges();
    http.expectOne('/api/game/podium').flush({
      ...PODIUM,
      finished: false,
      champion: null,
      places: [],
    });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('La partie est en cours');
  });

  it('propose de réessayer en cas d’erreur', async () => {
    const fixture = TestBed.createComponent(PodiumPanelComponent);
    fixture.detectChanges();
    http.expectOne('/api/game/podium').flush({}, { status: 500, statusText: 'Erreur' });
    await fixture.whenStable();
    fixture.detectChanges();

    const error = fixture.nativeElement.querySelector('.podium-error') as HTMLElement;
    expect(error).not.toBeNull();
    expect(error.querySelector('button')?.classList.contains('readonly-allow')).toBe(true);
  });
});
