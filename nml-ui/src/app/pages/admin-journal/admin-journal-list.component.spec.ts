import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AdminJournalListComponent } from './admin-journal-list.component';
import { GAZETTE_SUMMARIES } from '../../shared/gazette/gazette.fixture';

describe('AdminJournalListComponent', () => {
  let http: HttpTestingController;
  const snackBar = { open: jest.fn() };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminJournalListComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MatSnackBar, useValue: snackBar },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  it('supprime un brouillon après confirmation et recharge la liste', async () => {
    const fixture = TestBed.createComponent(AdminJournalListComponent);
    fixture.detectChanges();
    http.expectOne('/api/admin/game/gazette').flush(GAZETTE_SUMMARIES);
    await fixture.whenStable();
    const open = jest.spyOn(
      (fixture.componentInstance as unknown as { snackBar: { open: () => void } }).snackBar,
      'open',
    );
    jest.spyOn(window, 'confirm').mockReturnValue(true);

    const pending = fixture.componentInstance.deleteDraft(5);
    const del = http.expectOne('/api/admin/game/gazette/5');
    expect(del.request.method).toBe('DELETE');
    del.flush(null);
    await pending;
    TestBed.tick();
    http.expectOne('/api/admin/game/gazette').flush(GAZETTE_SUMMARIES);

    expect(open).toHaveBeenCalledWith('Brouillon supprimé', 'OK', { duration: 2500 });
    expect(fixture.componentInstance.error()).toBeNull();
  });

  it('ne supprime rien sans confirmation', async () => {
    const fixture = TestBed.createComponent(AdminJournalListComponent);
    fixture.detectChanges();
    http.expectOne('/api/admin/game/gazette').flush(GAZETTE_SUMMARIES);
    await fixture.whenStable();
    jest.spyOn(window, 'confirm').mockReturnValue(false);

    await fixture.componentInstance.deleteDraft(5);

    http.expectNone('/api/admin/game/gazette/5');
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
