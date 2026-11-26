import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { RapportsComponent } from './rapports.component';
import { BattleReportDialogComponent } from './battle-report-dialog.component';
import { BattleReportService } from '../../services/battle-report.service';
import { signal } from '@angular/core';
import type { BattleReport } from '../../models';

describe('RapportsComponent', () => {
  const service = {
    reports: signal([]),
    loading: signal(false),
    error: signal<string | null>(null),
    loadReports: jest.fn().mockResolvedValue(undefined),
  };
  const dialog = { open: jest.fn() };

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        { provide: BattleReportService, useValue: service },
        { provide: MatDialog, useValue: dialog },
      ],
    });
  });

  it('charge les rapports et expose leur état', () => {
    const component = TestBed.runInInjectionContext(() => new RapportsComponent());

    expect(service.loadReports).toHaveBeenCalled();
    expect(component.loading()).toBe(false);
  });

  it('ouvre le détail d’un rapport', () => {
    const component = TestBed.runInInjectionContext(() => new RapportsComponent());
    const report = { id: 12 } as unknown as BattleReport;

    component.openReport(report);

    expect(dialog.open).toHaveBeenCalledWith(
      BattleReportDialogComponent,
      expect.objectContaining({ data: report }),
    );
  });
});
