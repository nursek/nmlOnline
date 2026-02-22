import { createActionGroup, emptyProps, props } from '@ngrx/store';
import { Player } from '../../models';

export const AdminActions = createActionGroup({
  source: 'Admin',
  events: {
    'Fetch Admin Players': emptyProps(),
    'Fetch Admin Players Success': props<{ players: Player[] }>(),
    'Fetch Admin Players Failure': props<{ error: string }>(),
    'Import Player': props<{ file: File }>(),
    'Import Player Success': props<{ player: Player }>(),
    'Import Player Failure': props<{ error: string }>(),
    'Delete Player': props<{ playerId: number }>(),
    'Delete Player Success': props<{ playerId: number }>(),
    'Delete Player Failure': props<{ error: string }>(),
    'Clear Admin Messages': emptyProps(),
  },
});
