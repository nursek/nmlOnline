import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  linkedSignal,
  signal,
} from '@angular/core';
import { firstValueFrom, Observable } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import {
  MatDialog,
  MatDialogModule,
  MatDialogRef,
  MAT_DIALOG_DATA,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Unit, UnitClass } from '../../../../models';
import { AdminService } from '../../../../services/admin.service';
import { ApiService } from '../../../../services/api.service';
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from '../../../../shared/confirm-dialog/confirm-dialog.component';

export interface UnitEditDialogData {
  playerId: number;
  unitId: number;
}

@Component({
  selector: 'app-unit-edit-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTooltipModule,
  ],
  templateUrl: './unit-edit-dialog.component.html',
  styleUrls: ['./unit-edit-dialog.component.scss'],
})
export class UnitEditDialogComponent {
  private readonly admin = inject(AdminService);
  private readonly api = inject(ApiService);
  private readonly dialog = inject(MatDialog);
  private readonly dialogRef = inject(MatDialogRef<UnitEditDialogComponent>);
  readonly data = inject<UnitEditDialogData>(MAT_DIALOG_DATA);

  readonly edit = this.admin.edit;
  readonly catalog = this.admin.catalog;
  readonly busy = signal(false);

  readonly unit = computed(
    () => this.edit()?.units.find((candidate) => candidate.id === this.data.unitId) ?? null,
  );
  readonly player = computed(() => this.edit()?.player ?? null);
  readonly ownedSectors = computed(() => this.player()?.sectors ?? []);
  readonly equipmentCatalog = computed(() => this.catalog()?.equipments ?? []);
  readonly unitClassCatalog = computed(() => this.catalog()?.unitClasses ?? []);

  readonly experience = signal(0);
  readonly classes = signal<string[]>([]);
  readonly injured = signal(false);
  readonly sectorChoice = signal<number | null>(null);
  private formInitialized = false;

  readonly embedded = computed(() =>
    (this.edit()?.vehicles ?? []).some(
      (vehicle) =>
        vehicle.pilotId === this.data.unitId || vehicle.passengerIds.includes(this.data.unitId),
    ),
  );

  readonly canAddSecondClass = computed(() => {
    const unit = this.unit();
    if (!unit) return false;
    return unit.type.name !== 'LARBIN' && unit.type.name !== 'VOYOU' && this.experience() >= 5;
  });

  readonly availableEquipments = computed(() => {
    const unit = this.unit();
    if (!unit) return [];
    const available = new Map(
      (this.player()?.equipments ?? []).map((stack) => [stack.equipment.name, stack.available]),
    );
    const classNames = new Set(this.classes());
    return this.equipmentCatalog().filter(
      (equipment) =>
        (available.get(equipment.name) ?? 0) > 0 &&
        equipment.compatibleClass.some((unitClass) => classNames.has(unitClass.name)),
    );
  });

  readonly equipmentToAdd = linkedSignal(() => this.availableEquipments()[0]?.name ?? '');

  readonly incompatibleEquipment = computed(() => {
    const unit = this.unit();
    if (!unit) return [];
    const classNames = new Set(this.classes());
    return unit.equipments
      .filter(
        (equipment) =>
          !equipment.compatibleClass.some((unitClass) => classNames.has(unitClass.name)),
      )
      .map((equipment) => equipment.name);
  });

  constructor() {
    effect(() => {
      const unit = this.unit();
      if (!unit || this.formInitialized) return;
      this.experience.set(unit.experience);
      this.classes.set(unit.classes.map((unitClass) => unitClass.name));
      this.injured.set(unit.isInjured);
      this.sectorChoice.set(unit.sectorNumber ?? null);
      this.formInitialized = true;
    });
  }

  unitLabel(unit: Unit | null): string {
    if (!unit) return 'Unité';
    return unit.number > 0 ? `${unit.type.name} n°${unit.number}` : unit.type.name;
  }

  sectorLabel(sectorNumber: number | null): string {
    if (sectorNumber === null) return 'Réserve';
    const sector = this.ownedSectors().find((candidate) => candidate.number === sectorNumber);
    return sector ? `${sector.name} (#${sectorNumber})` : `#${sectorNumber}`;
  }

  isClassDisabled(unitClass: UnitClass): boolean {
    if (this.classes().includes(unitClass.name)) return false;
    if (this.classes().length >= 2) return true;
    return this.classes().length === 1 && !this.canAddSecondClass();
  }

  async save(): Promise<void> {
    const unit = this.unit();
    if (!unit || this.classes().length === 0) return;
    const experience = this.experience();
    if (!Number.isFinite(experience) || experience < 0) return;

    const incompatible = this.incompatibleEquipment();
    if (incompatible.length > 0) {
      const confirmed = await this.confirm({
        title: 'Changer les classes',
        message: `Ces équipements deviendront incompatibles et seront rendus au stock : ${incompatible.join(', ')}. Continuer ?`,
        confirmLabel: 'Changer',
      });
      if (!confirmed) return;
    }

    await this.run(
      this.api.adminUpdateUnit(this.data.playerId, this.data.unitId, {
        experience,
        classes: this.classes(),
        injured: this.injured(),
      }),
      'Unité mise à jour',
    );
  }

  async move(): Promise<void> {
    if (this.embedded()) return;
    await this.run(
      this.api.adminMoveUnit(this.data.playerId, this.data.unitId, this.sectorChoice()),
      'Unité déplacée',
    );
  }

  async equip(): Promise<void> {
    const name = this.equipmentToAdd();
    if (!name) return;
    const done = await this.run(
      this.api.adminEquipUnit(this.data.playerId, this.data.unitId, name),
      `${name} équipé`,
    );
    if (done) {
      this.equipmentToAdd.set('');
    }
  }

  async unequip(name: string): Promise<void> {
    await this.run(
      this.api.adminUnequipUnit(this.data.playerId, this.data.unitId, name),
      `${name} retiré`,
    );
  }

  close(): void {
    this.dialogRef.close();
  }

  private async run(call: Observable<unknown>, successMessage: string): Promise<boolean> {
    if (this.busy()) return false;
    this.busy.set(true);
    try {
      await this.admin.mutate(call, successMessage);
      return true;
    } catch {
      return false;
    } finally {
      this.busy.set(false);
    }
  }

  private async confirm(data: ConfirmDialogData): Promise<boolean> {
    return (
      (await firstValueFrom(this.dialog.open(ConfirmDialogComponent, { data }).afterClosed())) ===
      true
    );
  }
}
