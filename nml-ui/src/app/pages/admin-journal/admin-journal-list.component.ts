import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { GazetteIssueSummary } from '../../models';
import { environment } from '../../../environments/environment';
import { ApiService } from '../../services/api.service';
import { httpErrorMessage } from '../../core/http-error.interceptor';

@Component({
  selector: 'app-admin-journal-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  templateUrl: './admin-journal-list.component.html',
  styleUrls: ['./admin-journal-list.component.scss'],
})
export class AdminJournalListComponent {
  private readonly api = inject(ApiService);
  private readonly snackBar = inject(MatSnackBar);

  readonly issuesRef = httpResource<GazetteIssueSummary[]>(
    () => `${environment.apiBaseUrl}/admin/game/gazette`,
  );
  readonly issues = computed(() => (this.issuesRef.hasValue() ? this.issuesRef.value() : []));
  readonly deleting = signal<number | null>(null);
  readonly error = signal<string | null>(null);

  retry(): void {
    this.issuesRef.reload();
  }

  async deleteDraft(turn: number): Promise<void> {
    if (!window.confirm(`Supprimer le brouillon du tour ${turn} ?`)) {
      return;
    }
    this.deleting.set(turn);
    this.error.set(null);
    try {
      await firstValueFrom(this.api.adminDeleteGazetteDraft(turn));
      this.snackBar.open('Brouillon supprimé', 'OK', { duration: 2500 });
      this.issuesRef.reload();
    } catch (error) {
      this.error.set(httpErrorMessage(error, 'Suppression impossible'));
    } finally {
      this.deleting.set(null);
    }
  }
}
