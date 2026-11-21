import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { kindLabel } from '../../core/battle-report-labels';
import { BattleReport } from '../../models';
import { BattleReportService } from '../../services/battle-report.service';
import { BattleReportDialogComponent } from './battle-report-dialog.component';
import { reportOpponentsLabel, reportResultLabel, reportTone } from './rapports.helpers';

@Component({
  selector: 'app-rapports',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatIconModule, MatProgressBarModule],
  templateUrl: './rapports.component.html',
  styleUrls: ['./rapports.component.scss'],
})
export class RapportsComponent {
  private readonly battleReports = inject(BattleReportService);
  private readonly dialog = inject(MatDialog);

  readonly reports = this.battleReports.reports;
  readonly loading = this.battleReports.loading;
  readonly error = this.battleReports.error;

  readonly kindLabel = kindLabel;
  readonly reportResultLabel = reportResultLabel;
  readonly reportOpponentsLabel = reportOpponentsLabel;
  readonly reportTone = reportTone;

  constructor() {
    void this.battleReports.loadReports();
  }

  openReport(report: BattleReport): void {
    this.dialog.open(BattleReportDialogComponent, {
      data: report,
      width: '860px',
      maxWidth: '95vw',
    });
  }
}
