import { TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';
import { ApiService } from './api.service';
import { ConflictPreviewService } from './conflict-preview.service';
import type { MovementResolutionResult } from '../models';

function report(turn: number): MovementResolutionResult {
  return {
    turn,
    resolved: [],
    blocked: [],
    conflicts: [],
    transitCombats: [],
    capturedSectors: [],
    hasConflicts: false,
    hasTransitCombats: false,
  };
}

describe('ConflictPreviewService', () => {
  it('ne laisse pas une réponse périmée écraser la plus récente', async () => {
    const first = new Subject<MovementResolutionResult>();
    const second = new Subject<MovementResolutionResult>();
    const api = {
      adminPreviewMovements: jest
        .fn()
        .mockReturnValueOnce(first.asObservable())
        .mockReturnValueOnce(second.asObservable()),
    };

    TestBed.configureTestingModule({
      providers: [ConflictPreviewService, { provide: ApiService, useValue: api }],
    });
    const service = TestBed.inject(ConflictPreviewService);

    const firstLoad = service.loadPreview();
    const secondLoad = service.loadPreview();

    second.next(report(2));
    second.complete();
    await secondLoad;
    first.next(report(1));
    first.complete();
    await firstLoad;

    expect(service.report()?.turn).toBe(2);
    expect(service.previewing()).toBe(false);
  });
});
