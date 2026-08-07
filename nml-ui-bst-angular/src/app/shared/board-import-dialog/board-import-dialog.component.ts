import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface BoardImportResult {
  file: File;
  mapImage?: File;
  svgOverlay?: File;
}

/**
 * ponytail: dialog dédié pour 3 fichiers plutôt que 3 window.prompt chaînés
 * — l'admin a besoin de voir les noms et ne peut pas coller un File via prompt.
 */
@Component({
  selector: 'app-board-import-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <h2 mat-dialog-title>Importer le Board</h2>
    <mat-dialog-content>
      <p class="hint">
        board.json (secteurs) requis. Image de carte + SVG overlay optionnels : si fournis, ils sont
        uploadés et leurs URLs passées à l'import.
      </p>

      <div class="field">
        <button mat-stroked-button (click)="fileInput.click()">
          <mat-icon>upload_file</mat-icon>
          {{ file() ? file()!.name : 'Choisir board.json' }}
        </button>
        <input #fileInput type="file" accept=".json" hidden (change)="onFile($event, 'file')" />
      </div>

      <div class="field">
        <button mat-stroked-button (click)="mapInput.click()">
          <mat-icon>image</mat-icon>
          {{ mapImage() ? mapImage()!.name : 'Image de carte (optionnel)' }}
        </button>
        <input
          #mapInput
          type="file"
          accept="image/*"
          hidden
          (change)="onFile($event, 'mapImage')"
        />
      </div>

      <div class="field">
        <button mat-stroked-button (click)="svgInput.click()">
          <mat-icon>layers</mat-icon>
          {{ svgOverlay() ? svgOverlay()!.name : 'Overlay SVG (optionnel)' }}
        </button>
        <input
          #svgInput
          type="file"
          accept=".svg,image/svg+xml"
          hidden
          (change)="onFile($event, 'svgOverlay')"
        />
      </div>

      @if ((mapImage() && !svgOverlay()) || (svgOverlay() && !mapImage())) {
        <p class="warn">
          Image et SVG doivent être fournis ensemble (sinon les assets sont ignorés).
        </p>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Annuler</button>
      <button mat-flat-button color="primary" (click)="confirm()" [disabled]="!canConfirm()">
        Importer
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .hint {
        color: rgba(0, 0, 0, 0.6);
        font-size: 0.85rem;
        margin-bottom: 16px;
      }
      .field {
        margin-bottom: 12px;
      }
      .warn {
        color: #dc2626;
        font-size: 0.8rem;
      }
    `,
  ],
})
export class BoardImportDialogComponent {
  readonly dialogRef = inject(MatDialogRef<BoardImportDialogComponent>);

  readonly file = signal<File | null>(null);
  readonly mapImage = signal<File | null>(null);
  readonly svgOverlay = signal<File | null>(null);

  onFile(event: Event, key: 'file' | 'mapImage' | 'svgOverlay'): void {
    const input = event.target as HTMLInputElement;
    const f = input.files?.[0] ?? null;
    this[key].set(f);
  }

  canConfirm(): boolean {
    if (!this.file()) return false;
    // Assets doivent venir ensemble ou pas du tout.
    const hasMap = !!this.mapImage();
    const hasSvg = !!this.svgOverlay();
    return hasMap === hasSvg;
  }

  confirm(): void {
    if (!this.canConfirm()) return;
    this.dialogRef.close({
      file: this.file()!,
      mapImage: this.mapImage() ?? undefined,
      svgOverlay: this.svgOverlay() ?? undefined,
    } as BoardImportResult);
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
