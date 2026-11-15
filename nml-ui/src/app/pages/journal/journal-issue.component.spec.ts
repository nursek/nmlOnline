import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { JournalIssueComponent } from './journal-issue.component';
import { AuthService } from '../../services/auth.service';
import { InstanceService } from '../../services/instance.service';
import { GAZETTE_ISSUE } from '../../shared/gazette/gazette.fixture';

describe('JournalIssueComponent', () => {
  const isAdmin = signal(false);
  let http: HttpTestingController;

  beforeEach(async () => {
    isAdmin.set(false);
    await TestBed.configureTestingModule({
      imports: [JournalIssueComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { isAdmin } },
        { provide: InstanceService, useValue: { selectedRole: signal('PLAYER') } },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  it('rend l’édition publiée et le bouton d’impression', async () => {
    const fixture = TestBed.createComponent(JournalIssueComponent);
    fixture.componentRef.setInput('turn', 5);
    fixture.detectChanges();
    http.expectOne('/api/game/gazette/5').flush(GAZETTE_ISSUE);
    await fixture.whenStable();
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('.journal-name')?.textContent).toContain('Brigade des Morts');
    expect(element.querySelector('.readonly-allow')).not.toBeNull();
    expect(element.querySelector('a[href="/admin/journal/5"]')).toBeNull();
  });

  it('affiche l’état introuvable', async () => {
    const fixture = TestBed.createComponent(JournalIssueComponent);
    fixture.componentRef.setInput('turn', 5);
    fixture.detectChanges();
    http
      .expectOne('/api/game/gazette/5')
      .flush({}, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('introuvable');
  });

  it('propose le lien d’édition au MJ', async () => {
    isAdmin.set(true);
    const fixture = TestBed.createComponent(JournalIssueComponent);
    fixture.componentRef.setInput('turn', 5);
    fixture.detectChanges();
    http.expectOne('/api/game/gazette/5').flush(GAZETTE_ISSUE);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('a[href="/admin/journal/5"]')).not.toBeNull();
  });
});
