import { Injectable, inject } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { Store } from '@ngrx/store';
import { of } from 'rxjs';
import { map, exhaustMap, catchError, tap, withLatestFrom } from 'rxjs/operators';
import { ApiService } from '../../services/api.service';
import { ShopActions } from './shop.actions';
import { selectCart } from './shop.selectors';
import { CartItem } from '../../models';

@Injectable()
export class ShopEffects {
  private actions$ = inject(Actions);
  private apiService = inject(ApiService);
  private store = inject(Store);

  fetchEquipments$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ShopActions.fetchEquipments),
      exhaustMap(() =>
        this.apiService.getEquipments().pipe(
          map((equipments) => ShopActions.fetchEquipmentsSuccess({ equipments })),
          catchError((error) =>
            of(ShopActions.fetchEquipmentsFailure({
              error: error.error?.message || error.message || 'Erreur lors de la récupération des équipements'
            }))
          )
        )
      )
    )
  );

  // Charger le panier depuis localStorage au démarrage
  loadCart$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ShopActions.loadCart),
      map(() => {
        try {
          const storedCart = localStorage.getItem('cart');
          if (storedCart) {
            const parsed = JSON.parse(storedCart);
            if (Array.isArray(parsed)) {
              return ShopActions.loadCartSuccess({ cart: parsed as CartItem[] });
            }
          }
        } catch {
          localStorage.removeItem('cart');
        }
        return ShopActions.loadCartSuccess({ cart: [] });
      })
    )
  );

  // Persister le panier dans localStorage après chaque modification
  persistCart$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(
          ShopActions.addToCart,
          ShopActions.removeFromCart,
          ShopActions.updateCartItemQuantity,
          ShopActions.clearCart
        ),
        withLatestFrom(this.store.select(selectCart)),
        tap(([, cart]) => {
          if (cart.length > 0) {
            localStorage.setItem('cart', JSON.stringify(cart));
          } else {
            localStorage.removeItem('cart');
          }
        })
      ),
    { dispatch: false }
  );
}
