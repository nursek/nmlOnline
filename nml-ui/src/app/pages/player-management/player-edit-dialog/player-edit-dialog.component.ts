import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  linkedSignal,
  signal,
  WritableSignal,
} from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { firstValueFrom, Observable } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import {
  MatDialog,
  MatDialogModule,
  MatDialogRef,
  MAT_DIALOG_DATA,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  AdminAuditEntry,
  BUILDING_TYPES,
  Building,
  BuildingType,
  EquipmentStack,
  PlayerResource,
  Unit,
  UnitClass,
  Vehicle,
} from '../../../models';
import { AdminService } from '../../../services/admin.service';
import { ApiService } from '../../../services/api.service';
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from '../../../shared/confirm-dialog/confirm-dialog.component';
import { UnitEditDialogComponent } from './unit-edit-dialog/unit-edit-dialog.component';

export interface PlayerEditDialogData {
  playerId: number;
  playerName: string;
}

@Component({
  selector: 'app-player-edit-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    DecimalPipe,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatTabsModule,
    MatTooltipModule,
  ],
  templateUrl: './player-edit-dialog.component.html',
  styleUrls: ['./player-edit-dialog.component.scss'],
})
export class PlayerEditDialogComponent {
  private readonly admin = inject(AdminService);
  private readonly api = inject(ApiService);
  private readonly dialog = inject(MatDialog);
  private readonly dialogRef = inject(MatDialogRef<PlayerEditDialogComponent>);
  readonly data = inject<PlayerEditDialogData>(MAT_DIALOG_DATA);

  readonly edit = this.admin.edit;
  readonly catalog = this.admin.catalog;
  readonly players = this.admin.players;
  readonly loading = this.admin.editLoading;
  readonly busy = signal(false);

  readonly currentEdit = computed(() => {
    const edit = this.edit();
    return edit && edit.player.id === this.data.playerId ? edit : null;
  });
  readonly player = computed(() => this.currentEdit()?.player ?? null);
  readonly ownedSectors = computed(() => this.player()?.sectors ?? []);
  readonly equipmentCatalog = computed(() => this.catalog()?.equipments ?? []);
  readonly resourceCatalog = computed(() => this.catalog()?.resources ?? []);
  readonly unitTypes = computed(() => this.catalog()?.unitTypes ?? []);
  readonly unitClassCatalog = computed(() => this.catalog()?.unitClasses ?? []);
  readonly vehicleTypes = computed(() => this.catalog()?.vehicleTypes ?? []);

  readonly playerName = signal('');
  readonly character = computed(() => this.player()?.character ?? null);
  readonly characterName = signal('');
  readonly characterAttack = signal(0);
  readonly characterDefense = signal(0);
  readonly characterPdf = signal(0);
  readonly characterPdc = signal(0);
  readonly characterArmor = signal(0);
  readonly characterEvasion = signal(0);
  private formInitialized = false;

  readonly auditEntries = signal<AdminAuditEntry[]>([]);
  readonly auditLoading = signal(false);
  readonly auditError = signal(false);
  readonly auditTotal = signal(0);
  readonly auditPage = signal(0);
  readonly auditActionLabels: Record<string, string> = {
    PLAYER_NAME: 'Nom du joueur',
    CHARACTER_CREATE: 'Création du personnage',
    CHARACTER_UPDATE: 'Mise à jour du personnage',
    CHARACTER_DELETE: 'Suppression du personnage',
    SET_MONEY: 'Argent',
    ADD_RESOURCE: 'Ajout de ressource',
    REMOVE_RESOURCE: 'Retrait de ressource',
    ADD_EQUIPMENT: 'Ajout d’équipement',
    REMOVE_EQUIPMENT: 'Retrait d’équipement',
    ADD_UNITS: 'Ajout d’unités',
    DELETE_UNIT: 'Suppression d’unité',
    UPDATE_UNIT: 'Mise à jour d’unité',
    MOVE_UNIT: 'Déplacement d’unité',
    EQUIP_UNIT: 'Équipement d’unité',
    UNEQUIP_UNIT: 'Déséquipement d’unité',
    ADD_VEHICLES: 'Ajout de véhicules',
    DELETE_VEHICLE: 'Suppression de véhicule',
    ADD_BUILDING: 'Ajout de bâtiment',
    DELETE_BUILDING: 'Suppression de bâtiment',
    REASSIGN_SECTOR: 'Réassignation de secteur',
    IMPORT_PLAYER: 'Import de joueur',
    DELETE_PLAYER: 'Suppression de joueur',
    IMPORT_BOARD: 'Import de board',
    RANKING_COMMENT: 'Commentaire de classement',
  };

