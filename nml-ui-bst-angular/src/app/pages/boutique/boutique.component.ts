import { Component, inject, OnInit, OnDestroy, signal, computed, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Store } from '@ngrx/store';
import { Actions, ofType } from '@ngrx/effects';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatBadgeModule } from '@angular/material/badge';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import {
  selectUser, selectCurrentPlayer, selectEquipments, selectCart, selectCartTotalItems,
  selectCartTotalPrice, selectShopLoading, selectShopError, selectVehicleTypes,
  selectPurchaseLoading, selectVehicleCart, selectVehicleCartTotalItems,
  selectVehicleCartTotalPrice, selectSellCart, selectSellCartTotalValue,
  PlayerActions, ShopActions
} from '../../store';
import {
  Equipment, CartItem, EquipmentStack, VehicleTypeInfo, VehicleCartItem, SellCartItem, PlayerResource
} from '../../models';
import { filter, take, takeUntil } from 'rxjs/operators';
import { toSignal } from '@angular/core/rxjs-interop';
import { Subject } from 'rxjs';
import {
  PurchaseSuccessDialogComponent,
  PurchaseSuccessData
} from '../../shared/purchase-success-dialog/purchase-success-dialog.component';

@Component({
  selector: 'app-boutique',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatChipsModule,
    MatButtonModule,
    MatBadgeModule,
    MatSidenavModule,
    MatDividerModule,
    MatFormFieldModule,
    MatInputModule,
    MatTabsModule,
    MatTooltipModule,
    MatDialogModule,
  ],
  templateUrl: './boutique.component.html',
  styleUrls: ['./boutique.component.scss']
})
export class BoutiqueComponent implements OnInit, OnDestroy {
  private readonly store = inject(Store);
  private readonly actions$ = inject(Actions);
  private readonly dialog = inject(MatDialog);
  private readonly destroy$ = new Subject<void>();

  // Snapshots capturés avant checkout (pour la pop-up de confirmation)
  private _pendingEquipCart: CartItem[] = [];
  private _pendingVehicleCart: VehicleCartItem[] = [];
  private _pendingSellCart: SellCartItem[] = [];

  equipments$ = this.store.select(selectEquipments);
  private readonly cart$ = this.store.select(selectCart);
  loading$ = this.store.select(selectShopLoading);
  player$ = this.store.select(selectCurrentPlayer);

  // Signals équipements
  readonly cart = toSignal(this.cart$, { initialValue: [] as CartItem[] });
  readonly totalItems = toSignal(this.store.select(selectCartTotalItems), { initialValue: 0 });
  readonly totalPrice = toSignal(this.store.select(selectCartTotalPrice), { initialValue: 0 });
  readonly loading = toSignal(this.loading$, { initialValue: false });
  readonly error = toSignal(this.store.select(selectShopError));
  readonly player = toSignal(this.player$);
  readonly vehicleTypes = toSignal(this.store.select(selectVehicleTypes), { initialValue: [] as VehicleTypeInfo[] });
  readonly purchaseLoading = toSignal(this.store.select(selectPurchaseLoading), { initialValue: false });

  // Panier véhicules
  readonly vehicleCart = toSignal(this.store.select(selectVehicleCart), { initialValue: [] as VehicleCartItem[] });
  readonly vehicleCartTotalItems = toSignal(this.store.select(selectVehicleCartTotalItems), { initialValue: 0 });
  readonly vehicleCartTotalPrice = toSignal(this.store.select(selectVehicleCartTotalPrice), { initialValue: 0 });

  // Panier revente
  readonly sellCart = toSignal(this.store.select(selectSellCart), { initialValue: [] as SellCartItem[] });
  readonly sellCartTotalValue = toSignal(this.store.select(selectSellCartTotalValue), { initialValue: 0 });

  // Quantités de vente (clé = resource.id)
  resourceSellQuantities = signal<Record<number, number>>({});

  showCart = signal(false);
  showFilters = signal(false);

  // Quantités dans la fiche des véhicules avant ajout au panier (clé = vehicleType.name)
  vehicleQuantities = signal<Record<string, number>>({});

  // Filtres et recherche
  searchTerm = signal('');
  selectedCategory = signal<string>('all');
  selectedBonusFilter = signal<string>('all');

  allEquipments = toSignal(this.equipments$, { initialValue: [] });

