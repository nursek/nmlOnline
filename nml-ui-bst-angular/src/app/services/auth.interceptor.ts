import { inject } from '@angular/core';
import {
  HttpEvent,
  HttpRequest,
  HttpErrorResponse,
  HttpInterceptorFn,
  HttpHandlerFn
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AuthActions } from '../store';
import { TokenService } from './token.service';

/**
 * Intercepteur HTTP qui gère l'authentification JWT.
 * - Ajoute le token aux requêtes si présent
 * - Gère le refresh automatique sur erreur 401
 * - Redirige vers /login si le refresh échoue
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenService = inject(TokenService);
  const router = inject(Router);
  const store = inject(Store);

  // Ne pas intercepter les requêtes d'authentification
  if (isAuthRequest(req.url)) {
    return next(req);
  }

  // Ajouter le token si présent
  const token = tokenService.getAccessToken();
  const authReq = token ? addTokenToRequest(req, token) : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // Gérer les erreurs 401 (token expiré)
      if (error.status === 401) {
        return handleUnauthorized(req, next, tokenService, router, store);
      }

      // Propager les autres erreurs
      return throwError(() => error);
    })
  );
};

/**
 * Vérifie si la requête est une requête d'authentification.
 */
function isAuthRequest(url: string): boolean {
  return url.includes('/auth/refresh') ||
         url.includes('/login') ||
         url.includes('/register') ||
         url.includes('/auth/logout');
}

/**
 * Ajoute le token d'authentification à la requête.
 */
function addTokenToRequest(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });
}

/**
 * Gère une erreur 401 en tentant un refresh du token.
 */
function handleUnauthorized(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  tokenService: TokenService,
  router: Router,
  store: Store
): Observable<HttpEvent<unknown>> {

  return tokenService.refreshToken().pipe(
    switchMap((newToken) => {
      // Réessayer la requête originale avec le nouveau token
      return next(addTokenToRequest(req, newToken));
    }),
    catchError((refreshError) => {
      // Le refresh a échoué, déconnecter l'utilisateur
      tokenService.clearAuth();
      store.dispatch(AuthActions.logoutSuccess());
      router.navigate(['/login']);
      return throwError(() => refreshError);
    })
  );
}

