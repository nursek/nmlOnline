import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  linkedSignal,
  signal,
} from '@angular/core';
import { DecimalPipe, NgTemplateOutlet } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatBadgeModule } from '@angular/material/badge';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatDividerModule } from '@angular/material/divider';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import {
  Equipment,
  EquipmentStack,
  PlayerResource,
  UnitCartItem,
  UnitCatalogEntry,
  UnitClass,
  VehicleTypeInfo,
} from '../../models';
import { ShopService, unitCartKey } from '../../services/shop.service';
import { PlayerService } from '../../services/player.service';
import {
  clampUnitQuantity,
  compareEquipments,
  equipmentClassLabel,
  equipmentSummary,
  groupEquipmentsByClass,
  matchesEquipmentSearch,
  sortVehiclesByCost,
  unitCartQuantityForType,
  unitClassBonusSummary,
  unitImageUrl,
  unitPendingQuantityForType,
  unitQuotaRemaining,
  vehicleSummary,
} from './boutique.helpers';
import { UNIT_CLASS_ORDER, unitClassLabel } from '../../core/labels';
import {
  equipmentLabel as equipmentLabelFor,
  equipmentDescription as equipmentDescriptionFor,
} from '../../core/equipment-label';
import { EquipmentImageService } from '../../core/equipment-image.service';
import { optimizedImage } from '../../core/optimized-image';
import type { OptimizedImage } from '../../core/optimized-image';
import { slugify } from '../../core/slug';
import { SALE_MAX_QUANTITY, saleMultiplier, saleValue } from '../../core/sale-multiplier';
import {
  PurchaseSuccessDialogComponent,
  PurchaseSuccessData,
} from '../../shared/purchase-success-dialog/purchase-success-dialog.component';

@Component({
  selector: 'app-boutique',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DecimalPipe,
    NgTemplateOutlet,
    MatCardModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatButtonModule,
    MatBadgeModule,
    MatSidenavModule,
    MatDividerModule,
    MatTabsModule,
    MatTooltipModule,
    MatDialogModule,
  ],
  templateUrl: './boutique.component.html',
  styleUrls: ['./boutique.component.scss'],
})
export class BoutiqueComponent {
  private readonly shop = inject(ShopService);
  private readonly playerService = inject(PlayerService);
  private readonly equipmentImages = inject(EquipmentImageService);
  private readonly dialog = inject(MatDialog);

  readonly allEquipments = this.shop.equipments;
  readonly cart = this.shop.cart;
  readonly vehicleCart = this.shop.vehicleCart;
  readonly unitCart = this.shop.unitCart;
  readonly sellCart = this.shop.sellCart;
  readonly error = this.shop.error;
  readonly loading = this.shop.equipmentsLoading;
  readonly purchaseLoading = this.shop.purchaseLoading;
  readonly totalItems = this.shop.cartTotalItems;
  readonly totalPrice = this.shop.cartTotalPrice;
  readonly vehicleCartTotalItems = this.shop.vehicleCartTotalItems;
  readonly vehicleCartTotalPrice = this.shop.vehicleCartTotalPrice;
  readonly unitCartTotalItems = this.shop.unitCartTotalItems;
  readonly unitCartTotalPrice = this.shop.unitCartTotalPrice;
  readonly sellCartTotalValue = this.shop.sellCartTotalValue;
  readonly vehicleTypes = this.shop.vehicleTypes;
  readonly unitCatalog = this.shop.unitCatalog;
  readonly unitTypes = computed(() => this.shop.unitCatalog().entries);
  readonly unitClasses = computed(() => this.shop.unitCatalog().classes);

  /** Une entrée d'unité à la fois : ses classes de recrutement. */
  readonly selectedUnitEntry = computed(() => {
    const entries = this.unitTypes();
    return entries.find((entry) => entry.name === this.selectedUnitType()) ?? entries[0];
  });

  readonly player = this.playerService.player;
  readonly currentTurn = this.playerService.currentTurn;