  private readonly sectorChoice = signal<number | null | undefined>(undefined);
  readonly sectorToReassign = computed(() => {
    const choice = this.sectorChoice();
    return choice === undefined ? (this.ownedSectors()[0]?.number ?? null) : choice;
  });
  readonly sectorTarget = linkedSignal<number | null>(() => null);
  readonly sectorTargets = computed(() =>
    this.players().filter(
      (candidate) =>
        candidate.id !== null && candidate.id !== this.data.playerId && !candidate.eliminated,
    ),
  );
  readonly selectedSector = computed(
    () => this.ownedSectors().find((sector) => sector.number === this.sectorToReassign()) ?? null,
  );

  readonly money = signal<number | null>(null);
  readonly moneyValue = computed(() => this.money() ?? this.player()?.stats.money ?? 0);

  readonly resourceName = linkedSignal(() => this.resourceCatalog()[0]?.name ?? '');
  readonly resourceQuantity = signal(1);
  readonly resourceRemoveQuantity = signal<Record<string, number>>({});

  readonly equipmentName = linkedSignal(() => this.equipmentCatalog()[0]?.name ?? '');
  readonly equipmentQuantity = signal(1);
  readonly equipmentRemoveQuantity = signal<Record<string, number>>({});

  readonly unitType = linkedSignal(() => this.unitTypes()[0]?.name ?? '');
  readonly unitClasses = linkedSignal(() => {
    const first = this.unitClassCatalog()[0]?.name;
    return first ? [first] : [];
  });
  readonly selectedAddUnitType = computed(
    () => this.unitTypes().find((type) => type.name === this.unitType()) ?? null,
  );
  readonly canAddSecondClassToNewUnit = computed(() => {
    const type = this.selectedAddUnitType();
    return !!type && type.name !== 'LARBIN' && type.name !== 'VOYOU' && type.minExp >= 5;
  });
  readonly unitQuantity = signal(1);
  private readonly unitSectorChoice = signal<number | null | undefined>(undefined);
  readonly unitSector = computed(() => {
    const choice = this.unitSectorChoice();
    return choice === undefined ? (this.ownedSectors()[0]?.number ?? null) : choice;
  });

  readonly vehicleType = linkedSignal(() => this.vehicleTypes()[0]?.name ?? '');
  readonly vehicleQuantity = signal(1);
  private readonly vehicleSectorChoice = signal<number | null | undefined>(undefined);
  readonly vehicleSector = computed(() => {
    const choice = this.vehicleSectorChoice();
    return choice === undefined ? (this.ownedSectors()[0]?.number ?? null) : choice;
  });

  readonly buildingTypes = BUILDING_TYPES;
  readonly buildingType = signal<BuildingType>('WEAPON_CACHE');
  private readonly buildingSectorChoice = signal<number | null | undefined>(undefined);
  readonly buildingSector = computed(() => {
    const choice = this.buildingSectorChoice();
    return choice === undefined ? (this.ownedSectors()[0]?.number ?? null) : choice;
  });

  readonly buildingLabels: Record<BuildingType, string> = {
    HEADQUARTERS: 'Quartier Général',
    WEAPON_CACHE: "Cache d'armes",
    BANK: 'Banque',
  };

