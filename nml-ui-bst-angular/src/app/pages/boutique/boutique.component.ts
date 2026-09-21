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
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatBadgeModule } from '@angular/material/badge';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatDividerModule } from '@angular/material/divider';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { Equipment, EquipmentStack, PlayerResource, UnitCartItem, UnitCatalogEntry, VehicleTypeInfo } from '../../models';
import { ShopService, unitCartKey } from '../../services/shop.service';
import { PlayerService } from '../../services/player.service';
import {
  clampUnitQuantity,
  compareEquipments,
  equipmentBonusSummary,
  equipmentClassLabel,
  equipmentSummary,
  sortVehiclesByCost,
  unitCartQuantityForType,
  unitQuotaRemaining,
  vehicleSummary,
} from './boutique.helpers';
import { equipmentCategoryLabel, unitClassLabel } from '../../core/labels';
import { slugify } from '../../core/slug';
import { saleMultiplier, saleValue } from '../../core/sale-multiplier';
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
    MatChipsModule,
    MatButtonModule,
    MatBadgeModule,
    MatSidenavModule,
    MatDividerModule,
    MatTabsModule,
    MatTooltipModule,
    MatDialogModule,
    MatFormFieldModule,
    MatSelectModule,
  ],
  templateUrl: './boutique.component.html',
  styleUrls: ['./boutique.component.scss'],
})
export class BoutiqueComponent {
  private readonly shop = inject(ShopService);
  private readonly playerService = inject(PlayerService);
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
  readonly showFilters = signal(false);
  readonly searchTerm = signal('');
  readonly selectedCategory = signal<string>('all');
  readonly selectedBonusFilter = signal<string>('all');
  readonly vehicleQuantities = signal<Record<string, number>>({});
  readonly unitQuantities = signal<Record<string, number>>({});
  readonly unitClassChoices = signal<Record<string, string>>({});
  readonly resourceSellQuantities = signal<Record<number, number>>({});

  // Images boutique : track des vignettes introuvables (fallback icône).
  private readonly _brokenImages = signal<ReadonlySet<string>>(new Set());

  readonly categories = computed(() => {
    const cats = new Set<string>();
    this.allEquipments().forEach((eq) => {
      if (eq.category) cats.add(eq.category);
    });
    return Array.from(cats)
      .sort((a, b) => a.localeCompare(b))
      .map((key) => ({ key, label: equipmentCategoryLabel(key) }));
  });

  readonly filteredEquipments = computed(() => {
    let filtered = [...this.allEquipments()];

    const search = this.searchTerm().toLowerCase().trim();
    if (search) filtered = filtered.filter((eq) => eq.name.toLowerCase().includes(search));

    const category = this.selectedCategory();
    if (category !== 'all') filtered = filtered.filter((eq) => eq.category === category);

    const bonus = this.selectedBonusFilter();
    if (bonus !== 'all') {
      filtered = filtered.filter((eq) => {
        switch (bonus) {
          case 'PDF':
            return eq.pdfBonus > 0;
          case 'PDC':
            return eq.pdcBonus > 0;
          case 'ARM':
            return eq.armBonus > 0;
          case 'ESQ':
            return eq.evasionBonus > 0;
          default:
            return true;
        }
      });
    }
    return filtered.sort(compareEquipments);
  });

  readonly sortedVehicleTypes = computed(() => sortVehiclesByCost(this.vehicleTypes()));

  /**
   * Équipements groupés par 1re classe compatible (ordre hérité du tri :
   * Léger → Mastodonte → Tireur → Sniper → Pilote destructeur → Élémentaire).
   * Utilisé pour les titres de section en vue par défaut (sans filtre).
   */
  readonly groupedEquipments = computed(() => {
    const items = this.filteredEquipments();
    const groups: { classKey: string; classLabel: string; items: Equipment[] }[] = [];
    const idx = new Map<string, number>();
    for (const eq of items) {
      const key = eq.compatibleClass?.[0]?.name ?? 'AUCUNE';
      if (!idx.has(key)) {
        idx.set(key, groups.length);
        groups.push({ classKey: key, classLabel: unitClassLabel(key), items: [] });
      }
      groups[idx.get(key)!].items.push(eq);
    }
    return groups;
  });

  readonly totalCartBadge = computed(
    () => this.totalItems() + this.vehicleCartTotalItems() + this.unitCartTotalItems(),
  );

