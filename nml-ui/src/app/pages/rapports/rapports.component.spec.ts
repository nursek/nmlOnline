import { TestBed } from '@angular/core/testing';
import { RapportsComponent } from './rapports.component';
import { BattleReportService } from '../../services/battle-report.service';
import { signal } from '@angular/core';
import type { BattleUnitForce } from '../../models';

describe('RapportsComponent', () => {
  const service = {
    reports: signal([]),
    loading: signal(false),
    error: signal<string | null>(null),
    loadReports: jest.fn().mockResolvedValue(undefined),
  };

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [{ provide: BattleReportService, useValue: service }],
    });
  });

  it('charge les rapports et expose leur état', () => {
    const component = TestBed.runInInjectionContext(() => new RapportsComponent());

    expect(service.loadReports).toHaveBeenCalled();
    expect(component.loading()).toBe(false);
  });

  it('joint les noms de l’équipage', () => {
    const component = TestBed.runInInjectionContext(() => new RapportsComponent());
    const crew = [
      { displayName: 'VOYOU n°1' },
      { displayName: 'VOYOU n°2' },
    ] as BattleUnitForce[];

    expect(component.crewNames(crew)).toBe('VOYOU n°1, VOYOU n°2');
    expect(component.crewNames([])).toBe('');
  });
});