  readonly tab = input<string>('equipements');
  readonly selectedTabIndex = linkedSignal(() => this.tabIndex(this.tab()));

  private tabIndex(tab: string): number {
    switch (tab) {
      case 'vehicules':
        return 1;
      case 'unites':
        return 2;
      case 'revente':
        return 3;
      default:
        return 0;
    }
  }

  readonly showCart = signal(false);
  readonly searchOpen = signal(false);
  readonly searchTerm = signal('');
  // Sélection par défaut : la 1re classe du catalogue (le filtrage par classe est la navigation).
  readonly selectedClassFilter = linkedSignal(() => this.equipmentClasses()[0]?.key ?? 'all');
  readonly selectedUnitType = linkedSignal(() => this.unitTypes()[0]?.name ?? '');
  readonly vehicleQuantities = signal<Record<string, number>>({});
  readonly unitQuantities = signal<Record<string, number>>({});
  readonly equipmentQuantities = signal<Record<string, number>>({});
  readonly resourceSellQuantities = signal<Record<number, number>>({});

  // Images boutique : track des vignettes introuvables (fallback icône).
  private readonly _brokenImages = signal<ReadonlySet<string>>(new Set());

  readonly filteredEquipments = computed(() => {
    let filtered = [...this.allEquipments()];

    const search = this.searchTerm().toLowerCase().trim();
    if (search) {
      filtered = filtered.filter((eq) => matchesEquipmentSearch(eq, search, this.player()?.race));
    } else {
      const unitClass = this.selectedClassFilter();
      filtered = filtered.filter((eq) =>
        eq.compatibleClass?.some((compatible) => compatible.name === unitClass),
      );
    }

    return filtered.sort(compareEquipments);
  });

  readonly sortedVehicleTypes = computed(() => sortVehiclesByCost(this.vehicleTypes()));

  readonly vehicleGroups = computed(() => {
    const groups = new Map<number, VehicleTypeInfo[]>();
    for (const vt of this.sortedVehicleTypes()) {
      const bucket = groups.get(vt.availableFromTurn);
      if (bucket) bucket.push(vt);
      else groups.set(vt.availableFromTurn, [vt]);
    }
    return [...groups.entries()]
      .sort(([a], [b]) => a - b)
      .map(([turn, items]) => ({ turn, items }));
  });

  // L'ordre dans chaque section vient de `compareEquipments` (classe puis coût), pas d'un tri local.
  readonly equipmentGroups = computed(() => groupEquipmentsByClass(this.filteredEquipments()));

  readonly equipmentClasses = computed(() => {
    const keys = new Set<string>();
    for (const eq of this.allEquipments()) {
      for (const compatible of eq.compatibleClass ?? []) {
        keys.add(compatible.name);
      }
    }
    return [...keys]
      .sort(
        (a, b) =>
          (UNIT_CLASS_ORDER[a] ?? Number.MAX_SAFE_INTEGER) -
          (UNIT_CLASS_ORDER[b] ?? Number.MAX_SAFE_INTEGER),
      )
      .map((key) => ({ key, label: unitClassLabel(key) }));
  });

  readonly totalCartBadge = computed(
    () => this.totalItems() + this.vehicleCartTotalItems() + this.unitCartTotalItems(),
  );

  // Normalized cart lines for the shared `cartItem` template.
  readonly equipmentCartLines = computed(() =>
    this.cart().map((i) => ({
      kind: 'eq',
      key: i.equipment.name,
      name: this.equipmentLabel(i.equipment),
      cost: i.equipment.cost,
      qty: i.quantity,
      vehicleType: null,
    })),
  );
  readonly vehicleCartLines = computed(() =>
    this.vehicleCart().map((i) => ({
      kind: 'veh',
      key: i.vehicleType.name,
      name: i.vehicleType.displayName,
      cost: i.vehicleType.cost,
      qty: i.quantity,
      vehicleType: i.vehicleType,
    })),
  );

