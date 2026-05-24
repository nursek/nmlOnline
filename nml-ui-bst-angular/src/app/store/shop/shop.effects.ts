import { Injectable, inject } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { Store } from '@ngrx/store';
import { from, of } from 'rxjs';
import { map, exhaustMap, switchMap, catchError, tap, withLatestFrom, concatMap, toArray } from 'rxjs/operators';
import { ApiService } from '../../services/api.service';
import { ShopActions } from './shop.actions';
import { selectCart, selectVehicleCart, selectSellCart } from './shop.selectors';
import { selectUser } from '../auth/auth.selectors';
import { PlayerActions } from '../player/player.actions';
import { CartItem, BuyEquipmentItem, Vehicle } from '../../models';

@Injectable()
export class ShopEffects {
  private readonly actions$ = inject(Actions);
  private readonly apiService = inject(ApiService);
  private readonly store = inject(Store);

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

  // Charger le panier d'équipements depuis localStorage au démarrage
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

  // Charger le panier véhicules depuis localStorage au démarrage
  loadVehicleCart$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ShopActions.loadVehicleCart),
      map(() => {
        try {
          const stored = localStorage.getItem('vehicleCart');
          if (stored) {
            const parsed = JSON.parse(stored);
            if (Array.isArray(parsed)) {
              return ShopActions.loadVehicleCartSuccess({ cart: parsed });
            }
          }
        } catch {
          localStorage.removeItem('vehicleCart');
        }
        return ShopActions.loadVehicleCartSuccess({ cart: [] });
      })
    )
  );

  // Persister le panier équipements dans localStorage
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

  // Persister le panier véhicules dans localStorage
  persistVehicleCart$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(
          ShopActions.addVehicleToCart,
          ShopActions.removeVehicleFromCart,
          ShopActions.updateVehicleCartItemQuantity,
          ShopActions.clearVehicleCart
        ),
        withLatestFrom(this.store.select(selectVehicleCart)),
        tap(([, cart]) => {
          if (cart.length > 0) {
            localStorage.setItem('vehicleCart', JSON.stringify(cart));
          } else {
            localStorage.removeItem('vehicleCart');
          }
        })
      ),
    { dispatch: false }
  );

  // Charger les types de véhicules depuis le backend
  fetchVehicleTypes$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ShopActions.fetchVehicleTypes),
      exhaustMap(() =>
        this.apiService.getVehicleTypes().pipe(
          map((vehicleTypes) => ShopActions.fetchVehicleTypesSuccess({ vehicleTypes })),
          catchError((error) =>
            of(ShopActions.fetchVehicleTypesFailure({
              error: error.error?.message || error.message || 'Erreur lors du chargement des véhicules'
            }))
          )
        )
      )
    )
  );

  // Checkout du panier véhicules — achète chaque type en séquentiel
  checkoutVehicles$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ShopActions.checkoutVehicles),
      withLatestFrom(this.store.select(selectVehicleCart), this.store.select(selectUser)),
      exhaustMap(([, cart, user]) =>
        from(cart).pipe(
          concatMap((item) => this.apiService.buyVehicle(item.vehicleType.name, item.quantity)),
          toArray(),
          map((results: Vehicle[][]) => results.flat()),
          switchMap((vehicles) => [
            ShopActions.checkoutVehiclesSuccess({ vehicles }),
            ShopActions.clearVehicleCart(),
            ...(user
              ? [
                  PlayerActions.fetchCurrentPlayer({ username: user.username }),
                  PlayerActions.fetchPlayerVehicles(),
                ]
              : []),
          ]),
          catchError((error) => {
            const status = error.status;
            const message =
              status === 402
                ? 'Fonds insuffisants pour acheter ces véhicules'
                : error.error?.message || error.message || "Erreur lors de l'achat des véhicules";
            return of(ShopActions.checkoutVehiclesFailure({ error: message }));
          })
        )
      )
    )
  );

  // Checkout du panier d'équipements
  checkoutEquipments$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ShopActions.checkoutEquipments),
      withLatestFrom(this.store.select(selectCart), this.store.select(selectUser)),
      exhaustMap(([, cart, user]) => {
        const items: BuyEquipmentItem[] = cart.map((item: CartItem) => ({
          name: item.equipment.name,
          quantity: item.quantity,
        }));
        return this.apiService.buyEquipments(items).pipe(
          switchMap(() => [
            ShopActions.checkoutEquipmentsSuccess(),
            ShopActions.clearCart(),
            ...(user ? [PlayerActions.fetchCurrentPlayer({ username: user.username })] : []),
          ]),
          catchError((error) => {
            const status = error.status;
            const message =
              status === 402
                ? 'Fonds insuffisants pour finaliser la commande'
                : error.error?.message || error.message || "Erreur lors de l'achat des équipements";
            return of(ShopActions.checkoutEquipmentsFailure({ error: message }));
          })
        );
      })
    )
  );

  // Checkout du panier de revente de ressources — vend chaque item en séquentiel
  checkoutSellCart$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ShopActions.checkoutSellCart),
      withLatestFrom(this.store.select(selectSellCart), this.store.select(selectUser)),
      exhaustMap(([, cart, user]) =>
        from(cart).pipe(
          concatMap((item) => this.apiService.sellResource(item.resource.id!, item.quantity)),
          toArray(),
          map((results) => results.reduce((sum, r) => sum + r.saleValue, 0)),
          switchMap((totalValue) => [
            ShopActions.checkoutSellCartSuccess({ totalValue }),
            ShopActions.clearSellCart(),
            ...(user ? [PlayerActions.fetchCurrentPlayer({ username: user.username })] : []),
          ]),
          catchError((error) => {
            const message =
              error.error?.message || error.message || 'Erreur lors de la vente des ressources';
            return of(ShopActions.checkoutSellCartFailure({ error: message }));
          })
        )
      )
    )
  );
}
