import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { BattleReportService } from './battle-report.service';
import { AuthService } from './auth.service';
import type { BattleReport } from '../models';

describe('BattleReportService', () => {
  const user = signal({ id: 1, username: 'lurio' });
  let service: BattleReportService;
  let http: HttpTestingController;

  beforeEach(() => {
    user.set({ id: 1, username: 'lurio' });
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: AuthService,
          useValue: { user: user.asReadonly(), initialized: signal(true) },
        },
      ],
    });
    service = TestBed.inject(BattleReportService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('charge les rapports et expose l’erreur', async () => {
    const loaded = service.loadReports();
    http.expectOne('/api/battle-reports').flush([{ id: 1 }] as unknown as BattleReport[]);
    await loaded;
    expect(service.reports()).toHaveLength(1);
    expect(service.loading()).toBe(false);

    const failed = service.loadReports();
    http
      .expectOne('/api/battle-reports')
      .flush({ detail: 'Indisponible' }, { status: 500, statusText: 'Erreur' });
    await failed;
    expect(service.error()).toBe('Indisponible');
  });
});
