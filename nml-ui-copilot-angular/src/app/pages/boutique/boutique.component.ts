import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Store } from '@ngrx/store';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatBadgeModule } from '@angular/material/badge';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatDividerModule } from '@angular/material/divider';
import { ShopActions } from '../../store/shop/shop.actions';
import { PlayerActions } from '../../store/player/player.actions';
import { selectEquipments, selectCart, selectCartTotalItems, selectCartTotalPrice, selectShopLoading, selectShopError } from '../../store/shop/shop.selectors';
import { selectCurrentPlayer } from '../../store/player/player.selectors';
import { selectUser } from '../../store/auth/auth.selectors';
import { Equipment } from '../../models';
import { filter, take } from 'rxjs/operators';

@Component({
  selector: 'app-boutique',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatChipsModule,
    MatButtonModule,
    MatBadgeModule,
    MatSidenavModule,
    MatDividerModule,
  ],
  template: `
    @if ((loading$ | async) && (equipments$ | async)?.length === 0) {
      <div class="loading-container">
        <mat-spinner diameter="60"></mat-spinner>
      </div>
    } @else {
      <div class="container fade-in">
        <!-- Header -->
        <div class="page-header">
          <div class="header-left">
            <div class="avatar">
              <mat-icon>shopping_bag</mat-icon>
            </div>
            <div>
              <h1 class="title">Boutique d'Équipements</h1>
              <p class="subtitle">Équipez vos troupes pour la victoire</p>
            </div>
          </div>

          <div class="header-right">
            <div class="money-display">
              <span class="money-label">Argent disponible:</span>
              <span class="money-value">{{ (player$ | async)?.stats?.money | number:'1.0-0' }} ₡</span>
            </div>
            <button mat-raised-button
                    [color]="showCart() ? 'primary' : 'basic'"
                    [matBadge]="(totalItems$ | async) || 0"
                    matBadgeColor="accent"
                    (click)="toggleCart()">
              <mat-icon>shopping_cart</mat-icon>
              Panier ({{ (cart$ | async)?.length || 0 }})
            </button>
          </div>
        </div>

        @if (error$ | async; as error) {
          <div class="error-alert">
            <mat-icon>error</mat-icon>
            {{ error }}
          </div>
        }

        <!-- Liste des équipements -->
        <h2 class="section-title">Équipements disponibles</h2>
        <div class="equipment-grid">
          @for (equipment of equipments$ | async; track equipment.name) {
            <mat-card class="equipment-card hover-lift">
              <mat-card-content>
                <div class="equipment-header">
                  <h3>{{ equipment.name }}</h3>
                  <mat-chip color="warn" highlighted>{{ equipment.cost }} ₡</mat-chip>
                </div>
                <span class="category">{{ equipment.category }}</span>

                <!-- Bonus -->
                <div class="bonus-chips">
                  @if (equipment.pdfBonus > 0) {
                    <mat-chip class="bonus pdf">+{{ equipment.pdfBonus }} PDF</mat-chip>
                  }
                  @if (equipment.pdcBonus > 0) {
                    <mat-chip class="bonus pdc">+{{ equipment.pdcBonus }} PDC</mat-chip>
                  }
                  @if (equipment.armBonus > 0) {
                    <mat-chip class="bonus arm">+{{ equipment.armBonus }} ARM</mat-chip>
                  }
                  @if (equipment.evasionBonus > 0) {
                    <mat-chip class="bonus esq">+{{ equipment.evasionBonus }} ESQ</mat-chip>
                  }
                </div>

                <!-- Classes compatibles -->
                <div class="compatible-classes">
                  <span class="label">Compatible avec:</span>
                  <div class="class-chips">
                    @if (equipment.compatibleClass && equipment.compatibleClass.length > 0) {
                      @for (unitClass of equipment.compatibleClass; track unitClass.code) {
                        <mat-chip>{{ unitClass.name }}</mat-chip>
                      }
                    } @else {
                      <span class="none">Aucune</span>
                    }
                  </div>
                </div>

                <!-- Quantité possédée -->
                @if (getOwnedQuantity(equipment.name) > 0) {
                  <p class="owned">Vous en possédez: <strong>{{ getOwnedQuantity(equipment.name) }}</strong></p>
                }

                <!-- Bouton d'ajout -->
                <button mat-raised-button color="primary" class="add-btn" (click)="addToCart(equipment)">
                  <mat-icon>add</mat-icon>
                  Ajouter au panier
                  @if (getCartQuantity(equipment.name) > 0) {
                    ({{ getCartQuantity(equipment.name) }})
                  }
                </button>
              </mat-card-content>
            </mat-card>
          }
        </div>
      </div>

      <!-- Cart Drawer -->
      <mat-sidenav-container class="cart-container">
        <mat-sidenav #cartDrawer mode="over" position="end" [opened]="showCart()" (closed)="showCart.set(false)">
          <div class="cart-content">
            <h2>Panier</h2>
            <mat-divider></mat-divider>

            @if ((cart$ | async)?.length === 0) {
              <div class="empty-cart">
                <mat-icon>shopping_cart</mat-icon>
                <p>Votre panier est vide</p>
              </div>
            } @else {
              <div class="cart-items">
                @for (item of cart$ | async; track item.equipment.name) {
                  <div class="cart-item">
                    <div class="item-header">
                      <h4>{{ item.equipment.name }}</h4>
                      <button mat-icon-button color="warn" (click)="removeFromCart(item.equipment.name)">
                        <mat-icon>delete</mat-icon>
                      </button>
                    </div>
                    <p class="item-price">{{ item.equipment.cost }} ₡ × {{ item.quantity }} = {{ item.equipment.cost * item.quantity }} ₡</p>
                    <div class="quantity-controls">
                      <button mat-icon-button [disabled]="item.quantity <= 1" (click)="updateQuantity(item.equipment.name, item.quantity - 1)">
                        <mat-icon>remove</mat-icon>
                      </button>
                      <span class="quantity">{{ item.quantity }}</span>
                      <button mat-icon-button (click)="updateQuantity(item.equipment.name, item.quantity + 1)">
                        <mat-icon>add</mat-icon>
                      </button>
                    </div>
                  </div>
                }
              </div>

              <mat-divider></mat-divider>

              <div class="cart-total">
                <div class="total-row">
                  <span>Total:</span>
                  <span class="total-value">{{ totalPrice$ | async }} ₡</span>
                </div>
                <div class="total-row">
                  <span>Argent disponible:</span>
                  <span>{{ (player$ | async)?.stats?.money | number:'1.0-0' }} ₡</span>
                </div>
              </div>

              <button mat-raised-button
                      [color]="canAfford() ? 'primary' : 'warn'"
                      [disabled]="!canAfford()"
                      class="checkout-btn"
                      (click)="checkout()">
                <mat-icon>attach_money</mat-icon>
                {{ canAfford() ? 'Acheter' : 'Fonds insuffisants' }}
              </button>

              <button mat-stroked-button color="warn" class="clear-btn" (click)="clearCart()">
                Vider le panier
              </button>
            }
          </div>
        </mat-sidenav>
      </mat-sidenav-container>
    }
  `,
  styles: [`
    .loading-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 80vh;
    }

    .container {
      max-width: 1400px;
      margin: 0 auto;
      padding: 32px 16px;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 16px;
      margin-bottom: 32px;
    }

    .header-left {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .avatar {
      width: 64px;
      height: 64px;
      background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;

      mat-icon {
        font-size: 40px;
        width: 40px;
        height: 40px;
        color: white;
      }
    }

    .title {
      font-size: 1.75rem;
      font-weight: 700;
      background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
      margin: 0;
    }

    .subtitle {
      color: #64748b;
      margin: 4px 0 0;
    }

    .money-display {
      padding: 8px 16px;
      background: rgba(245, 158, 11, 0.1);
      border: 1px solid #f59e0b;
      border-radius: 8px;

      .money-label {
        font-size: 0.75rem;
        color: #64748b;
      }

      .money-value {
        margin-left: 8px;
        font-weight: 700;
        color: #f59e0b;
      }
    }

    .section-title {
      font-size: 1.25rem;
      font-weight: 600;
      margin: 0 0 16px;
    }

    .equipment-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 24px;
    }

    .equipment-card {
      border-radius: 12px;
    }

    .equipment-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 8px;

      h3 {
        margin: 0;
        font-weight: 600;
      }
    }

    .category {
      font-size: 0.75rem;
      color: #64748b;
      display: block;
      margin-bottom: 12px;
    }

    .bonus-chips {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;
      margin-bottom: 12px;

      .bonus {
        font-size: 0.7rem !important;
        min-height: 24px !important;

        &.pdf { background: #dc2626 !important; color: white !important; }
        &.pdc { background: #0891b2 !important; color: white !important; }
        &.arm { background: #059669 !important; color: white !important; }
        &.esq { background: #d97706 !important; color: white !important; }
      }
    }

    .compatible-classes {
      margin-bottom: 12px;

      .label {
        font-size: 0.75rem;
        color: #64748b;
        display: block;
        margin-bottom: 4px;
      }

      .class-chips {
        display: flex;
        flex-wrap: wrap;
        gap: 4px;
      }

      .none {
        font-size: 0.75rem;
        color: #94a3b8;
      }
    }

    .owned {
      font-size: 0.875rem;
      color: #64748b;
      margin-bottom: 12px;

      strong {
        color: #6366f1;
      }
    }

    .add-btn {
      width: 100%;
    }

    .cart-container {
      position: fixed;
      top: 0;
      right: 0;
      bottom: 0;
      width: 0;
    }

    mat-sidenav {
      width: 400px;
      max-width: 100vw;
    }

    .cart-content {
      padding: 24px;

      h2 {
        margin: 0 0 16px;
        font-weight: 600;
      }
    }

    .empty-cart {
      text-align: center;
      padding: 64px 16px;

      mat-icon {
        font-size: 64px;
        width: 64px;
        height: 64px;
        color: #94a3b8;
      }

      p {
        color: #64748b;
      }
    }

    .cart-items {
      padding: 16px 0;
    }

    .cart-item {
      padding: 12px;
      background: #f8fafc;
      border-radius: 8px;
      margin-bottom: 12px;

      .item-header {
        display: flex;
        justify-content: space-between;
        align-items: center;

        h4 {
          margin: 0;
          font-weight: 600;
        }
      }

      .item-price {
        font-size: 0.75rem;
        color: #64748b;
        margin: 4px 0 8px;
      }

      .quantity-controls {
        display: flex;
        align-items: center;
        gap: 8px;

        .quantity {
          min-width: 30px;
          text-align: center;
          font-weight: 600;
        }
      }
    }

    .cart-total {
      padding: 16px 0;

      .total-row {
        display: flex;
        justify-content: space-between;
        margin-bottom: 8px;

        &:first-child {
          font-size: 1.1rem;
        }

        .total-value {
          font-weight: 700;
          color: #f59e0b;
        }
      }
    }

    .checkout-btn {
      width: 100%;
      margin-bottom: 8px;
    }

    .clear-btn {
      width: 100%;
    }

    .error-alert {
      display: flex;
      align-items: center;
      gap: 8px;
      background: #fef2f2;
      color: #dc2626;
      padding: 16px;
      border-radius: 8px;
      border: 1px solid #fecaca;
      margin-bottom: 24px;
    }

    .fade-in {
      animation: fadeIn 0.3s ease;
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(10px); }
      to { opacity: 1; transform: translateY(0); }
    }

    .hover-lift {
      transition: transform 0.3s, box-shadow 0.3s;

      &:hover {
        transform: translateY(-4px);
        box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
      }
    }
  `]
})
export class BoutiqueComponent implements OnInit {
  private store = inject(Store);