  constructor() {
    this.admin.openEdit(this.data.playerId);
    effect(() => {
      const player = this.player();
      if (!player || this.formInitialized) return;
      this.playerName.set(player.name);
      this.seedCharacterForm();
      this.formInitialized = true;
    });
    void this.loadAudit();
  }

  close(): void {
    this.dialogRef.close();
  }

  unitLabel(unit: Unit): string {
    const type = unit.type?.name ?? '?';
    return unit.number > 0 ? `${type} n°${unit.number}` : type;
  }

  classesLabel(classes: ReadonlyArray<{ name: string }> | null | undefined): string {
    return (classes ?? []).map((unitClass) => unitClass.name).join(' / ');
  }

  sectorLabel(sectorNumber: number | null | undefined): string {
    return sectorNumber === null || sectorNumber === undefined
      ? 'Réserve'
      : `Secteur ${sectorNumber}`;
  }

  carriers(equipmentName: string): Unit[] {
    return (this.currentEdit()?.units ?? []).filter((unit) =>
      (unit.equipments ?? []).some((equipment) => equipment.name === equipmentName),
    );
  }

  equippedCount(equipmentName: string): number {
    return (this.currentEdit()?.units ?? []).reduce(
      (total, unit) =>
        total +
        (unit.equipments ?? []).filter((equipment) => equipment.name === equipmentName).length,
      0,
    );
  }

  carrierNames(equipmentName: string): string {
    return this.carriers(equipmentName)
      .map((unit) => `${this.unitLabel(unit)} (${this.sectorLabel(unit.sectorNumber)})`)
      .join(', ');
  }

  equipmentNames(unit: Unit): string {
    return (unit.equipments ?? []).map((equipment) => equipment.name).join(', ');
  }

  setQuantity(target: WritableSignal<number>, value: number): void {
    target.set(Math.max(0, Number.isFinite(value) ? Math.trunc(value) : 0));
  }

  setUnitClasses(values: string[]): void {
    this.unitClasses.set(values.slice(0, 2));
  }

  setUnitType(name: string): void {
    this.unitType.set(name);
    if (!this.canAddSecondClassToNewUnit() && this.unitClasses().length > 1) {
      this.unitClasses.set(this.unitClasses().slice(0, 1));
    }
  }

  isAddClassDisabled(unitClass: UnitClass): boolean {
    if (this.unitClasses().includes(unitClass.name)) return false;
    if (this.unitClasses().length >= 2) return true;
    return this.unitClasses().length === 1 && !this.canAddSecondClassToNewUnit();
  }

  setUnitSector(value: number | null): void {
    this.unitSectorChoice.set(value);
  }

  setVehicleSector(value: number | null): void {
    this.vehicleSectorChoice.set(value);
  }

  setBuildingSector(value: number | null): void {
    this.buildingSectorChoice.set(value);
  }

  equipmentRemoveQty(name: string, max: number): number {
    return this.clampQuantity(this.equipmentRemoveQuantity()[name] ?? 1, max);
  }

  setEquipmentRemoveQty(name: string, value: number): void {
    this.equipmentRemoveQuantity.update((map) => ({
      ...map,
      [name]: Math.max(1, Math.trunc(value) || 1),
    }));
  }

  resourceRemoveQty(name: string, max: number): number {
    return this.clampQuantity(this.resourceRemoveQuantity()[name] ?? 1, max);
  }

  setResourceRemoveQty(name: string, value: number): void {
    this.resourceRemoveQuantity.update((map) => ({
      ...map,
      [name]: Math.max(1, Math.trunc(value) || 1),
    }));
  }

  async applyMoney(): Promise<void> {
    const amount = this.moneyValue();
    if (!Number.isFinite(amount) || amount < 0) {
      return;
    }
    const done = await this.run(
      this.api.adminSetMoney(this.data.playerId, amount),
      'Argent mis à jour',
    );
    if (done) {
      this.money.set(null);
    }
  }

