import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { JournalListComponent } from './journal-list.component';
import { GAZETTE_SUMMARIES } from '../../shared/gazette/gazette.fixture';

describe('JournalListComponent', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JournalListComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  it('liste les éditions publiées avec leur lien', async () => {
    const fixture = TestBed.createComponent(JournalListComponent);
    fixture.detectChanges();
    http
      .expectOne('/api/game/gazette')
      .flush(GAZETTE_SUMMARIES.filter((issue) => issue.status === 'PUBLISHED'));
    await fixture.whenStable();
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelectorAll('.issue-card').length).toBe(1);
    expect(element.textContent).toContain('LA CHUTE DE LURIO');
    expect(element.querySelector('.issue-card')?.getAttribute('href')).toBe('/journal/4');
  });

  it('affiche l’état vide', async () => {
    const fixture = TestBed.createComponent(JournalListComponent);
    fixture.detectChanges();
    http.expectOne('/api/game/gazette').flush([]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aucune édition publiée');
  });
});