  readonly canAfford = computed(() => (this.player()?.stats?.money ?? 0) >= this.totalPrice());
  readonly canAffordVehicleCart = computed(
    () => (this.player()?.stats?.money ?? 0) >= this.vehicleCartTotalPrice(),
  );
  readonly canAffordUnitCart = computed(
    () => (this.player()?.stats?.money ?? 0) >= this.unitCartTotalPrice(),
  );

  /** Recherche en cours : les résultats balaient tout le catalogue, d'où les en-têtes de classe. */
  readonly showClassTitles = computed(() => this.searchTerm().trim() !== '');

  constructor() {
    // Ensure the player profile is loaded for the money display / sell tab.
    void this.playerService.loadCurrent();
    void this.playerService.loadCurrentTurn();
    this.shop.refreshUnitCatalog();
  }

  private openSuccessDialog(data: PurchaseSuccessData): void {
    this.dialog.open(PurchaseSuccessDialogComponent, { width: '400px', data });
  }

  private async runCheckout(
    itemCount: number,
    checkout: () => Promise<unknown>,
    data: (result: unknown) => PurchaseSuccessData,
  ): Promise<void> {
    if (!itemCount) return;
    this.showCart.set(false);
    try {
      this.openSuccessDialog(data(await checkout()));
    } catch {
      // Error already surfaced through the shop service's `error` signal.
    }
  }

  toggleCart(): void {
    this.showCart.update((v) => !v);
  }

  getEquipmentQuantity(equipmentName: string): number {
    return this.equipmentQuantities()[equipmentName] ?? 1;
  }

  setEquipmentQuantity(equipmentName: string, quantity: number): void {
    this.equipmentQuantities.update((prev) => ({
      ...prev,
      [equipmentName]: Math.max(1, quantity),
    }));
  }

  addEquipmentToCart(equipment: Equipment): void {
    const qty = this.getEquipmentQuantity(equipment.name);
    this.shop.addToCart(equipment, qty);
    // La saisie repasse à 1 : sinon la quantité s'accumule à chaque clic.
    this.equipmentQuantities.update((prev) => {
      const next = { ...prev };
      delete next[equipment.name];
      return next;
    });
  }

  removeFromCart(name: string): void {
    this.shop.removeFromCart(name);
  }

  updateQuantity(name: string, quantity: number): void {
    this.shop.updateCartItemQuantity(name, quantity);
  }

  clearCart(): void {
    this.shop.clearCart();
  }

  getOwnedQuantity(equipmentName: string): number {
    const stack = this.player()?.equipments?.find(
      (e: EquipmentStack) => e.equipment.name === equipmentName,
    );
    return stack?.quantity || 0;
  }

  getCartQuantity(equipmentName: string): number {
    return this.cart().find((i) => i.equipment.name === equipmentName)?.quantity || 0;
  }

  async checkout(): Promise<void> {
    const snapshot = [...this.cart()];
    await this.runCheckout(
      snapshot.length,
      () => this.shop.checkoutEquipments(),
      () => ({
        title: 'Équipements achetés !',
        lines: snapshot.map((item) => `${item.quantity} × ${this.equipmentLabel(item.equipment)}`),
        totalCost: snapshot.reduce((s, i) => s + i.equipment.cost * i.quantity, 0),
      }),
    );
  }

  addVehicleToCart(vehicleType: VehicleTypeInfo): void {
    const qty = this.vehicleQuantities()[vehicleType.name] ?? 1;
    this.shop.addVehicleToCart(vehicleType, qty);
  }

  removeVehicleFromCart(name: string): void {
    this.shop.removeVehicleFromCart(name);
  }

  decrementVehicleCartQuantity(name: string): void {
    const current = this.getVehicleCartQuantity(name);
    if (current > 1) {
      this.shop.updateVehicleCartItemQuantity(name, current - 1);
    } else {
      this.shop.removeVehicleFromCart(name);
    }
  }

  getVehicleCartQuantity(name: string): number {
    return this.vehicleCart().find((i) => i.vehicleType.name === name)?.quantity ?? 0;
  }

