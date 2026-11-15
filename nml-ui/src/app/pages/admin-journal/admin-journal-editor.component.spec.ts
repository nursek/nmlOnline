import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AdminJournalEditorComponent } from './admin-journal-editor.component';
import { PlayerDirectoryService } from '../../services/player-directory.service';
import { GAZETTE_ADMIN_ISSUE, LEAD_ARTICLE } from '../../shared/gazette/gazette.fixture';

describe('AdminJournalEditorComponent', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminJournalEditorComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MatSnackBar, useValue: { open: jest.fn() } },
        {
          provide: PlayerDirectoryService,
          useValue: { players: signal([{ id: 1, name: 'nursek' }, { id: 2, name: 'lurio' }]) },
        },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  async function load(): Promise<AdminJournalEditorComponent> {
    const fixture = TestBed.createComponent(AdminJournalEditorComponent);
    fixture.componentRef.setInput('turn', 5);
    fixture.detectChanges();
    http.expectOne('/api/admin/game/gazette/5').flush(GAZETTE_ADMIN_ISSUE);
    await fixture.whenStable();
    TestBed.tick();
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  it('pré-remplit le formulaire et l’aperçu depuis le brouillon', async () => {
    const component = await load();

    expect(component.frontPage().length).toBe(2);
    expect(component.briefs().length).toBe(1);
    expect(component.notes()?.eliminations.length).toBe(1);
    expect(component.previewIssue()?.frontPage[0].headline).toBe('LA CHUTE DE LURIO');
  });

  it('publie une une complète et recharge l’édition', async () => {
    const component = await load();
    component.frontPage.set([{ ...LEAD_ARTICLE, headline: 'TITRE PUBLIÉ', body: 'Corps', author: 'Auteur' }]);
    component.briefs.set([]);
    expect(component.canPublish()).toBe(true);

    const pending = component.publish();
    const post = http.expectOne('/api/admin/game/gazette/5/publish');
    expect(post.request.method).toBe('POST');
    expect(post.request.body.frontPage[0].headline).toBe('TITRE PUBLIÉ');
    post.flush(GAZETTE_ADMIN_ISSUE);
    await pending;
    TestBed.tick();
    http.expectOne('/api/admin/game/gazette/5').flush(GAZETTE_ADMIN_ISSUE);
  });
});