  async addResource(): Promise<void> {
    const name = this.resourceName();
    const quantity = this.resourceQuantity();
    if (!name || quantity < 1) {
      return;
    }
    const done = await this.run(
      this.api.adminAddResource(this.data.playerId, name, quantity),
      `${quantity} × ${name} ajouté(s)`,
    );
    if (done) {
      this.resourceQuantity.set(1);
    }
  }

  async removeResource(resource: PlayerResource): Promise<void> {
    const quantity = this.clampQuantity(
      this.resourceRemoveQuantity()[resource.name] ?? 1,
      resource.quantity,
    );
    const done = await this.run(
      this.api.adminRemoveResource(this.data.playerId, resource.name, quantity),
      `${quantity} × ${resource.name} retiré(s)`,
    );
    if (done) {
      this.resourceRemoveQuantity.update((map) => ({ ...map, [resource.name]: 1 }));
    }
  }

  async addEquipment(): Promise<void> {
    const name = this.equipmentName();
    const quantity = this.equipmentQuantity();
    if (!name || quantity < 1) {
      return;
    }
    const done = await this.run(
      this.api.adminAddEquipment(this.data.playerId, name, quantity),
      `${quantity} × ${name} ajouté(s)`,
    );
    if (done) {
      this.equipmentQuantity.set(1);
    }
  }

  async removeEquipment(stack: EquipmentStack): Promise<void> {
    const name = stack.equipment.name;
    const quantity = this.clampQuantity(this.equipmentRemoveQuantity()[name] ?? 1, stack.quantity);
    const fromUnits = Math.max(0, quantity - stack.available);
    if (fromUnits > 0) {
      const detail = this.carrierNames(name) || 'aucune unité identifiée';
      const confirmed = await this.confirm({
        title: `Retirer ${quantity} × ${name} ?`,
        message: `${fromUnits} exemplaire(s) sont équipés sur : ${detail}. Ils seront déséquipés avant le retrait.`,
        confirmLabel: 'Retirer',
      });
      if (!confirmed) {
        return;
      }
    }
    const done = await this.run(
      this.api.adminRemoveEquipment(this.data.playerId, name, quantity, fromUnits > 0),
      `${quantity} × ${name} retiré(s)`,
    );
    if (done) {
      this.equipmentRemoveQuantity.update((map) => ({ ...map, [name]: 1 }));
    }
  }

  async addUnit(): Promise<void> {
    const type = this.unitType();
    const classes = this.unitClasses();
    const quantity = this.unitQuantity();
    if (!type || classes.length === 0 || quantity < 1) {
      return;
    }
    const done = await this.run(
      this.api.adminAddUnits(this.data.playerId, {
        type,
        classes,
        quantity,
        sectorNumber: this.unitSector(),
      }),
      `${quantity} × ${type} ajouté(s)`,
    );
    if (done) {
      this.unitQuantity.set(1);
    }
  }

  async savePlayerName(): Promise<void> {
    const name = this.playerName().trim();
    if (!name) return;
    await this.run(
      this.api.adminUpdatePlayerName(this.data.playerId, name),
      'Nom du joueur mis à jour',
    );
  }

  async saveCharacter(): Promise<void> {
    if (!this.validCharacter()) return;
    const payload = this.characterPayload();
    const call = this.character()
      ? this.api.adminUpdateCharacter(this.data.playerId, payload)
      : this.api.adminCreateCharacter(this.data.playerId, payload);
    await this.run(call, this.character() ? 'Personnage mis à jour' : 'Personnage créé');
  }

  async deleteCharacter(): Promise<void> {
    const character = this.character();
    if (!character) return;
    const confirmed = await this.confirm({
      title: 'Supprimer le personnage',
      message: `Supprimer ${character.name} ? Il sera détaché de son secteur et de son véhicule.`,
      confirmLabel: 'Supprimer',
    });
    if (!confirmed) return;
    const done = await this.run(
      this.api.adminDeleteCharacter(this.data.playerId),
      'Personnage supprimé',
    );
    if (done) {
      this.resetCharacterForm();
    }
  }

