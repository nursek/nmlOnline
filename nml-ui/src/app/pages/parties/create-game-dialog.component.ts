import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { firstValueFrom } from 'rxjs';
import { ApiService } from '../../services/api.service';
import { Board, BoardPresetSummary } from '../../models';
import { httpErrorMessage } from '../../core/http-error.interceptor';
import {
  BoardImportDialogComponent,
  BoardImportResult,
} from '../../shared/board-import-dialog/board-import-dialog.component';

@Component({
  selector: 'app-create-game-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatIconModule,
  ],
  template: `
    <h2 mat-dialog-title>Nouvelle partie</h2>
    <mat-dialog-content>
      <p class="hint">
        La carte devient une nouvelle partie indépendante. Choisissez une carte préfaite ou importez
        un board.json personnalisé.
      </p>
      <mat-form-field appearance="outline" class="full">
        <mat-label>Nom de la partie</mat-label>
        <input matInput [value]="name()" (input)="name.set($any($event.target).value)" />
      </mat-form-field>
      <mat-form-field appearance="outline" class="full">
        <mat-label>Carte</mat-label>
        <mat-select [value]="presetKey()" (selectionChange)="presetKey.set($event.value)">
          @for (preset of presets(); track preset.key) {
            <mat-option [value]="preset.key">
              {{ preset.name }} — {{ preset.sectorCount }} secteurs, {{ preset.spotCount }} spots
            </mat-option>
          }
        </mat-select>
      </mat-form-field>
      <button mat-stroked-button type="button" (click)="importBoard()" [disabled]="creating()">
        <mat-icon>upload_file</mat-icon>
        Importer un board.json
      </button>
      @if (error(); as message) {
        <p class="error">{{ message }}</p>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Annuler</button>
      <button mat-flat-button color="primary" (click)="create()" [disabled]="!canCreate()">
        @if (creating()) {
          <mat-spinner diameter="18"></mat-spinner>
        } @else {
          Créer
        }
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .hint {
        color: var(--muted);
        font-size: 0.85rem;
        margin-bottom: 14px;
      }
      .full {
        width: 100%;
        margin-bottom: 12px;
      }
      .error {
        color: var(--danger);
        font-size: 0.85rem;
      }
    `,
  ],
})
export class CreateGameDialogComponent {
  private readonly api = inject(ApiService);
  private readonly dialogRef = inject(MatDialogRef<CreateGameDialogComponent>);
  private readonly dialog = inject(MatDialog);

  readonly presets = signal<BoardPresetSummary[]>([]);
  readonly name = signal('');
  readonly presetKey = signal<string>('');
  readonly creating = signal(false);
  readonly error = signal<string | null>(null);

  readonly canCreate = computed(
    () => this.name().trim().length > 0 && this.presetKey().length > 0 && !this.creating(),
  );

  constructor() {
    void this.loadPresets();
  }

  private async loadPresets(): Promise<void> {
    try {
      this.presets.set(await firstValueFrom(this.api.adminGetBoardPresets()));
      if (this.presets().length === 1) {
        this.presetKey.set(this.presets()[0].key);
      }
    } catch (error) {
      this.error.set(httpErrorMessage(error, 'Impossible de charger les cartes préfaites'));
    }
  }

  async create(): Promise<void> {
    if (!this.canCreate()) return;
    this.creating.set(true);
    this.error.set(null);
    try {
      const preset = await firstValueFrom(this.api.adminGetBoardPreset(this.presetKey()));
      const board: Board = {
        ...preset.board,
        id: 0,
        name: this.name().trim(),
        presetKey: preset.key,
      };
      this.dialogRef.close(await firstValueFrom(this.api.createBoard(board)));
    } catch (error) {
      this.error.set(httpErrorMessage(error, 'Impossible de créer la partie'));
    } finally {
      this.creating.set(false);
    }
  }

  async importBoard(): Promise<void> {
    const result = (await firstValueFrom(
      this.dialog.open(BoardImportDialogComponent).afterClosed(),
    )) as BoardImportResult | undefined;
    if (!result) return;
    this.creating.set(true);
    this.error.set(null);
    try {
      let mapImageUrl: string | undefined;
      let svgOverlayUrl: string | undefined;
      if (result.mapImage && result.svgOverlay) {
        const assets = await firstValueFrom(
          this.api.adminUploadBoardAssets(result.mapImage, result.svgOverlay),
        );
        mapImageUrl = assets.mapImageUrl;
        svgOverlayUrl = assets.svgOverlayUrl;
      }
      const board = await firstValueFrom(
        this.api.adminImportBoard(
          result.file,
          mapImageUrl,
          svgOverlayUrl,
          this.name().trim() || undefined,
        ),
      );
      this.dialogRef.close(board);
    } catch (error) {
      this.error.set(httpErrorMessage(error, "Erreur lors de l'import du board"));
    } finally {
      this.creating.set(false);
    }
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
