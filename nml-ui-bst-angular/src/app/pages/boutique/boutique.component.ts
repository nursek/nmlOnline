import { Component, inject, OnInit, OnDestroy, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Store } from '@ngrx/store';
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
import { selectUser, selectCurrentPlayer, selectEquipments, selectCart, selectCartTotalItems, selectCartTotalPrice, selectShopLoading, selectShopError, PlayerActions, ShopActions } from '../../store';
import { Equipment, CartItem, Player, EquipmentStack } from '../../models';
import { filter, take } from 'rxjs/operators';
import { toSignal } from '@angular/core/rxjs-interop';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-boutique',
  standalone: true,
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
  ],
  templateUrl: './boutique.component.html',
  styleUrls: ['./boutique.component.scss']
})
export class BoutiqueComponent implements OnInit, OnDestroy {
  private readonly store = inject(Store);
  private readonly destroy$ = new Subject<void>();

  equipments$ = this.store.select(selectEquipments);
  cart$ = this.store.select(selectCart);
  totalItems$ = this.store.select(selectCartTotalItems);
  totalPrice$ = this.store.select(selectCartTotalPrice);
  loading$ = this.store.select(selectShopLoading);
  error$ = this.store.select(selectShopError);
  player$ = this.store.select(selectCurrentPlayer);

  showCart = signal(false);
  showFilters = signal(false);

  private cartItems: CartItem[] = [];
  private playerData: Player | null = null;

  // Filtres et recherche
  searchTerm = signal('');
  selectedCategory = signal<string>('all');
  selectedBonusFilter = signal<string>('all');

  // Liste complète des équipements
  allEquipments = toSignal(this.equipments$, { initialValue: [] });

  // Catégories uniques
  categories = computed(() => {
    const cats = new Set<string>();
    this.allEquipments().forEach(eq => {
      if (eq.category) cats.add(eq.category);
    });
    return Array.from(cats).sort((a, b) => a.localeCompare(b));
  });

  // Équipements filtrés
  filteredEquipments = computed(() => {
    let filtered = [...this.allEquipments()];

    // Filtre par recherche
    const search = this.searchTerm().toLowerCase().trim();
    if (search) {
      filtered = filtered.filter(eq =>
        eq.name.toLowerCase().includes(search)
      );
    }

    // Filtre par catégorie
    const category = this.selectedCategory();
    if (category !== 'all') {
      filtered = filtered.filter(eq => eq.category === category);
    }

    // Filtre par bonus
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

  // Vérifie si des filtres sont actifs
  hasActiveFilters = computed(() => {
    return this.searchTerm() !== '' ||
      this.selectedCategory() !== 'all' ||
      this.selectedBonusFilter() !== 'all';
  });

  // Vérifie si des filtres avancés (catégorie ou bonus) sont actifs
  hasAdvancedFilters = computed(() => {
    return this.selectedCategory() !== 'all' ||
      this.selectedBonusFilter() !== 'all';
  });

  // Compte le nombre de filtres avancés actifs
  getActiveFiltersCount = computed(() => {
    let count = 0;
    if (this.selectedCategory() !== 'all') count++;
    if (this.selectedBonusFilter() !== 'all') count++;
    return count;
  });

  ngOnInit(): void {
    this.store.dispatch(ShopActions.fetchEquipments());

    this.store.select(selectUser).pipe(
      filter(user => !!user),
      take(1),
      takeUntil(this.destroy$)
    ).subscribe(user => {
      if (user) {
        this.store.dispatch(PlayerActions.fetchCurrentPlayer({ username: user.username }));
      }
    });

    this.cart$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(cart => this.cartItems = cart);

    this.player$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(player => this.playerData = player);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

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
    const stack = this.playerData?.equipments?.find((e: EquipmentStack) => e.equipment.name === equipmentName);
    return stack?.quantity || 0;
  }

  getCartQuantity(equipmentName: string): number {
    const item = this.cartItems.find(i => i.equipment.name === equipmentName);
    return item?.quantity || 0;
  }

  canAfford(): boolean {
    const total = this.cartItems.reduce((sum, item) => sum + item.equipment.cost * item.quantity, 0);
    return this.playerData !== null && this.playerData.stats.money >= total;
  }

  checkout(): void {
    // TODO: Implement real purchase logic (e.g., call backend to process the order,
    //       update player money and equipments, and clear the cart).
    alert('Fonctionnalité d\'achat en cours d\'implémentation');
  }

  // Gestion des filtres

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