  categories = computed(() => {
    const cats = new Set<string>();
    this.allEquipments().forEach(eq => {
      if (eq.category) cats.add(eq.category);
    });
    return Array.from(cats).sort((a, b) => a.localeCompare(b));
  });

  filteredEquipments = computed(() => {
    let filtered = [...this.allEquipments()];

    const search = this.searchTerm().toLowerCase().trim();
    if (search) {
      filtered = filtered.filter(eq => eq.name.toLowerCase().includes(search));
    }

    const category = this.selectedCategory();
    if (category !== 'all') {
      filtered = filtered.filter(eq => eq.category === category);
    }

    const bonus = this.selectedBonusFilter();
    if (bonus !== 'all') {
      filtered = filtered.filter(eq => {
        switch (bonus) {
          case 'PDF': return eq.pdfBonus > 0;
          case 'PDC': return eq.pdcBonus > 0;
          case 'ARM': return eq.armBonus > 0;
          case 'ESQ': return eq.evasionBonus > 0;
          default: return true;
        }
      });
    }

    return filtered;
  });

  // Badge global du bouton panier (équipements + véhicules)
  readonly totalCartBadge = computed(() => this.totalItems() + this.vehicleCartTotalItems());

  hasActiveFilters = computed(() =>
    this.searchTerm() !== '' || this.selectedCategory() !== 'all' || this.selectedBonusFilter() !== 'all'
  );

  hasAdvancedFilters = computed(() =>
    this.selectedCategory() !== 'all' || this.selectedBonusFilter() !== 'all'
  );

  getActiveFiltersCount = computed(() => {
    let count = 0;
    if (this.selectedCategory() !== 'all') count++;
    if (this.selectedBonusFilter() !== 'all') count++;
    return count;
  });

  ngOnInit(): void {
    this.store.dispatch(ShopActions.fetchEquipments());
    this.store.dispatch(ShopActions.fetchVehicleTypes());
    this.store.dispatch(ShopActions.loadVehicleCart());

    this.store.select(selectUser).pipe(
      filter(user => !!user),
      take(1),
      takeUntil(this.destroy$)
    ).subscribe(user => {
      if (user) {
        this.store.dispatch(PlayerActions.fetchCurrentPlayer({ username: user.username }));
      }
    });

    // Pop-up succés après checkout équipements
    this.actions$.pipe(
      ofType(ShopActions.checkoutEquipmentsSuccess),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      const snapshot = this._pendingEquipCart;
      if (!snapshot.length) return;
      const lines = snapshot.map(item => `${item.quantity} × ${item.equipment.name}`);
      const totalCost = snapshot.reduce((s, i) => s + i.equipment.cost * i.quantity, 0);
      this.openSuccessDialog({ title: 'Équipements achetés !', lines, totalCost });
    });

