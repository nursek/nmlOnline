import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { BattleReport } from '../../models';
import {
  highlightIcon,
  highlightTone,
  kindLabel,
  sideLabel,
} from '../../core/battle-report-labels';
import { ExpPipe } from '../../shared/exp.pipe';
import { ReportForcePanelComponent } from './report-force-panel.component';
import {
  casualtyGroups,
  experienceGroups,
  reportResultLabel,
  reportSides,
  reportTone,
} from './rapports.helpers';

@Component({
  selector: 'app-battle-report-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ExpPipe,
    MatButtonModule,
    MatDialogModule,
    MatExpansionModule,
    MatIconModule,
    ReportForcePanelComponent,
  ],
  templateUrl: './battle-report-dialog.component.html',
  styleUrls: ['./battle-report-dialog.component.scss'],
})
export class BattleReportDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<BattleReportDialogComponent>);

  readonly report: BattleReport = inject(MAT_DIALOG_DATA);

  readonly resultLabel = reportResultLabel(this.report);
  readonly tone = reportTone(this.report);
  readonly forces = this.report.forces ?? [];
  readonly camps = this.report.camps ?? [];
  readonly sides = reportSides(this.report);
  readonly losses = casualtyGroups(this.report);
  readonly gains = experienceGroups(this.report).filter((group) => group.entries.length > 0);

  readonly kindLabel = kindLabel;
  readonly sideLabel = sideLabel;
  readonly highlightIcon = highlightIcon;
  readonly highlightTone = highlightTone;

  close(): void {
    this.dialogRef.close();
  }
}
