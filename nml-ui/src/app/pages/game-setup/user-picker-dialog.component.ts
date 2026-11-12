import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { firstValueFrom } from 'rxjs';
import { ApiService } from '../../services/api.service';
import { AdminUser } from '../../models';
import { httpErrorMessage } from '../../core/http-error.interceptor';

export interface UserPickerData {
  title: string;
  actionLabel: string;
}

@Component({
  selector: 'app-user-picker-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <div class="search-row">
        <mat-form-field appearance="outline" class="search-field">
          <mat-label>Nom d'utilisateur</mat-label>
          <input
            matInput
            [value]="query()"
            (input)="query.set($any($event.target).value)"
            (keydown.enter)="search()"
          />
        </mat-form-field>
        <button mat-stroked-button (click)="search()" [disabled]="loading()">
          <mat-icon>search</mat-icon>
          Chercher
        </button>
      </div>

      @if (error(); as message) {
        <p class="error">{{ message }}</p>
      }
      @if (loading()) {
        <div class="center"><mat-spinner diameter="28"></mat-spinner></div>
      } @else if (results().length === 0) {
        <p class="empty">Aucun compte trouvé.</p>
      } @else {
        <div class="results">
          @for (user of results(); track user.id) {
            <div class="result">
              <div>
                <strong>{{ user.username }}</strong>
                <span class="role">{{ user.role }}</span>
              </div>
              <button mat-flat-button color="primary" (click)="pick(user)">
                {{ data.actionLabel }}
              </button>
            </div>
          }
        </div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Annuler</button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .search-row {
        display: flex;
        gap: 12px;
        align-items: center;
      }
      .search-field {
        flex: 1;
      }
      .results {
        display: flex;
        flex-direction: column;
        gap: 8px;
        max-height: 320px;
        overflow-y: auto;
      }
      .result {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 12px;
        padding: 8px 10px;
        border: 1px solid var(--line);
        border-radius: var(--r-sm);
      }
      .role {
        margin-left: 8px;
        font-size: 0.7rem;
        color: var(--muted);
        text-transform: uppercase;
      }
      .empty,
      .error {
        color: var(--muted);
        font-size: 0.85rem;
      }
      .error {
        color: var(--danger);
      }
      .center {
        display: flex;
        justify-content: center;
        padding: 20px;
      }
    `,
  ],
})
export class UserPickerDialogComponent {
  readonly data = inject<UserPickerData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<UserPickerDialogComponent>);
  private readonly api = inject(ApiService);

  readonly query = signal('');
  readonly results = signal<AdminUser[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  constructor() {
    void this.search();
  }

  async search(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const page = await firstValueFrom(this.api.adminSearchUsers(this.query().trim()));
      this.results.set(page.content);
    } catch (error) {
      this.error.set(httpErrorMessage(error, 'Recherche impossible'));
    } finally {
      this.loading.set(false);
    }
  }

  pick(user: AdminUser): void {
    this.dialogRef.close(user);
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
