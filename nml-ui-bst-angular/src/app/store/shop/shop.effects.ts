import { Injectable, inject } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { map, exhaustMap, catchError } from 'rxjs/operators';
import { ApiService } from '../../services/api.service';
import { ShopActions } from './shop.actions';

@Injectable()
export class ShopEffects {
  private actions$ = inject(Actions);
  private apiService = inject(ApiService);

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
}
