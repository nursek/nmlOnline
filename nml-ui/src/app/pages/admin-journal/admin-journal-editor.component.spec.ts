import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AdminJournalEditorComponent } from './admin-journal-editor.component';
import { PlayerDirectoryService } from '../../services/player-directory.service';
import { GAZETTE_ADMIN_ISSUE, LEAD_ARTICLE } from '../../shared/gazette/gazette.fixture';

describe('AdminJournalEditorComponent', () => {
  let http: HttpTestingController;
  const snackBar = { open: jest.fn() };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminJournalEditorComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MatSnackBar, useValue: snackBar },
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

  it('ajoute, réordonne et retire des articles, normalise le champ photo', async () => {
    const component = await load();

    component.addMain();
    expect(component.frontPage()).toHaveLength(3);
    component.move('frontPage', 2, -1);
    expect(component.frontPage()[1].section).toBe('LA UNE');
    component.remove('frontPage', 0);
    expect(component.frontPage()).toHaveLength(2);

    component.addBrief();
    expect(component.briefs()).toHaveLength(2);
    component.updateField('frontPage', 0, 'headline', {
      target: { value: 'NOUVEAU TITRE' },
    } as unknown as Event);
    expect(component.frontPage()[0].headline).toBe('NOUVEAU TITRE');
    component.updateField('frontPage', 0, 'photoPlayerName', {
      target: { value: '' },
    } as unknown as Event);
    expect(component.frontPage()[0].photoPlayerName).toBeNull();
  });

  it('copie les faits du tour et imprime', async () => {
    const component = await load();
    const writeText = jest.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText },
      configurable: true,
    });
    const open = jest.spyOn(
      (component as unknown as { snackBar: { open: () => void } }).snackBar,
      'open',
    );
    const print = jest.spyOn(window, 'print').mockImplementation(() => undefined);

    await component.copyNotes();
    expect(writeText).toHaveBeenCalledWith(expect.stringContaining('eliminations'));
    expect(open).toHaveBeenCalledWith('Faits du tour copiés', 'OK', { duration: 2000 });

    component.print();
    expect(print).toHaveBeenCalled();
  });

  it('recharge l’édition sur demande', async () => {
    const component = await load();

    component.retry();
    TestBed.tick();
    http.expectOne('/api/admin/game/gazette/5').flush(GAZETTE_ADMIN_ISSUE);
  });

  it('enregistre le brouillon courant et recharge l’édition', async () => {
    const component = await load();
    const open = jest.spyOn(
      (component as unknown as { snackBar: { open: () => void } }).snackBar,
      'open',
    );
    const promise = component.save();
    const put = http.expectOne('/api/admin/game/gazette/5');
    expect(put.request.method).toBe('PUT');
    expect(put.request.body.frontPage).toHaveLength(2);
    put.flush(GAZETTE_ADMIN_ISSUE);
    await promise;
    TestBed.tick();
    http.expectOne('/api/admin/game/gazette/5').flush(GAZETTE_ADMIN_ISSUE);
    expect(open).toHaveBeenCalledWith('Brouillon enregistré', 'OK', { duration: 3000 });
  });

  it('supprime le brouillon après confirmation puis revient à la liste', async () => {
    const component = await load();
    const open = jest.spyOn(
      (component as unknown as { snackBar: { open: () => void } }).snackBar,
      'open',
    );
    const router = TestBed.inject(Router);
    const navigate = jest.spyOn(router, 'navigate').mockResolvedValue(true);
    const confirm = jest.spyOn(window, 'confirm').mockReturnValue(true);

    const promise = component.deleteDraft();
    const del = http.expectOne('/api/admin/game/gazette/5');
    expect(del.request.method).toBe('DELETE');
    del.flush(null);
    await promise;

    expect(confirm).toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith(['/admin/journal']);
    expect(open).toHaveBeenCalledWith('Brouillon supprimé', 'OK', { duration: 2500 });
  });

  it('ne supprime rien sans confirmation', async () => {
    const component = await load();
    jest.spyOn(window, 'confirm').mockReturnValue(false);

    await component.deleteDraft();

    http.expectNone('/api/admin/game/gazette/5');
  });

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
