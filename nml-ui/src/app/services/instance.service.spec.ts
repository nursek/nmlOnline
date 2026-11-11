import { ApplicationRef, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { InstanceService } from './instance.service';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';
import { GameInstance, User } from '../models';
import { environment } from '../../environments/environment';

function instance(id: number, name: string): GameInstance {
  return {
    id,
    name,
    currentTurn: 3,
    playerCount: 4,
    playerId: id * 10,
    playerName: name,
    eliminated: false,
    role: 'PLAYER',
  };
}

describe('InstanceService (partie sélectionnée et en-tête X-Board-Id)', () => {
  beforeEach(() => sessionStorage.clear());
  afterEach(() => sessionStorage.clear());

  it('persiste le choix et signale la partie active', async () => {
    const user = signal<User | null>({ id: 1, username: 'lurio' });
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { user: user.asReadonly() } },
      ],
    });
    const service = TestBed.inject(InstanceService);
    const http = TestBed.inject(HttpTestingController);
    const appRef = TestBed.inject(ApplicationRef);

    TestBed.tick();
    http
      .expectOne(`${environment.apiBaseUrl}/instances`)
      .flush([instance(7, 'Lurio'), instance(9, 'Nurio')]);
    await appRef.whenStable();
    expect(service.instances().length).toBe(2);
    expect(service.selected()).toBeNull();

    service.select(instance(9, 'Nurio'));
    expect(sessionStorage.getItem('instanceId')).toBe('9');
    expect(service.selected()?.name).toBe('Nurio');
    expect(service.selectedRole()).toBe('PLAYER');

    service.clear();
    expect(sessionStorage.getItem('instanceId')).toBeNull();
    expect(service.selected()).toBeNull();
  });

  it("ajoute X-Board-Id aux appels d'API, sauf sur /instances", () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    const http = TestBed.inject(HttpTestingController);
    const client = TestBed.inject(HttpClient);
    sessionStorage.setItem('instanceId', '42');

    client.get('/api/players/me').subscribe();
    const scoped = http.expectOne('/api/players/me');
    expect(scoped.request.headers.get('X-Board-Id')).toBe('42');
    scoped.flush({});

    client.get(`${environment.apiBaseUrl}/instances`).subscribe();
    const unscoped = http.expectOne(`${environment.apiBaseUrl}/instances`);
    expect(unscoped.request.headers.get('X-Board-Id')).toBeNull();
    unscoped.flush([]);
    http.verify();
  });

  it("n'expose une partie unique que s'il n'y a pas de choix", async () => {
    const user = signal<User | null>({ id: 1, username: 'lurio' });
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { user: user.asReadonly() } },
      ],
    });
    const service = TestBed.inject(InstanceService);
    const http = TestBed.inject(HttpTestingController);
    const appRef = TestBed.inject(ApplicationRef);

    TestBed.tick();
    http.expectOne(`${environment.apiBaseUrl}/instances`).flush([instance(7, 'Lurio')]);
    await appRef.whenStable();
    expect(service.soleInstance()?.id).toBe(7);

    http.expectNone(`${environment.apiBaseUrl}/instances`);
    service.select(instance(7, 'Lurio'));
    expect(service.soleInstance()).not.toBeNull();
  });
});
