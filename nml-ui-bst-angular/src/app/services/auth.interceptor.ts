import { inject } from '@angular/core';
import {
  HttpEvent,
  HttpRequest,
  HttpErrorResponse,
  HttpInterceptorFn,
  HttpHandlerFn
} from '@angular/common/http';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { catchError, filter, switchMap, take } from 'rxjs/operators';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AuthActions } from '../store/auth/auth.actions';
import { ApiService } from './api.service';

// Note: These module-level variables are intentionally shared across all interceptor calls
// to coordinate token refresh across concurrent requests. The isRefreshing flag prevents
// multiple simultaneous refresh attempts, while refreshTokenSubject queues requests
// waiting for the new token. This is a common pattern for handling 401s with refresh tokens.
let isRefreshing = false;
let refreshTokenSubject: BehaviorSubject<string | null> = new BehaviorSubject<string | null>(null);

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('accessToken');
  const router = inject(Router);
  const store = inject(Store);
  const apiService = inject(ApiService);

  // Ajouter le token si présent
  let authReq = req;
  if (token) {
    authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !req.url.includes('/auth/refresh') && !req.url.includes('/login')) {
        return handleUnauthorized(req, next, router, store, apiService);
      }
      return throwError(() => error);
    })
  );
};

function handleUnauthorized(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  router: Router,
  store: Store,
  apiService: ApiService
): Observable<HttpEvent<unknown>> {
  if (!isRefreshing) {
    isRefreshing = true;
    refreshTokenSubject.next(null);

    return apiService.refreshToken().pipe(
      switchMap((response) => {
        isRefreshing = false;

        if (response.valid && response.token) {
          // Stocker le nouveau token
          localStorage.setItem('accessToken', response.token);

          // Restaurer le user dans le localStorage
          if (response.id && response.name) {
            const user = { id: response.id, username: response.name };
            localStorage.setItem('user', JSON.stringify(user));
          }

          refreshTokenSubject.next(response.token);

          return next(req.clone({
            setHeaders: {
              Authorization: `Bearer ${response.token}`
            }
          }));
        } else {
          // Token invalide, déconnecter
          throw new Error('Invalid refresh token');
        }
      }),
      catchError((refreshError) => {
        isRefreshing = false;
        localStorage.removeItem('accessToken');
        localStorage.removeItem('user');
        store.dispatch(AuthActions.logoutSuccess());
        router.navigate(['/login']);
        return throwError(() => refreshError);
      })
    );
  }

  return refreshTokenSubject.pipe(
    filter(token => token !== null),
    take(1),
    switchMap(token => {
      return next(req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      }));
    })
  );
}
