import { inject } from '@angular/core';
import {
  HttpEvent,
  HttpRequest,
  HttpErrorResponse,
  HttpInterceptorFn,
  HttpHandlerFn,
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { TokenService } from './token.service';
import { AuthService } from './auth.service';

/**
 * Intercepteur HTTP qui gère l'authentification JWT.
 * - Ajoute le token aux requêtes si présent
 * - Gère le refresh automatique sur erreur 401
 * - Propage les 403 comme des erreurs d'autorisation (pas de refresh)
 * - Redirige vers /login si le refresh échoue
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenService = inject(TokenService);
  const authService = inject(AuthService);
  const router = inject(Router);

  // Ne pas intercepter les requêtes d'authentification de base
  if (isAuthRequest(req.url)) {
    return next(req);
  }

  // Ajouter le token si présent
  const token = tokenService.getAccessToken();
  const authReq = token ? addTokenToRequest(req, token) : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // 401 = token expiré/absent : tenter un refresh une seule fois
      if (error.status === 401) {
        return handleUnauthorized(req, next, tokenService, authService, router);
      }

      // 403 = autorisations insuffisantes : ne PAS tenter de refresh
      if (error.status === 403) {
        authService.reportForbidden(error.error?.message || 'Accès refusé');
      }

      // Propager les autres erreurs
      return throwError(() => error);
    }),
  );
};

/**
 * Vérifie si la requête est une requête d'authentification de base.
 * /auth/logout garde le token attaché si le backend en a besoin.
 */
function isAuthRequest(url: string): boolean {
  return url.includes('/auth/refresh') || url.includes('/login') || url.includes('/register');
}

/**
 * Ajoute le token d'authentification à la requête.
 */
function addTokenToRequest(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`,
    },
  });
}

const RETRY_HEADER = 'X-Retry-After-Refresh';

/**
 * Gère une erreur 401 en tentant un refresh du token.
 * Utilise un header personnalisé pour éviter les boucles infinies.
 */
function handleUnauthorized(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  tokenService: TokenService,
  authService: AuthService,
  router: Router,
): Observable<HttpEvent<unknown>> {
  // Si la requête a déjà été retentée après un refresh, ne pas boucler
  if (req.headers.has(RETRY_HEADER)) {
    authService.clear();
    void router.navigate(['/login']);
    return throwError(() => new Error('Token refresh failed after retry'));
  }

  return tokenService.refreshToken().pipe(
    switchMap((newToken) => {
      // Marquer la requête comme déjà retentée après refresh
      const retriedReq = req.clone({
        setHeaders: { [RETRY_HEADER]: '1' },
      });
      return next(addTokenToRequest(retriedReq, newToken));
    }),
    catchError((refreshError) => {
      // Le refresh a échoué, déconnecter l'utilisateur
      authService.clear();
      void router.navigate(['/login']);
      return throwError(() => refreshError);
    }),
  );
}
