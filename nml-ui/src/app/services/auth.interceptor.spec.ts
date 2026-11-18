import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor — 401 puis refresh', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  const router = { navigate: jest.fn().mockResolvedValue(true) };

  beforeEach(() => {
    sessionStorage.clear();
    sessionStorage.setItem('accessToken', 'expiré');
    sessionStorage.setItem('instanceId', '7');
    router.navigate.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: router },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  function expireThenRefresh(): void {
    backend.expectOne('/api/players/me').flush(null, { status: 401, statusText: 'Unauthorized' });
    backend.expectOne('/api/auth/refresh').flush({ valid: true, token: 'neuf' });
  }

  it('rejoue la requête avec le nouveau jeton et la partie active', () => {
    http.get('/api/players/me').subscribe();
    expireThenRefresh();

    const retried = backend.expectOne('/api/players/me');
    expect(retried.request.headers.get('Authorization')).toBe('Bearer neuf');
    expect(retried.request.headers.get('X-Board-Id')).toBe('7');
    retried.flush({});
  });

  it("ne déconnecte pas quand la requête rejouée échoue pour une raison métier", () => {
    let status = 0;
    http.get('/api/players/me').subscribe({ error: (e) => (status = e.status) });
    expireThenRefresh();
    backend.expectOne('/api/players/me').flush(null, { status: 409, statusText: 'Conflict' });

    expect(status).toBe(409);
    expect(router.navigate).not.toHaveBeenCalled();
    expect(sessionStorage.getItem('accessToken')).toBe('neuf');
  });

  it('déconnecte quand le refresh échoue', () => {
    http.get('/api/players/me').subscribe({ error: () => undefined });
    backend.expectOne('/api/players/me').flush(null, { status: 401, statusText: 'Unauthorized' });
    backend.expectOne('/api/auth/refresh').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(router.navigate).toHaveBeenCalledWith(['/login']);
    expect(sessionStorage.getItem('accessToken')).toBeNull();
  });
});
