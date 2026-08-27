import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
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
import { Equipment, EquipmentStack, PlayerResource, VehicleTypeInfo } from '../../models';
import { ShopService } from '../../services/shop.service';
import { PlayerService } from '../../services/player.service';
import {
  compareEquipments,
  equipmentBonusSummary,
  equipmentClassLabel,
  equipmentSummary,
  sortVehiclesByCost,
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
  ],
  templateUrl: './boutique.component.html',
  styleUrls: ['./boutique.component.scss'],
})
export class BoutiqueComponent {
  private readonly shop = inject(ShopService);
  private readonly playerService = inject(PlayerService);
  private readonly dialog = inject(MatDialog);

  // Catalog + carts come straight from the shop service signals.
  readonly allEquipments = this.shop.equipments;
  readonly cart = this.shop.cart;
  readonly vehicleCart = this.shop.vehicleCart;
  readonly sellCart = this.shop.sellCart;
  readonly error = this.shop.error;
  readonly loading = this.shop.equipmentsLoading;
  readonly purchaseLoading = this.shop.purchaseLoading;
  readonly totalItems = this.shop.cartTotalItems;
  readonly totalPrice = this.shop.cartTotalPrice;
  readonly vehicleCartTotalItems = this.shop.vehicleCartTotalItems;
  readonly vehicleCartTotalPrice = this.shop.vehicleCartTotalPrice;
  readonly sellCartTotalValue = this.shop.sellCartTotalValue;
  readonly vehicleTypes = this.shop.vehicleTypes;

  // Player profile is owned by PlayerService.
  readonly player = this.playerService.player;

  // UI state (local).
  readonly showCart = signal(false);
  readonly showFilters = signal(false);
  readonly searchTerm = signal('');
  readonly selectedCategory = signal<string>('all');
  readonly selectedBonusFilter = signal<string>('all');
  readonly vehicleQuantities = signal<Record<string, number>>({});
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

  /** Véhicules triés par coût croissant. */
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

  readonly totalCartBadge = computed(() => this.totalItems() + this.vehicleCartTotalItems());

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
  }

  private openSuccessDialog(data: PurchaseSuccessData): void {
    this.dialog.open(PurchaseSuccessDialogComponent, { width: '400px', data });
  }

  /**
   * Shared checkout skeleton: guard on empty cart, close the drawer, run the
   * checkout, then open the success dialog. Errors surface via the shop
   * service's `error` signal.
   */
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

  // --- Equipment cart ---

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

  // --- Vehicle cart ---

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

  // --- Sell cart ---

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

  // --- Filters ---

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

  // --- Libellés FR + résumés compacts (délégués aux helpers purs) ---

  equipmentCategoryLabel = equipmentCategoryLabel;
  unitClassLabel = unitClassLabel;
  equipmentSummary = equipmentSummary;
  equipmentBonusSummary = equipmentBonusSummary;
  equipmentClassLabel = equipmentClassLabel;
  vehicleSummary = vehicleSummary;
  saleMultiplier = saleMultiplier;
  saleValue = saleValue;

  // --- Vignettes boutique (assets statiques, fallback sur erreur) ---

  equipmentImageUrl(equipment: Equipment): string {
    return `assets/shop/equipment/${slugify(equipment.name)}.png`;
  }

  // ponytail: toLowerCase() et non slugify() — vt.name est le nom de l'énum
  // Java (ex. VTT_LEGER) dont les '_' doivent être conservés dans le nom de
  // fichier ; slugify remplacerait '_' par '-' et casserait l'URL.
  vehicleImageUrl(vt: VehicleTypeInfo): string {
    return `assets/shop/vehicles/${vt.name.toLowerCase()}.png`;
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
