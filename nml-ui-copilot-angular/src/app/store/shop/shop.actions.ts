import { createActionGroup, emptyProps, props } from '@ngrx/store';
import { Equipment } from '../../models';

export const ShopActions = createActionGroup({
  source: 'Shop',
  events: {
    'Fetch Equipments': emptyProps(),
    'Fetch Equipments Success': props<{ equipments: Equipment[] }>(),
    'Fetch Equipments Failure': props<{ error: string }>(),
    'Add To Cart': props<{ equipment: Equipment }>(),
    'Remove From Cart': props<{ name: string }>(),
    'Update Cart Item Quantity': props<{ name: string; quantity: number }>(),
    'Clear Cart': emptyProps(),
    'Clear Shop Error': emptyProps(),
  },
});
