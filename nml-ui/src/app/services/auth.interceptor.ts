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

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenService = inject(TokenService);
  const authService = inject(AuthService);
  const router = inject(Router);

  if (isAuthRequest(req.url)) {
    return next(req);
  }

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
      const retriedReq = req.clone({
        setHeaders: { [RETRY_HEADER]: '1' },
      });
      return next(addTokenToRequest(retriedReq, newToken));
    }),
    catchError((refreshError) => {
      authService.clear();
      void router.navigate(['/login']);
      return throwError(() => refreshError);
    }),
  );
}