  async loadAudit(page = 0, append = false): Promise<void> {
    if (this.auditLoading()) return;
    this.auditLoading.set(true);
    this.auditError.set(false);
    try {
      const result = await firstValueFrom(this.api.adminGetAudit(this.data.playerId, page, 20));
      this.auditEntries.set(append ? [...this.auditEntries(), ...result.content] : result.content);
      this.auditTotal.set(result.totalElements ?? result.content.length);
      this.auditPage.set(page);
    } catch {
      // Historique indisponible : la fiche reste utilisable.
      this.auditError.set(true);
    } finally {
      this.auditLoading.set(false);
    }
  }

  loadMoreAudit(): void {
    void this.loadAudit(this.auditPage() + 1, true);
  }

  auditActionLabel(action: string): string {
    return this.auditActionLabels[action] ?? action;
  }

  async reassignSector(): Promise<void> {
    const sector = this.selectedSector();
    if (!sector || sector.number === null) return;
    const target = this.sectorTargets().find((candidate) => candidate.id === this.sectorTarget());
    const targetLabel = target?.name ?? 'neutre';
    const entities = [
      sector.army?.length ? `${sector.army.length} unité(s)` : '',
      sector.vehicles?.length ? `${sector.vehicles.length} véhicule(s)` : '',
      sector.buildings?.length ? `${sector.buildings.length} bâtiment(s)` : '',
      sector.character ? '1 personnage' : '',
    ].filter(Boolean);
    const entityLabel = entities.length > 0 ? entities.join(', ') : 'aucune entité';
    const confirmed = await this.confirm({
      title: 'Réassigner le secteur',
      message: `Réassigner ${sector.name} (#${sector.number}) à ${targetLabel} ? Les entités présentes (${entityLabel}) restent sur place et pourront recapturer le secteur.`,
      confirmLabel: 'Réassigner',
    });
    if (!confirmed) return;
    const done = await this.run(
      this.api.adminReassignSector(sector.number, this.sectorTarget()),
      'Secteur réassigné',
    );
    if (done) {
      this.sectorChoice.set(undefined);
      this.sectorTarget.set(null);
    }
  }

  setSectorToReassign(value: number | null): void {
    this.sectorChoice.set(value);
  }

  private seedCharacterForm(): void {
    const character = this.character();
    this.characterName.set(character?.name ?? '');
    this.characterAttack.set(character?.baseAttack ?? 0);
    this.characterDefense.set(character?.baseDefense ?? 0);
    this.characterPdf.set(character?.basePdf ?? 0);
    this.characterPdc.set(character?.basePdc ?? 0);
    this.characterArmor.set(character?.baseArmor ?? 0);
    this.characterEvasion.set(character?.baseEvasion ?? 0);
  }

  private resetCharacterForm(): void {
    this.characterName.set('');
    this.characterAttack.set(0);
    this.characterDefense.set(0);
    this.characterPdf.set(0);
    this.characterPdc.set(0);
    this.characterArmor.set(0);
    this.characterEvasion.set(0);
  }

  openUnitEdit(unit: Unit): void {
    if (unit.id === null) return;
    this.dialog.open(UnitEditDialogComponent, {
      width: '95vw',
      maxWidth: '900px',
      maxHeight: '90vh',
      data: { playerId: this.data.playerId, unitId: unit.id },
    });
  }

  async deleteUnit(unit: Unit): Promise<void> {
    const confirmed = await this.confirm({
      title: "Supprimer l'unité",
      message: `Supprimer ${this.unitLabel(unit)} (${this.sectorLabel(unit.sectorNumber)}) ? Ses équipements reviennent à l'inventaire du joueur.`,
      confirmLabel: 'Supprimer',
    });
    if (!confirmed) {
      return;
    }
    await this.run(this.api.adminDeleteUnit(this.data.playerId, unit.id), 'Unité supprimée');
  }

