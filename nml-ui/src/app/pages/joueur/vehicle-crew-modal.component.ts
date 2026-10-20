import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatRadioModule } from '@angular/material/radio';
import { Sector, Vehicle } from '../../models';
import { VehicleImageService } from '../../core/vehicle-image.service';
import { passengerCandidates, pilotCandidates } from './vehicle-crew.helpers';

export interface VehicleCrewDialogData {
  vehicle: Vehicle;
  sector: Sector;
  playerId: number;
}

export interface VehicleCrewDialogResult {
  pilotId: number | null;
  passengerIds: number[];
}

@Component({
  selector: 'app-vehicle-crew-modal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatDialogModule, MatButtonModule, MatCheckboxModule, MatIconModule, MatRadioModule],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>groups</mat-icon>
      Équipage — {{ data.vehicle.displayName }}
    </h2>

    <mat-dialog-content>
      <div class="vehicle-hero">
        @if (vehicleImage(); as img) {
          <img
            [src]="img.src"
            [srcset]="img.srcset"
            (error)="onImgError()"
            [alt]="data.vehicle.displayName"
            loading="lazy"
            decoding="async"
          />
        } @else {
          <mat-icon>image_not_supported</mat-icon>
        }
      </div>

      <p class="hint">
        Pilote : personnage ou unité classe P. Passagers : {{ passengerIds().size }}/{{
          data.vehicle.capacity
        }}.
      </p>

      <h3>Pilote</h3>
      <mat-radio-group
        class="pilots"
        [value]="pilotId()"
        (change)="selectPilot($event.value)"
      >
        <mat-radio-button [value]="null">Aucun pilote</mat-radio-button>
        @for (candidate of pilots(); track candidate.id) {
          <mat-radio-button [value]="candidate.id">{{ candidate.name }}</mat-radio-button>
        }
      </mat-radio-group>

      <h3>Passagers</h3>
      @if (passengers().length === 0) {
        <p class="empty">Aucune unité disponible dans ce secteur.</p>
      } @else {
        <div class="passengers">
          @for (candidate of passengers(); track candidate.id) {
            <mat-checkbox
              [checked]="isPassenger(candidate.id)"
              [disabled]="!isPassenger(candidate.id) && seatsLeft() === 0"
              (change)="togglePassenger(candidate.id, $event.checked)"
            >
              {{ candidate.name }}
            </mat-checkbox>
          }
        </div>
      }
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Annuler</button>
      <button mat-raised-button color="primary" (click)="confirm()">
        <mat-icon>check</mat-icon>
        Valider
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      h2 mat-icon {
        vertical-align: middle;
        margin-right: 8px;
      }
      .vehicle-hero {
        display: flex;
        align-items: center;
        justify-content: center;
        height: 160px;
        margin-bottom: 16px;
        overflow: hidden;
        border: 1.5px solid var(--line);
        border-radius: var(--r-sm);
        background: #fff;

        img {
          width: 100%;
          height: 100%;
          object-fit: contain;
        }

        mat-icon {
          width: 48px;
          height: 48px;
          font-size: 48px;
          color: #a9a396;
        }
      }
      .hint {
        color: var(--muted);
        font-size: 0.85rem;
      }
      h3 {
        margin: 16px 0 6px;
        font-size: 0.8rem;
        text-transform: uppercase;
        letter-spacing: 0.04em;
        color: var(--muted);
      }
      .pilots,
      .passengers {
        display: flex;
        flex-direction: column;
        gap: 4px;
      }
      .passengers {
        max-height: 240px;
        overflow-y: auto;
        padding: 8px;
        border: 1px solid var(--line);
        border-radius: var(--r-sm);
        background: var(--paper);
      }
      .empty {
        color: var(--muted);
        font-size: 0.85rem;
      }
    `,
  ],
})
export class VehicleCrewModalComponent {
  readonly dialogRef = inject(MatDialogRef<VehicleCrewModalComponent>);
  readonly data: VehicleCrewDialogData = inject(MAT_DIALOG_DATA);
  private readonly vehicleImages = inject(VehicleImageService);

  readonly vehicleImage = computed(() => this.vehicleImages.image(this.data.vehicle.vehicleType));

  readonly pilotId = signal<number | null>(this.data.vehicle.pilotId ?? null);
  readonly passengerIds = signal<ReadonlySet<number>>(
    new Set(this.data.vehicle.passengerIds ?? []),
  );
  readonly seatsLeft = computed(
    () => this.data.vehicle.capacity - this.passengerIds().size,
  );

  readonly pilots = computed(() =>
    pilotCandidates(this.data.sector, this.data.vehicle, this.data.playerId),
  );

  readonly passengers = computed(() =>
    passengerCandidates(
      this.data.sector,
      this.data.vehicle,
      this.data.playerId,
      this.pilotId(),
    ),
  );

  selectPilot(pilotId: number | null): void {
    this.pilotId.set(pilotId);
    if (pilotId != null && this.passengerIds().has(pilotId)) {
      this.passengerIds.update((set) => {
        const next = new Set(set);
        next.delete(pilotId);
        return next;
      });
    }
  }

  isPassenger(id: number): boolean {
    return this.passengerIds().has(id);
  }

  togglePassenger(id: number, checked: boolean): void {
    if (checked && this.seatsLeft() <= 0) return;
    this.passengerIds.update((set) => {
      const next = new Set(set);
      next[checked ? 'add' : 'delete'](id);
      return next;
    });
  }

  confirm(): void {
    this.dialogRef.close({
      pilotId: this.pilotId(),
      passengerIds: [...this.passengerIds()],
    } satisfies VehicleCrewDialogResult);
  }

  onImgError(): void {
    this.vehicleImages.onError(this.data.vehicle.vehicleType);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }
}
