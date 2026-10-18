import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { RankingsPanelComponent } from './rankings-panel.component';
import { AuthService } from '../../services/auth.service';
import { Rankings } from '../../models';

const RANKINGS: Rankings = {
  turn: 2,
  military: [
    { playerId: 1, name: 'Alice', power: 300, comment: 'Flanc gauche' },
    { playerId: 2, name: 'Bob', power: 120, comment: null },
  ],
  economic: [
    { playerId: 2, name: 'Bob', power: 5000, comment: null },
    { playerId: 1, name: 'Alice', power: 2000, comment: null },
  ],
};

describe('RankingsPanelComponent — indice du MJ', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RankingsPanelComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { isAdmin: () => true } },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  it('édite puis enregistre un indice nettoyé', async () => {
    const fixture = TestBed.createComponent(RankingsPanelComponent);
    fixture.detectChanges();
    http.expectOne('/api/rankings').flush(RANKINGS);
    await fixture.whenStable();

    const component = fixture.componentInstance;
    expect(component.available()).toBe(true);

    component.startEdit(RANKINGS.military[0]);
    await fixture.whenStable();
    expect(component.editingPlayerId()).toBe(1);

    component.onDraftInput({
      target: { textContent: '  Aile droite fragile  ' },
    } as unknown as Event);
    const saving = component.save(RANKINGS.military[0]);

    const put = http.expectOne('/api/admin/players/1/ranking-comment');
    expect(put.request.method).toBe('PUT');
    expect(put.request.body).toEqual({ comment: 'Aile droite fragile' });
    put.flush({});

    await saving;
    fixture.detectChanges();
    http.expectOne('/api/rankings').flush(RANKINGS);
    await fixture.whenStable();
    expect(component.editingPlayerId()).toBeNull();
  });

  it('refuse un indice de plus de 500 caractères sans appel API', async () => {
    const fixture = TestBed.createComponent(RankingsPanelComponent);
    fixture.detectChanges();
    http.expectOne('/api/rankings').flush(RANKINGS);
    await fixture.whenStable();

    const component = fixture.componentInstance;
    component.startEdit(RANKINGS.military[0]);
    component.onDraftInput({ target: { textContent: 'x'.repeat(501) } } as unknown as Event);

    await component.save(RANKINGS.military[0]);

    http.expectNone('/api/admin/players/1/ranking-comment');
  });
});