    // Pop-up succés après checkout véhicules
    this.actions$.pipe(
      ofType(ShopActions.checkoutVehiclesSuccess),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      const snapshot = this._pendingVehicleCart;
      if (!snapshot.length) return;
      const lines = snapshot.map(item => `${item.quantity} × ${item.vehicleType.displayName}`);
      const totalCost = snapshot.reduce((s, i) => s + i.vehicleType.cost * i.quantity, 0);
      this.openSuccessDialog({ title: 'Véhicules achetés !', lines, totalCost });
    });

    // Pop-up succés après checkout revente
    this.actions$.pipe(
      ofType(ShopActions.checkoutSellCartSuccess),
      takeUntil(this.destroy$)
    ).subscribe(({ totalValue }) => {
      const snapshot = this._pendingSellCart;
      if (!snapshot.length) return;
      const lines = snapshot.map(item => `${item.quantity} × ${item.resource.name}`);
      this.openSuccessDialog({ title: 'Ressources vendues !', lines, totalCost: totalValue, isSale: true });
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private openSuccessDialog(data: PurchaseSuccessData): void {
    this.dialog.open(PurchaseSuccessDialogComponent, { width: '400px', data });
  }

  // Panier équipements

  toggleCart(): void {
    this.showCart.update(v => !v);
  }

  addToCart(equipment: Equipment): void {
    this.store.dispatch(ShopActions.addToCart({ equipment }));
  }

  removeFromCart(name: string): void {
    this.store.dispatch(ShopActions.removeFromCart({ name }));
  }

  updateQuantity(name: string, quantity: number): void {
    this.store.dispatch(ShopActions.updateCartItemQuantity({ name, quantity }));
  }

  decrementCartQuantity(name: string): void {
    const currentQty = this.getCartQuantity(name);
    if (currentQty > 1) {
      this.store.dispatch(ShopActions.updateCartItemQuantity({ name, quantity: currentQty - 1 }));
    } else {
      this.store.dispatch(ShopActions.removeFromCart({ name }));
    }
  }

  clearCart(): void {
    this.store.dispatch(ShopActions.clearCart());
  }

  getOwnedQuantity(equipmentName: string): number {
    const stack = this.player()?.equipments?.find((e: EquipmentStack) => e.equipment.name === equipmentName);
    return stack?.quantity || 0;
  }

  getCartQuantity(equipmentName: string): number {
    const item = this.cart().find(i => i.equipment.name === equipmentName);
    return item?.quantity || 0;
  }

  readonly canAfford = computed(() => (this.player()?.stats?.money ?? 0) >= this.totalPrice());

  checkout(): void {
    this._pendingEquipCart = [...this.cart()];
    this.store.dispatch(ShopActions.checkoutEquipments());
    this.showCart.set(false);
  }

  // Panier véhicules

  addVehicleToCart(vehicleType: VehicleTypeInfo): void {
    const qty = this.vehicleQuantities()[vehicleType.name] ?? 1;
    this.store.dispatch(ShopActions.addVehicleToCart({ vehicleType, quantity: qty }));
  }

  removeVehicleFromCart(name: string): void {
    this.store.dispatch(ShopActions.removeVehicleFromCart({ name }));
  }

  decrementVehicleCartQuantity(name: string): void {
    const current = this.getVehicleCartQuantity(name);
    if (current > 1) {
      this.store.dispatch(ShopActions.updateVehicleCartItemQuantity({ name, quantity: current - 1 }));
    } else {
      this.store.dispatch(ShopActions.removeVehicleFromCart({ name }));
    }
  }

  getVehicleCartQuantity(name: string): number {
    return this.vehicleCart().find(i => i.vehicleType.name === name)?.quantity ?? 0;
  }

  checkoutVehicles(): void {
    this._pendingVehicleCart = [...this.vehicleCart()];
    this.store.dispatch(ShopActions.checkoutVehicles());
    this.showCart.set(false);
  }

  readonly canAffordVehicleCart = computed(
    () => (this.player()?.stats?.money ?? 0) >= this.vehicleCartTotalPrice()
  );

  getVehicleQuantity(vehicleTypeName: string): number {
    return this.vehicleQuantities()[vehicleTypeName] ?? 1;
  }

  setVehicleQuantity(vehicleTypeName: string, qty: number): void {
    this.vehicleQuantities.update((prev) => ({ ...prev, [vehicleTypeName]: Math.max(1, qty) }));
  }

  canAffordVehicle(vehicleCost: number, qty: number = 1): boolean {
    return (this.player()?.stats?.money ?? 0) >= vehicleCost * qty;
  }

  // Panier revente de ressources

  getSellQty(resource: PlayerResource): number {
    return this.resourceSellQuantities()[resource.id ?? 0] ?? 1;
  }

  setSellQty(resource: PlayerResource, qty: number): void {
    const clamped = Math.max(1, Math.min(qty, resource.quantity));
    this.resourceSellQuantities.update(prev => ({ ...prev, [resource.id ?? 0]: clamped }));
  }

  addToSellCart(resource: PlayerResource): void {
    const qty = this.getSellQty(resource);
    this.store.dispatch(ShopActions.addToSellCart({ resource, quantity: qty }));
  }

  removeFromSellCart(resourceId: number): void {
    this.store.dispatch(ShopActions.removeFromSellCart({ resourceId }));
  }

  getSellCartQuantity(resourceId: number): number {
    return this.sellCart().find(i => i.resource.id === resourceId)?.quantity ?? 0;
  }

  isInSellCart(resourceId: number): boolean {
    return this.sellCart().some(i => i.resource.id === resourceId);
  }

  checkoutSellCart(): void {
    this._pendingSellCart = [...this.sellCart()];
    this.store.dispatch(ShopActions.checkoutSellCart());
    this.showCart.set(false);
  }

  clearVehicleCart(): void {
    this.store.dispatch(ShopActions.clearVehicleCart());
  }

  clearSellCartAll(): void {
    this.store.dispatch(ShopActions.clearSellCart());
  }

  // Filtres

  clearSearch(): void {
    this.searchTerm.set('');
  }

  toggleFilters(): void {
    this.showFilters.update(v => !v);
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
}