  async checkoutVehicles(): Promise<void> {
    const snapshot = [...this.vehicleCart()];
    await this.runCheckout(
      snapshot.length,
      () => this.shop.checkoutVehicles(),
      () => ({
        title: 'Véhicules achetés !',
        lines: snapshot.map((item) => `${item.quantity} × ${item.vehicleType.displayName}`),
        totalCost: snapshot.reduce((s, i) => s + i.vehicleType.cost * i.quantity, 0),
      }),
    );
  }

  getVehicleQuantity(vehicleTypeName: string): number {
    return this.vehicleQuantities()[vehicleTypeName] ?? 1;
  }

  setVehicleQuantity(vehicleTypeName: string, qty: number): void {
    this.vehicleQuantities.update((prev) => ({ ...prev, [vehicleTypeName]: Math.max(1, qty) }));
  }

  canAffordVehicle(vehicleCost: number, qty: number = 1): boolean {
    return (this.player()?.stats?.money ?? 0) >= vehicleCost * qty;
  }

  /** Tour inconnu = on laisse acheter, le backend reste juge. */
  vehicleAvailable(vt: VehicleTypeInfo): boolean {
    const turn = this.currentTurn();
    return turn == null || turn >= vt.availableFromTurn;
  }

  unitRemaining(entry: UnitCatalogEntry): number {
    return unitQuotaRemaining(
      entry.maxPerTurn,
      entry.purchasedThisTurn,
      unitCartQuantityForType(this.unitCart(), entry.name) +
        unitPendingQuantityForType(this.unitQuantities(), entry.name),
    );
  }

  /** Quota consommé (achats + panier + saisies en cours) : la preview se met à jour en direct. */
  unitQuotaUsed(entry: UnitCatalogEntry): number {
    return entry.maxPerTurn - this.unitRemaining(entry);
  }

  private unitQuantityKey(entry: UnitCatalogEntry, uc: UnitClass): string {
    return unitCartKey(entry.name, uc.name);
  }

  private getUnitQuantity(key: string): number {
    return this.unitQuantities()[key] ?? 1;
  }

  /** Plafond de la carte : `unitRemaining` inclut déjà sa propre saisie comptée, on la ré-ajoute. */
  unitQuantityLimit(entry: UnitCatalogEntry, uc: UnitClass): number {
    return (
      this.unitRemaining(entry) + (this.unitQuantities()[this.unitQuantityKey(entry, uc)] ?? 0)
    );
  }

  /** Quantité réellement ajoutable : reflète le clamp quand le panier consomme déjà le quota. */
  unitQuantityToAdd(entry: UnitCatalogEntry, uc: UnitClass): number {
    return clampUnitQuantity(
      this.getUnitQuantity(this.unitQuantityKey(entry, uc)),
      this.unitQuantityLimit(entry, uc),
    );
  }

  setUnitQuantity(entry: UnitCatalogEntry, uc: UnitClass, qty: number): void {
    const clamped = clampUnitQuantity(qty, this.unitQuantityLimit(entry, uc));
    if (clamped <= 0) return;
    this.unitQuantities.update((prev) => ({
      ...prev,
      [this.unitQuantityKey(entry, uc)]: clamped,
    }));
  }

  onUnitQuantityInput(entry: UnitCatalogEntry, uc: UnitClass, event: Event): void {
    const value = (event.target as HTMLInputElement).valueAsNumber;
    if (Number.isNaN(value)) return;
    this.setUnitQuantity(entry, uc, value);
  }

  normalizeUnitQuantity(entry: UnitCatalogEntry, uc: UnitClass, event: Event): void {
    (event.target as HTMLInputElement).value = String(this.unitQuantityToAdd(entry, uc));
  }

  canAddUnit(entry: UnitCatalogEntry, uc: UnitClass): boolean {
    return entry.availableNow && this.unitQuantityToAdd(entry, uc) > 0;
  }