  // Normalized cart lines for the shared `cartItem` template.
  readonly equipmentCartLines = computed(() =>
    this.cart().map((i) => ({
      kind: 'eq',
      key: i.equipment.name,
      name: i.equipment.name,
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

  readonly hasActiveFilters = computed(
    () =>
      this.searchTerm() !== '' ||
      this.selectedCategory() !== 'all' ||
      this.selectedBonusFilter() !== 'all',
  );

  readonly hasAdvancedFilters = computed(
    () => this.selectedCategory() !== 'all' || this.selectedBonusFilter() !== 'all',
  );

  readonly getActiveFiltersCount = computed(() => {
    let count = 0;
    if (this.selectedCategory() !== 'all') count++;
    if (this.selectedBonusFilter() !== 'all') count++;
    return count;
  });

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

  addToCart(equipment: Equipment): void {
    this.shop.addToCart(equipment);
  }

  removeFromCart(name: string): void {
    this.shop.removeFromCart(name);
  }

  updateQuantity(name: string, quantity: number): void {
    this.shop.updateCartItemQuantity(name, quantity);
  }

  decrementCartQuantity(name: string): void {
    const currentQty = this.getCartQuantity(name);
    if (currentQty > 1) {
      this.shop.updateCartItemQuantity(name, currentQty - 1);
    } else {
      this.shop.removeFromCart(name);
    }
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
        lines: snapshot.map((item) => `${item.quantity} × ${item.equipment.name}`),
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
      unitCartQuantityForType(this.unitCart(), entry.name),
    );
  }

  /** Quota consommé (achats + panier) : affiché pour rester cohérent avec « Limite atteinte ». */
  unitQuotaUsed(entry: UnitCatalogEntry): number {
    return entry.maxPerTurn - this.unitRemaining(entry);
  }

  private defaultUnitClass(): string {
    const classes = this.unitClasses();
    return classes.find((c) => c.name === 'ELEMENTAIRE')?.name ?? classes[0]?.name ?? '';
  }

  unitClassChoice(entry: UnitCatalogEntry): string {
    return this.unitClassChoices()[entry.name] ?? this.defaultUnitClass();
  }

  setUnitClass(entryName: string, className: string): void {
    this.unitClassChoices.update((prev) => ({ ...prev, [entryName]: className }));
  }

  getUnitQuantity(entryName: string): number {
    return this.unitQuantities()[entryName] ?? 1;
  }

  /** Quantité réellement ajoutable : reflète le clamp quand le panier consomme déjà le quota. */
  unitQuantityToAdd(entry: UnitCatalogEntry): number {
    return clampUnitQuantity(this.getUnitQuantity(entry.name), this.unitRemaining(entry));
  }

  setUnitQuantity(entry: UnitCatalogEntry, qty: number): void {
    const clamped = clampUnitQuantity(qty, this.unitRemaining(entry));
    this.unitQuantities.update((prev) => ({
      ...prev,
      [entry.name]: Math.max(1, clamped),
    }));
  }

  canAddUnit(entry: UnitCatalogEntry): boolean {
    return entry.availableNow && this.unitRemaining(entry) > 0;
  }

  addUnitToCart(entry: UnitCatalogEntry): void {
    const unitClass = this.unitClasses().find((c) => c.name === this.unitClassChoice(entry));
    if (!unitClass) return;
    const qty = this.unitQuantityToAdd(entry);
    if (qty <= 0) return;
    this.shop.addUnitToCart(entry, unitClass, qty);
  }

  unitCartLineQuantity(key: string): number {
    return (
      this.unitCart().find((i) => unitCartKey(i.unitType.name, i.unitClass.name) === key)?.quantity ??
      0
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
          (item) => `${item.quantity} × ${item.unitType.name} (${this.unitClassLabel(item.unitClass.name)})`,
        ),
        totalCost: snapshot.reduce((s, i) => s + i.unitType.cost * i.quantity, 0),
      }),
    );
  }

  getSellQty(resource: PlayerResource): number {
    return resource.id != null ? (this.resourceSellQuantities()[resource.id] ?? 1) : 1;
  }

  setSellQty(resource: PlayerResource, qty: number): void {
    if (resource.id == null) return;
    const id = resource.id;
    const clamped = Math.max(1, Math.min(qty, resource.quantity));
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

  toggleFilters(): void {
    this.showFilters.update((v) => !v);
  }

  selectCategory(category: string): void {
    this.selectedCategory.set(category);
  }

  selectBonusFilter(bonus: string): void {
    this.selectedBonusFilter.set(bonus);
  }

  clearAdvancedFilters(): void {
    this.selectedCategory.set('all');
    this.selectedBonusFilter.set('all');
  }

  equipmentCategoryLabel = equipmentCategoryLabel;
  unitClassLabel = unitClassLabel;
  unitCartKey = unitCartKey;
  equipmentSummary = equipmentSummary;
  equipmentBonusSummary = equipmentBonusSummary;
  equipmentClassLabel = equipmentClassLabel;
  vehicleSummary = vehicleSummary;
  saleMultiplier = saleMultiplier;
  saleValue = saleValue;

  equipmentImageUrl(equipment: Equipment): string {
    return `assets/shop/equipment/${slugify(equipment.name)}.png`;
  }

  // ponytail: toLowerCase() et non slugify() — vt.name est le nom de l'énum
  // Java (ex. VTT_LEGER) dont les '_' doivent être conservés dans le nom de
  // fichier ; slugify remplacerait '_' par '-' et casserait l'URL.
  vehicleImageUrl(vt: VehicleTypeInfo): string {
    return `assets/shop/vehicles/${vt.name.toLowerCase()}.png`;
  }

  unitImageUrl(entry: UnitCatalogEntry): string {
    return `assets/shop/units/${entry.name.toLowerCase()}.png`;
  }

  resourceImageUrl(resource: PlayerResource): string {
    return `assets/shop/resources/${slugify(resource.name)}.png`;
  }

  hasImage(key: string): boolean {
    return !this._brokenImages().has(key);
  }

  onImgError(key: string): void {
    this._brokenImages.update((set) => new Set(set).add(key));
  }
}
