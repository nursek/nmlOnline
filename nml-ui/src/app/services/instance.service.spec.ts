import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { InstanceService } from './instance.service';
import { authInterceptor } from './auth.interceptor';
import { GameInstance } from '../models';
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
    participant: true,
  };
}

describe('InstanceService (partie sélectionnée et en-tête X-Board-Id)', () => {
  beforeEach(() => sessionStorage.clear());
  afterEach(() => sessionStorage.clear());

  it('charge les parties, persiste le choix et signale la partie active', async () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const service = TestBed.inject(InstanceService);
    const http = TestBed.inject(HttpTestingController);

    const loading = service.load();
    http
      .expectOne(`${environment.apiBaseUrl}/instances`)
      .flush([instance(7, 'Lurio'), instance(9, 'Nurio')]);
    await loading;

    expect(service.instances().length).toBe(2);
    expect(service.myInstances().length).toBe(2);
    expect(service.selected()).toBeNull();

    service.select(instance(9, 'Nurio'));
    expect(sessionStorage.getItem('instanceId')).toBe('9');
    expect(service.selected()?.name).toBe('Nurio');
    expect(service.selectedRole()).toBe('PLAYER');

    service.clear();
    expect(sessionStorage.getItem('instanceId')).toBeNull();
    expect(service.selected()).toBeNull();
  });

  it('ne compte que les parties où le compte joue, pas celles ouvertes par le rôle admin', async () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const service = TestBed.inject(InstanceService);
    const http = TestBed.inject(HttpTestingController);

    const first = service.ensureLoaded();
    http
      .expectOne(`${environment.apiBaseUrl}/instances`)
      .flush([
        instance(7, 'Lurio'),
        { ...instance(9, 'AdminSeulement'), participant: false, role: 'ADMIN' },
      ]);
    await first;
    expect(service.instances().length).toBe(2);
    expect(service.myInstances().map((candidate) => candidate.id)).toEqual([7]);

    await service.ensureLoaded();
    http.expectNone(`${environment.apiBaseUrl}/instances`);

    service.invalidate();
    const reload = service.ensureLoaded();
    http.expectOne(`${environment.apiBaseUrl}/instances`).flush([instance(7, 'Lurio')]);
    await reload;
    http.verify();
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
});
