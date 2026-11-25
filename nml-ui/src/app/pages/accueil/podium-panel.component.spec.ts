import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { PodiumPanelComponent } from './podium-panel.component';
import { PODIUM } from '../podium/podium.fixture';

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
