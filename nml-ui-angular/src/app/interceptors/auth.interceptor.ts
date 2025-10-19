import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * Intercepteur HTTP pour ajouter automatiquement le token d'authentification
 * aux requêtes sortantes
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  // Si un token existe et que la requête n'est pas vers /login ou /register
  if (token && !req.url.includes('/login') && !req.url.includes('/register')) {
    // Clone la requête et ajoute le header Authorization
    const authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      },
      withCredentials: true // Pour gérer les cookies httpOnly
    });
    return next(authReq);
  }

  // Pour les requêtes auth, on garde withCredentials
  if (req.url.includes('/api/auth') || req.url.includes('/login') || req.url.includes('/register')) {
    const authReq = req.clone({
      withCredentials: true
    });
    return next(authReq);
  }

  return next(req);
};
