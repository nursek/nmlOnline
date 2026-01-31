import { createActionGroup, emptyProps, props } from '@ngrx/store';
import { Player } from '../../models';

export const PlayerActions = createActionGroup({
  source: 'Player',
  events: {
    'Fetch Current Player': props<{ username: string }>(),
    'Fetch Current Player Success': props<{ player: Player }>(),
    'Fetch Current Player Failure': props<{ error: string }>(),
    'Fetch All Players': emptyProps(),
    'Fetch All Players Success': props<{ players: Player[] }>(),
    'Fetch All Players Failure': props<{ error: string }>(),
    'Clear Player Error': emptyProps(),
    'Reset': emptyProps(), // Vider le state du player lors du logout
  },
});