  equipments$ = this.store.select(selectEquipments);
  cart$ = this.store.select(selectCart);
  totalItems$ = this.store.select(selectCartTotalItems);
  totalPrice$ = this.store.select(selectCartTotalPrice);
  loading$ = this.store.select(selectShopLoading);
  error$ = this.store.select(selectShopError);
  player$ = this.store.select(selectCurrentPlayer);

  showCart = signal(false);

  private cartItems: any[] = [];
  private playerData: any = null;

  ngOnInit(): void {
    this.store.dispatch(ShopActions.fetchEquipments());

    this.store.select(selectUser).pipe(
      filter(user => !!user),
      take(1)
    ).subscribe(user => {
      if (user) {
        this.store.dispatch(PlayerActions.fetchCurrentPlayer({ username: user.username }));
      }
    });

    this.cart$.subscribe(cart => this.cartItems = cart);
    this.player$.subscribe(player => this.playerData = player);
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

  clearCart(): void {
    this.store.dispatch(ShopActions.clearCart());
  }

  getOwnedQuantity(equipmentName: string): number {
    const stack = this.playerData?.equipments?.find((e: any) => e.equipment.name === equipmentName);
    return stack?.quantity || 0;
  }

  getCartQuantity(equipmentName: string): number {
    const item = this.cartItems.find(i => i.equipment.name === equipmentName);
    return item?.quantity || 0;
  }

  canAfford(): boolean {
    const total = this.cartItems.reduce((sum, item) => sum + item.equipment.cost * item.quantity, 0);
    return this.playerData && this.playerData.stats.money >= total;
  }

  checkout(): void {
    alert('Fonctionnalité d\'achat en cours d\'implémentation');
  }
}
