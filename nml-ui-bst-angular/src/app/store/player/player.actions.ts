import { createActionGroup, emptyProps, props } from '@ngrx/store';
import { Player, Vehicle } from '../../models';

export const PlayerActions = createActionGroup({
  source: 'Player',
  events: {
    'Fetch Current Player': props<{ username: string }>(),
    'Fetch Current Player Success': props<{ player: Player }>(),
    'Fetch Current Player Failure': props<{ error: string }>(),
    'Fetch All Players': emptyProps(),
    'Fetch All Players Success': props<{ players: Player[] }>(),
    'Fetch All Players Failure': props<{ error: string }>(),
    // Véhicules du joueur
    'Fetch Player Vehicles': emptyProps(),
    'Fetch Player Vehicles Success': props<{ vehicles: Vehicle[] }>(),
    'Fetch Player Vehicles Failure': props<{ error: string }>(),
    'Place Vehicle': props<{ vehicleId: number; boardId: number; sectorNumber: number }>(),
    'Place Vehicle Success': props<{ vehicle: Vehicle }>(),
    'Place Vehicle Failure': props<{ error: string }>(),
    'Clear Player Error': emptyProps(),
    'Reset': emptyProps(),
  },
});
