import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { highlightIcon, highlightTone, kindLabel, sideLabel } from '../../core/battle-report-labels';
import { BattleUnitForce } from '../../models';
import { BattleReportService } from '../../services/battle-report.service';
import { ExpPipe } from '../../shared/exp.pipe';

@Component({
  selector: 'app-rapports',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatExpansionModule, MatIconModule, MatProgressBarModule, ExpPipe],
  templateUrl: './rapports.component.html',
  styleUrls: ['./rapports.component.scss'],
})
export class RapportsComponent {
  private readonly battleReports = inject(BattleReportService);

  readonly reports = this.battleReports.reports;
  readonly loading = this.battleReports.loading;
  readonly error = this.battleReports.error;

  readonly highlightIcon = highlightIcon;
  readonly highlightTone = highlightTone;
  readonly kindLabel = kindLabel;
  readonly sideLabel = sideLabel;

  constructor() {
    void this.battleReports.loadReports();
  }

  crewNames(crew: BattleUnitForce[]): string {
    return crew.map((member) => member.displayName).join(', ');
  }
}
