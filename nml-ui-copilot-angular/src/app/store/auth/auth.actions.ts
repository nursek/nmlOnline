import { createActionGroup, emptyProps, props } from '@ngrx/store';
import { AuthResponse, LoginRequest, RefreshResponse, User } from '../../models';

export const AuthActions = createActionGroup({
  source: 'Auth',
  events: {
    'Login': props<{ credentials: LoginRequest }>(),
    'Login Success': props<{ response: AuthResponse }>(),
    'Login Failure': props<{ error: string }>(),
    'Logout': emptyProps(),
    'Logout Success': emptyProps(),
    'Clear Error': emptyProps(),
    'Load User From Storage': emptyProps(),
    // Refresh token au démarrage
    'Init Session': emptyProps(),
    'Init Session Success': props<{ token: string; id: number; username: string }>(),
    'Init Session Failure': emptyProps(),
  },
});
