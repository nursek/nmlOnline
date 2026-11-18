import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
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
      <p class="carte-label" id="choix-carte">Carte</p>
      <div class="cartes" role="group" aria-labelledby="choix-carte">
        @for (preset of presets(); track preset.key) {
          <label class="carte" [class.carte-selectionnee]="presetKey() === preset.key">
            <input
              type="radio"
              name="preset"
              [value]="preset.key"
              [checked]="presetKey() === preset.key"
              (change)="presetKey.set(preset.key)"
            />
            @if (preset.mapImageUrl; as image) {
              <img
                class="carte-image"
                [src]="image"
                [alt]="'Carte ' + preset.name"
                loading="lazy"
              />
            }
            <span class="carte-nom">{{ preset.name }}</span>
            <span class="carte-details"
              >{{ preset.sectorCount }} secteurs · {{ preset.spotCount }} spots</span
            >
          </label>
        }
      </div>
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
      .carte-label {
        margin: 0 0 8px;
        font-size: 0.8rem;
        font-weight: 600;
        color: var(--muted);
        text-transform: uppercase;
        letter-spacing: 0.06em;
      }
      .cartes {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
        gap: 12px;
        margin-bottom: 14px;
      }
      .carte {
        position: relative;
        display: flex;
        flex-direction: column;
        gap: 6px;
        padding: 10px;
        border: 1.5px solid var(--line-2);
        border-radius: var(--r-sm);
        background: var(--paper);
        cursor: pointer;
        transition:
          border-color 0.15s ease,
          box-shadow 0.15s ease;
      }
      .carte:hover {
        border-color: var(--accent);
      }
      .carte:has(input:focus-visible) {
        outline: 2px solid var(--accent);
        outline-offset: 2px;
      }
      .carte-selectionnee {
        border-color: var(--accent);
        box-shadow: 0 0 0 2px color-mix(in srgb, var(--accent) 30%, transparent);
      }
      .carte input {
        position: absolute;
        opacity: 0;
        pointer-events: none;
      }
      .carte-image {
        width: 100%;
        aspect-ratio: 3 / 2;
        object-fit: cover;
        border: 1px solid var(--line);
        border-radius: var(--r-xs);
        background: var(--paper-2);
      }
      .carte-nom {
        font-weight: 600;
      }
      .carte-details {
        font-family: var(--font-mono);
        font-size: 0.75rem;
        color: var(--muted);
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
