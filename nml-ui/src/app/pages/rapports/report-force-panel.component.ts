import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { BattlePlayerForce } from '../../models';
import { sideLabel } from '../../core/battle-report-labels';
import { ReportUnitLineComponent } from './report-unit-line.component';
import { battleStatTokens, evasionPercent, forceHasStats, forceSummary } from './rapports.helpers';

@Component({
  selector: 'app-report-force-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe, MatExpansionModule, MatIconModule, ReportUnitLineComponent],
  templateUrl: './report-force-panel.component.html',
  styleUrls: ['./report-force-panel.component.scss'],
})
export class ReportForcePanelComponent {
  readonly force = input.required<BattlePlayerForce>();

  readonly sideLabel = sideLabel;
  readonly forceSummary = forceSummary;
  readonly forceHasStats = forceHasStats;
  readonly battleStatTokens = battleStatTokens;
  readonly evasionPercent = evasionPercent;
}
