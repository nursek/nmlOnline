import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { firstValueFrom } from 'rxjs';
import { httpErrorInterceptor } from './http-error.interceptor';
import { APP_CONSTANTS } from './constants';
import { PlayerStatusService } from '../services/player-status.service';
import { PlayerService } from '../services/player.service';

describe('httpErrorInterceptor (lecture seule)', () => {
  const reload = jest.fn();
  const loadCurrent = jest.fn();

  const setup = (playerLoaded: boolean) => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([httpErrorInterceptor])),
        provideHttpClientTesting(),
        { provide: PlayerStatusService, useValue: { reload } },
        {
          provide: PlayerService,
          useValue: { player: () => (playerLoaded ? { id: 1 } : null), loadCurrent },
        },
      ],
    });
  };

  beforeEach(() => {
    reload.mockClear();
    loadCurrent.mockClear();
  });

  const postWithStatus = async (status: number): Promise<void> => {
    const request = firstValueFrom(TestBed.inject(HttpClient).post('/api/units/orders', {}));
    TestBed.inject(HttpTestingController)
      .expectOne('/api/units/orders')
      .flush({}, { status, statusText: 'Conflict' });
    await expect(request).rejects.toBeTruthy();
  };

  it('rafraîchit le statut et le profil sur un 409', async () => {
    setup(true);

    await postWithStatus(409);

    expect(reload).toHaveBeenCalledTimes(1);
    expect(loadCurrent).toHaveBeenCalledTimes(1);
  });

  it("ne recharge pas le profil d'un compte sans joueur", async () => {
    setup(false);

    await postWithStatus(409);

    expect(reload).toHaveBeenCalledTimes(1);
    expect(loadCurrent).not.toHaveBeenCalled();
  });


  it('rejoue une requête GET après une erreur 5xx', () => {
    jest.useFakeTimers();
    setup(true);
    let received: unknown;
    TestBed.inject(HttpClient)
      .get('/api/ready')
      .subscribe((value) => (received = value));
    const backend = TestBed.inject(HttpTestingController);

    backend.expectOne('/api/ready').flush(null, { status: 503, statusText: 'Indisponible' });
    jest.advanceTimersByTime(APP_CONSTANTS.HTTP_RETRY_DELAY_MS);
    const retried = backend.expectOne('/api/ready');
    expect(retried.request.method).toBe('GET');
    retried.flush({ ready: true });

    expect(received).toEqual({ ready: true });
    jest.useRealTimers();
  });

  it('ne rejoue pas une requête GET en 4xx', async () => {
    setup(true);
    const request = firstValueFrom(TestBed.inject(HttpClient).get('/api/absent'));
    const backend = TestBed.inject(HttpTestingController);

    backend.expectOne('/api/absent').flush(null, { status: 404, statusText: 'Absent' });
    await expect(request).rejects.toBeTruthy();
    backend.expectNone('/api/absent');
  });
  it('ignore les autres erreurs', async () => {
    setup(true);

    await postWithStatus(500);

    expect(reload).not.toHaveBeenCalled();
    expect(loadCurrent).not.toHaveBeenCalled();
  });
});
