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
import { httpErrorMessage } from '../core/http-error.interceptor';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenService = inject(TokenService);
  const authService = inject(AuthService);
  const router = inject(Router);

  if (isAuthRequest(req.url)) {
    return next(req);
  }

  const token = tokenService.getAccessToken();
  const scopedReq = addInstanceToRequest(req);
  const authReq = token ? addTokenToRequest(scopedReq, token) : scopedReq;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        return handleUnauthorized(scopedReq, next, tokenService, authService, router);
      }

      // Un 403 ne se rattrape pas par un refresh.
      if (error.status === 403) {
        authService.reportForbidden(httpErrorMessage(error, 'Accès refusé'));
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

/** Lu en sessionStorage : évite une dépendance circulaire avec HttpClient. */
function addInstanceToRequest(req: HttpRequest<unknown>): HttpRequest<unknown> {
  if (!req.url.startsWith('/api/') || isInstanceList(req.url)) {
    return req;
  }
  const instanceId = sessionStorage.getItem('instanceId');
  return instanceId ? req.clone({ setHeaders: { 'X-Board-Id': instanceId } }) : req;
}

// Seule la liste /api/instances est hors partie : /api/admin/instances/{id}/... reste scopé.
function isInstanceList(url: string): boolean {
  return url.split('?')[0] === '/api/instances';
}

function addTokenToRequest(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`,
    },
  });
}

// Seul l'échec du refresh déconnecte : une erreur métier sur la requête rejouée remonte telle quelle.
function handleUnauthorized(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  tokenService: TokenService,
  authService: AuthService,
  router: Router,
): Observable<HttpEvent<unknown>> {
  const logout = (error: unknown) => {
    authService.clear();
    void router.navigate(['/login']);
    return throwError(() => error);
  };
  return tokenService.refreshToken().pipe(
    catchError(logout),
    switchMap((newToken) =>
      next(addTokenToRequest(req, newToken)).pipe(
        catchError((error: HttpErrorResponse) =>
          error.status === 401 ? logout(error) : throwError(() => error),
        ),
      ),
    ),
  );
}
