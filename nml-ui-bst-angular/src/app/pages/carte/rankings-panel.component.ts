import {
  afterRenderEffect,
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RankingEntry, Rankings } from '../../models';
import { environment } from '../../../environments/environment';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-rankings-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe, MatCardModule, MatIconModule, MatButtonModule, MatSnackBarModule],
  templateUrl: './rankings-panel.component.html',
  styleUrls: ['./rankings-panel.component.scss'],
})
export class RankingsPanelComponent {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);

  readonly playerColors = input<ReadonlyMap<number, string>>(new Map());
  readonly isAdmin = this.auth.isAdmin;

  readonly rankingsRef = httpResource<Rankings>(() => `${environment.apiBaseUrl}/rankings`);
  readonly rankings = computed(() => this.rankingsRef.value() ?? null);
  readonly available = computed(() => (this.rankings()?.turn ?? 0) >= 2);

  readonly editingPlayerId = signal<number | null>(null);
  readonly draft = signal('');
  readonly saving = signal(false);

  private readonly commentEditor = viewChild<ElementRef<HTMLDivElement>>('commentEditor');
  private syncedEditorFor: number | null = null;

  constructor() {
    // contenteditable plutôt qu'un input : les gestionnaires de mots de passe n'injectent
    // leur menu que sur les champs de formulaire.
    afterRenderEffect(() => {
      const editor = this.commentEditor()?.nativeElement;
      const playerId = this.editingPlayerId();
      if (!editor || playerId === null) {
        this.syncedEditorFor = null;
        return;
      }
      if (this.syncedEditorFor === playerId) {
        return;
      }
      this.syncedEditorFor = playerId;
      editor.textContent = this.draft();
      editor.focus();
      const range = document.createRange();
      range.selectNodeContents(editor);
      range.collapse(false);
      window.getSelection()?.removeAllRanges();
      window.getSelection()?.addRange(range);
    });
  }

  getPlayerColor(playerId: number): string {
    return this.playerColors().get(playerId) ?? 'var(--muted)';
  }

  startEdit(entry: RankingEntry): void {
    this.editingPlayerId.set(entry.playerId);
    this.draft.set(entry.comment ?? '');
  }

  cancelEdit(): void {
    this.editingPlayerId.set(null);
  }

  onDraftInput(event: Event): void {
    this.draft.set((event.target as HTMLElement).textContent ?? '');
  }

  async save(entry: RankingEntry): Promise<void> {
    if (this.saving()) return;

    const comment = this.draft().trim();
    if (comment.length > 500) {
      this.snackBar.open('Indice limité à 500 caractères', 'Fermer', { duration: 4000 });
      return;
    }

    this.saving.set(true);
    try {
      await firstValueFrom(this.api.adminUpdateRankingComment(entry.playerId, comment));
      this.editingPlayerId.set(null);
      this.rankingsRef.reload();
      this.snackBar.open('Indice enregistré', 'OK', { duration: 2500 });
    } catch {
      this.snackBar.open("Erreur lors de l'enregistrement de l'indice", 'Fermer', {
        duration: 4000,
      });
    } finally {
      this.saving.set(false);
    }
  }
}
