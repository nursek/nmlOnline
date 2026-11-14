import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  numberAttribute,
  signal,
} from '@angular/core';
import { httpResource } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { Observable, firstValueFrom } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import {
  GazetteAdminIssue,
  GazetteArticle,
  GazetteContent,
  GazetteIssue,
} from '../../models';
import { environment } from '../../../environments/environment';
import { ApiService } from '../../services/api.service';
import { PlayerDirectoryService } from '../../services/player-directory.service';
import { httpErrorMessage } from '../../core/http-error.interceptor';
import { GazetteTemplateComponent } from '../../shared/gazette/gazette-template.component';

type ArticleList = 'frontPage' | 'briefs';

@Component({
  selector: 'app-admin-journal-editor',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    GazetteTemplateComponent,
  ],
  templateUrl: './admin-journal-editor.component.html',
  styleUrls: ['./admin-journal-editor.component.scss'],
})
export class AdminJournalEditorComponent {
  private readonly api = inject(ApiService);
  private readonly directory = inject(PlayerDirectoryService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);

  readonly turn = input<number | null>(null, { transform: numberAttribute });
  readonly issueRef = httpResource<GazetteAdminIssue>(() =>
    this.turn() === null
      ? undefined
      : `${environment.apiBaseUrl}/admin/game/gazette/${this.turn()}`,
  );
  readonly adminIssue = computed(() => (this.issueRef.hasValue() ? this.issueRef.value() : null));
  readonly notes = computed(() => this.adminIssue()?.editorNotes ?? null);
  readonly status = computed(() => this.adminIssue()?.status ?? null);
  readonly published = computed(() => this.status() === 'PUBLISHED');
  readonly players = this.directory.players;

  readonly frontPage = signal<GazetteArticle[]>([]);
  readonly briefs = signal<GazetteArticle[]>([]);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  private loadedTurn: number | null = null;

  readonly previewIssue = computed<GazetteIssue | null>(() => {
    const admin = this.adminIssue();
    if (!admin) {
      return null;
    }
    return { ...admin.issue, frontPage: this.frontPage(), briefs: this.briefs() };
  });

  readonly canPublish = computed(() =>
    this.frontPage().some(
      (article) => filled(article.headline) && filled(article.body) && filled(article.author),
    ),
  );

  constructor() {
    effect(() => {
      const admin = this.adminIssue();
      if (!admin || this.loadedTurn === admin.issue.turn) {
        return;
      }
      this.loadedTurn = admin.issue.turn;
      this.frontPage.set((admin.issue.frontPage ?? []).map((article) => ({ ...article })));
      this.briefs.set((admin.issue.briefs ?? []).map((article) => ({ ...article })));
    });
  }

  retry(): void {
    this.issueRef.reload();
  }

  addMain(): void {
    this.frontPage.update((items) => [...items, emptyArticle('LA UNE')]);
  }

  addBrief(): void {
    this.briefs.update((items) => [...items, emptyArticle('LES AUTRES FAITS')]);
  }

  remove(list: ArticleList, index: number): void {
    this[list].update((items) => items.filter((_, i) => i !== index));
  }

  move(list: ArticleList, index: number, delta: number): void {
    this[list].update((items) => {
      const target = index + delta;
      if (target < 0 || target >= items.length) {
        return items;
      }
      const copy = [...items];
      const [item] = copy.splice(index, 1);
      if (!item) {
        return items;
      }
      copy.splice(target, 0, item);
      return copy;
    });
  }

  updateField(
    list: ArticleList,
    index: number,
    field: keyof GazetteArticle,
    event: Event,
  ): void {
    const value = (event.target as HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement)
      .value;
    const normalized = field === 'photoPlayerName' && value === '' ? null : value;
    this[list].update((items) =>
      items.map((item, i) =>
        i === index ? ({ ...item, [field]: normalized } as GazetteArticle) : item,
      ),
    );
  }

  content(): GazetteContent {
    return { frontPage: this.frontPage(), briefs: this.briefs() };
  }

  async save(): Promise<void> {
    if (this.turn() === null) {
      return;
    }
    await this.mutate(
      () => this.api.adminSaveGazette(this.turn()!, this.content()),
      'Brouillon enregistré',
    );
  }

  async publish(): Promise<void> {
    if (this.turn() === null || !this.canPublish() || this.published()) {
      return;
    }
    await this.mutate(
      () => this.api.adminPublishGazette(this.turn()!, this.content()),
      'Édition publiée',
    );
  }

  async deleteDraft(): Promise<void> {
    if (this.turn() === null || !window.confirm('Supprimer ce brouillon ?')) {
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(this.api.adminDeleteGazetteDraft(this.turn()!));
      this.snackBar.open('Brouillon supprimé', 'OK', { duration: 2500 });
      await this.router.navigate(['/admin/journal']);
    } catch (error) {
      this.error.set(httpErrorMessage(error, 'Suppression impossible'));
    } finally {
      this.saving.set(false);
    }
  }

  async copyNotes(): Promise<void> {
    const notes = this.notes();
    if (!notes) {
      return;
    }
    try {
      await navigator.clipboard.writeText(JSON.stringify(notes, null, 2));
      this.snackBar.open('Faits du tour copiés', 'OK', { duration: 2000 });
    } catch {
      this.snackBar.open('Copie impossible', 'OK', { duration: 2500 });
    }
  }

  print(): void {
    window.print();
  }

  private async mutate(
    call: () => Observable<GazetteAdminIssue>,
    message: string,
  ): Promise<void> {
    this.saving.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(call());
      this.snackBar.open(message, 'OK', { duration: 3000 });
      this.issueRef.reload();
    } catch (error) {
      this.error.set(httpErrorMessage(error, 'Enregistrement impossible'));
    } finally {
      this.saving.set(false);
    }
  }
}

function filled(value: string | null | undefined): boolean {
  return !!value && value.trim().length > 0;
}

function emptyArticle(section: string): GazetteArticle {
  return {
    kind: 'ELIMINATION',
    section,
    headline: '',
    quote: null,
    body: '',
    author: '',
    photoPlayerName: null,
    photoCaption: null,
  };
}