  addUnitToCart(entry: UnitCatalogEntry, unitClass: UnitClass): void {
    const qty = this.unitQuantityToAdd(entry, unitClass);
    if (qty <= 0) return;
    this.shop.addUnitToCart(entry, unitClass, qty);
    // La saisie repasse à 1 : sinon le quota compterait la ligne deux fois (saisie + panier).
    this.unitQuantities.update((prev) => {
      const next = { ...prev };
      delete next[this.unitQuantityKey(entry, unitClass)];
      return next;
    });
  }

  unitCartLineQuantity(key: string): number {
    return (
      this.unitCart().find((i) => unitCartKey(i.unitType.name, i.unitClass.name) === key)
        ?.quantity ?? 0
    );
  }

  decrementUnitCartQuantity(key: string): void {
    const current = this.unitCartLineQuantity(key);
    if (current > 1) {
      this.shop.updateUnitCartItemQuantity(key, current - 1);
    } else {
      this.shop.removeUnitFromCart(key);
    }
  }

  incrementUnitCartLine(item: UnitCartItem): void {
    const entry = this.unitTypes().find((e) => e.name === item.unitType.name);
    if (!entry || this.unitRemaining(entry) <= 0) return;
    const key = unitCartKey(item.unitType.name, item.unitClass.name);
    this.shop.updateUnitCartItemQuantity(key, this.unitCartLineQuantity(key) + 1);
  }

  canIncrementUnitCartLine(item: UnitCartItem): boolean {
    const entry = this.unitTypes().find((e) => e.name === item.unitType.name);
    return entry != null && this.unitRemaining(entry) > 0;
  }

  removeUnitFromCart(key: string): void {
    this.shop.removeUnitFromCart(key);
  }

  clearUnitCart(): void {
    this.shop.clearUnitCart();
  }

  async checkoutUnits(): Promise<void> {
    const snapshot = [...this.unitCart()];
    await this.runCheckout(
      snapshot.length,
      () => this.shop.checkoutUnits(),
      () => ({
        title: 'Unités recrutées !',
        lines: snapshot.map(
          (item) =>
            `${item.quantity} × ${item.unitType.name} (${this.unitClassLabel(item.unitClass.name)})`,
        ),
        totalCost: snapshot.reduce((s, i) => s + i.unitType.cost * i.quantity, 0),
      }),
    );
  }

  getSellQty(resource: PlayerResource): number {
    return resource.id != null ? (this.resourceSellQuantities()[resource.id] ?? 1) : 1;
  }

  sellQtyMax(resource: PlayerResource): number {
    return Math.min(resource.quantity, SALE_MAX_QUANTITY);
  }

  setSellQty(resource: PlayerResource, qty: number): void {
    if (resource.id == null) return;
    const id = resource.id;
    const clamped = Math.max(1, Math.min(qty, this.sellQtyMax(resource)));
    this.resourceSellQuantities.update((prev) => ({ ...prev, [id]: clamped }));
  }

  addToSellCart(resource: PlayerResource): void {
    const qty = this.getSellQty(resource);
    this.shop.addToSellCart(resource, qty);
  }

  removeFromSellCart(resourceId: number): void {
    this.shop.removeFromSellCart(resourceId);
  }

  getSellCartQuantity(resourceId: number): number {
    return this.sellCart().find((i) => i.resource.id === resourceId)?.quantity ?? 0;
  }

  isInSellCart(resourceId: number): boolean {
    return this.sellCart().some((i) => i.resource.id === resourceId);
  }

  async checkoutSellCart(): Promise<void> {
    const snapshot = [...this.sellCart()];
    await this.runCheckout(
      snapshot.length,
      () => this.shop.checkoutSellCart(),
      (totalValue) => ({
        title: 'Ressources vendues !',
        lines: snapshot.map((item) => `${item.quantity} × ${item.resource.name}`),
        totalCost: totalValue as number,
        isSale: true,
      }),
    );
  }

  clearVehicleCart(): void {
    this.shop.clearVehicleCart();
  }

