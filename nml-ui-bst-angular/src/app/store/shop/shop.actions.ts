import { createActionGroup, emptyProps, props } from '@ngrx/store';
import { Equipment, CartItem, VehicleTypeInfo, Vehicle, VehicleCartItem, SellCartItem } from '../../models';

export const ShopActions = createActionGroup({
  source: 'Shop',
  events: {
    'Fetch Equipments': emptyProps(),
    'Fetch Equipments Success': props<{ equipments: Equipment[] }>(),
    'Fetch Equipments Failure': props<{ error: string }>(),
    'Load Cart': emptyProps(),
    'Load Cart Success': props<{ cart: CartItem[] }>(),
    'Add To Cart': props<{ equipment: Equipment }>(),
    'Remove From Cart': props<{ name: string }>(),
    'Update Cart Item Quantity': props<{ name: string; quantity: number }>(),
    'Clear Cart': emptyProps(),
    'Clear Shop Error': emptyProps(),
    // Véhicules — types
    'Fetch Vehicle Types': emptyProps(),
    'Fetch Vehicle Types Success': props<{ vehicleTypes: VehicleTypeInfo[] }>(),
    'Fetch Vehicle Types Failure': props<{ error: string }>(),
    // Panier véhicules
    'Add Vehicle To Cart': props<{ vehicleType: VehicleTypeInfo; quantity: number }>(),
    'Remove Vehicle From Cart': props<{ name: string }>(),
    'Update Vehicle Cart Item Quantity': props<{ name: string; quantity: number }>(),
    'Clear Vehicle Cart': emptyProps(),
    'Load Vehicle Cart': emptyProps(),
    'Load Vehicle Cart Success': props<{ cart: VehicleCartItem[] }>(),
    'Checkout Vehicles': emptyProps(),
    'Checkout Vehicles Success': props<{ vehicles: Vehicle[] }>(),
    'Checkout Vehicles Failure': props<{ error: string }>(),
    // Checkout équipements
    'Checkout Equipments': emptyProps(),
    'Checkout Equipments Success': emptyProps(),
    'Checkout Equipments Failure': props<{ error: string }>(),
    // Panier revente de ressources
    'Add To Sell Cart': props<{ resource: SellCartItem['resource']; quantity: number }>(),
    'Remove From Sell Cart': props<{ resourceId: number }>(),
    'Update Sell Cart Item Quantity': props<{ resourceId: number; quantity: number }>(),
    'Clear Sell Cart': emptyProps(),
    'Checkout Sell Cart': emptyProps(),
    'Checkout Sell Cart Success': props<{ totalValue: number }>(),
    'Checkout Sell Cart Failure': props<{ error: string }>(),
  },
});
