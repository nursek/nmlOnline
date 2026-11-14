import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AdminJournalListComponent } from './admin-journal-list.component';
import { GAZETTE_SUMMARIES } from '../../shared/gazette/gazette.fixture';

describe('AdminJournalListComponent', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminJournalListComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MatSnackBar, useValue: { open: jest.fn() } },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  it('distingue brouillon et édition publiée', async () => {
    const fixture = TestBed.createComponent(AdminJournalListComponent);
    fixture.detectChanges();
    http.expectOne('/api/admin/game/gazette').flush(GAZETTE_SUMMARIES);
    await fixture.whenStable();
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelectorAll('.issue-card').length).toBe(2);
    expect(element.textContent).toContain('Brouillon');
    expect(element.textContent).toContain('Publiée');
    expect(element.querySelector('a[href="/admin/journal/5"]')).not.toBeNull();
    expect(element.querySelector('a[href="/admin/journal/4"]')).not.toBeNull();
    expect(element.querySelector('a[href="/journal/4"]')).not.toBeNull();
    expect(element.querySelector('.issue-card.draft button')).not.toBeNull();
  });
});
