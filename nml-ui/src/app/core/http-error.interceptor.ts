import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { retry, timeout } from 'rxjs/operators';
import { APP_CONSTANTS } from './constants';

/**
 * Intercepteur HTTP global.
 * - Applique un timeout à toutes les requêtes.
 * - Retente une fois les requêtes idempotentes (GET/HEAD/OPTIONS) en cas d'erreur réseau.
 * Note : la gestion 401/refresh est volontairement laissée à auth.interceptor.
 */
export const httpErrorInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
): Observable<HttpEvent<unknown>> => {
  const idempotent = req.method === 'GET' || req.method === 'HEAD' || req.method === 'OPTIONS';

  return next(req).pipe(
    timeout(APP_CONSTANTS.HTTP_TIMEOUT_MS),
    idempotent
      ? retry({ count: APP_CONSTANTS.HTTP_RETRY_COUNT, delay: APP_CONSTANTS.HTTP_RETRY_DELAY_MS })
      : (source) => source,
  );
};

/**
 * Extract a readable message from an HTTP error response, walking the common
 * backend shapes (`error.detail`, `error.error`, `error.message`) before
 * falling back to `Error.message` and finally `fallback`.
 */
export function httpErrorMessage(error: unknown, fallback: string): string {
  const e = error as {
    error?: { detail?: string; error?: string; message?: string };
    message?: string;
  };
  return e?.error?.detail || e?.error?.error || e?.error?.message || e?.message || fallback;
}