  clearSellCartAll(): void {
    this.shop.clearSellCart();
  }

  clearSearch(): void {
    this.searchTerm.set('');
  }

  toggleSearch(): void {
    this.searchOpen.update((v) => !v);
    if (!this.searchOpen()) this.searchTerm.set('');
  }

  selectClassFilter(unitClass: string): void {
    this.selectedClassFilter.set(unitClass);
    this.searchTerm.set('');
  }

  selectAdjacentClass(step: number): void {
    const classes = this.equipmentClasses();
    if (classes.length === 0) return;
    const index = classes.findIndex((c) => c.key === this.selectedClassFilter());
    const next = (index + step + classes.length) % classes.length;
    this.selectClassFilter(classes[next].key);
  }

  selectAdjacentUnitType(step: number): void {
    const entries = this.unitTypes();
    if (entries.length === 0) return;
    const index = entries.findIndex((e) => e.name === this.selectedUnitType());
    const next = (index + step + entries.length) % entries.length;
    this.selectedUnitType.set(entries[next].name);
  }

  unitClassLabel = unitClassLabel;
  unitCartKey = unitCartKey;
  equipmentSummary = equipmentSummary;
  equipmentClassLabel = equipmentClassLabel;
  vehicleSummary = vehicleSummary;
  unitClassBonusSummary = unitClassBonusSummary;
  saleMultiplier = saleMultiplier;
  saleValue = saleValue;

  equipmentLabel(equipment: Equipment): string {
    return equipmentLabelFor(equipment, this.player()?.race);
  }

  equipmentDescription(equipment: Equipment): string {
    return equipmentDescriptionFor(equipment, this.player()?.race);
  }

  /** État d'un palier de déblocage ; tour inconnu = disponible (le backend reste juge). */
  vehicleTurnStatus(turn: number): string {
    const current = this.currentTurn();
    if (current == null || current >= turn) return 'Disponible';
    const remaining = turn - current;
    return `Dans ${remaining} tour${remaining > 1 ? 's' : ''}`;
  }

  equipmentImage(equipment: Equipment): OptimizedImage | null {
    return this.equipmentImages.image(equipment);
  }

  onEquipmentImgError(equipment: Equipment): void {
    this.equipmentImages.onError(equipment);
  }

  // ponytail: toLowerCase() et non slugify() — vt.name est le nom de l'énum
  // Java (ex. VTT_LEGER) dont les '_' doivent être conservés dans le nom de
  // fichier ; slugify remplacerait '_' par '-' et casserait l'URL.
  vehicleImage(vt: VehicleTypeInfo): OptimizedImage | null {
    return this.hasImage(`veh:${vt.name}`)
      ? optimizedImage(`assets/shop/vehicles/${vt.name.toLowerCase()}.png`)
      : null;
  }

  private faction(): string {
    return this.player()?.name?.toLowerCase() ?? '';
  }

  /** Vignette par classe ; repli sur le portrait du type puis sur l'icône (cf. `onUnitImgError`). */
  unitImage(entry: UnitCatalogEntry, unitClass: UnitClass): OptimizedImage | null {
    const original = unitImageUrl(entry, unitClass, this.faction(), this._brokenImages());
    return original ? optimizedImage(original) : null;
  }

  onUnitImgError(entry: UnitCatalogEntry, unitClass: UnitClass): void {
    const classKey = `unit:${entry.name}:${unitClass.name}`;
    this.onImgError(this.hasImage(classKey) ? classKey : `unit:${entry.name}`);
  }

  resourceImage(resource: PlayerResource): OptimizedImage | null {
    return this.hasImage(`res:${resource.name}`)
      ? optimizedImage(`assets/shop/resources/${slugify(resource.name)}.png`)
      : null;
  }

  hasImage(key: string): boolean {
    return !this._brokenImages().has(key);
  }

  onImgError(key: string): void {
    this._brokenImages.update((set) => new Set(set).add(key));
  }
}