  async addVehicle(): Promise<void> {
    const vehicleType = this.vehicleType();
    const quantity = this.vehicleQuantity();
    if (!vehicleType || quantity < 1) {
      return;
    }
    const done = await this.run(
      this.api.adminAddVehicles(this.data.playerId, {
        vehicleType,
        quantity,
        sectorNumber: this.vehicleSector(),
      }),
      `${quantity} × ${vehicleType} ajouté(s)`,
    );
    if (done) {
      this.vehicleQuantity.set(1);
    }
  }

  async deleteVehicle(vehicle: Vehicle): Promise<void> {
    if (vehicle.id === null) {
      return;
    }
    const confirmed = await this.confirm({
      title: 'Supprimer le véhicule',
      message: `Supprimer ${vehicle.displayName} (${this.sectorLabel(vehicle.sectorNumber)}) ? L'équipage sera débarqué dans le secteur.`,
      confirmLabel: 'Supprimer',
    });
    if (!confirmed) {
      return;
    }
    await this.run(
      this.api.adminDeleteVehicle(this.data.playerId, vehicle.id),
      'Véhicule supprimé',
    );
  }

  async addBuilding(): Promise<void> {
    const sectorNumber = this.buildingSector();
    if (sectorNumber === null) {
      return;
    }
    const type = this.buildingType();
    await this.run(
      this.api.adminAddBuilding(this.data.playerId, { buildingType: type, sectorNumber }),
      `${this.buildingLabels[type]} ajouté`,
    );
  }

  async deleteBuilding(building: Building): Promise<void> {
    if (building.id === null) {
      return;
    }
    const warning = this.buildingWarning(building.buildingType);
    const confirmed = await this.confirm({
      title: 'Supprimer le bâtiment',
      message: `Supprimer ${this.buildingLabels[building.buildingType]} (${this.sectorLabel(building.sectorNumber)}) ?${warning}`,
      confirmLabel: 'Supprimer',
    });
    if (!confirmed) {
      return;
    }
    await this.run(
      this.api.adminDeleteBuilding(this.data.playerId, building.id),
      'Bâtiment supprimé',
    );
  }

  private buildingWarning(type: BuildingType): string {
    switch (type) {
      case 'HEADQUARTERS':
        return " Le joueur n'aura plus de QG.";
      case 'BANK':
        return ' Les ressources stockées dans la banque seront perdues.';
      case 'WEAPON_CACHE':
        return ' Les équipements stockés dans la cache seront perdus.';
    }
  }

  private characterPayload() {
    return {
      name: this.characterName().trim(),
      baseAttack: this.characterAttack(),
      baseDefense: this.characterDefense(),
      basePdf: this.characterPdf(),
      basePdc: this.characterPdc(),
      baseArmor: this.characterArmor(),
      baseEvasion: this.characterEvasion(),
    };
  }

  validCharacter(): boolean {
    const name = this.characterName().trim();
    if (name.length === 0 || name.length > 100) return false;
    return [
      this.characterAttack(),
      this.characterDefense(),
      this.characterPdf(),
      this.characterPdc(),
      this.characterArmor(),
      this.characterEvasion(),
    ].every((stat) => Number.isFinite(stat) && stat >= 0);
  }

  private clampQuantity(value: number, max: number): number {
    const rounded = Math.trunc(Number.isFinite(value) ? value : 1);
    return Math.min(Math.max(1, rounded), Math.max(1, max));
  }

  private async confirm(data: ConfirmDialogData): Promise<boolean> {
    return (
      (await firstValueFrom(this.dialog.open(ConfirmDialogComponent, { data }).afterClosed())) ===
      true
    );
  }

  private async run(call: Observable<unknown>, successMessage: string): Promise<boolean> {
    if (this.busy()) {
      return false;
    }
    this.busy.set(true);
    try {
      await this.admin.mutate(call, successMessage);
      void this.loadAudit();
      return true;
    } catch {
      // Erreur déjà exposée par AdminService (snackbar de la page admin).
      return false;
    } finally {
      this.busy.set(false);
    }
  }
}
