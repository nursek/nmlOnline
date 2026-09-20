import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { BattleReportService } from '../../services/battle-report.service';
import { AllianceStateService } from '../../services/alliance-state.service';
import { Announcement } from '../../models';
import { ExpPipe } from '../../shared/exp.pipe';

@Component({
  selector: 'app-rapports',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatCardModule, MatExpansionModule, MatIconModule, MatProgressBarModule, ExpPipe],
  templateUrl: './rapports.component.html',
  styleUrls: ['./rapports.component.scss'],
})
export class RapportsComponent {
  private readonly battleReports = inject(BattleReportService);
  private readonly allianceState = inject(AllianceStateService);

  readonly reports = this.battleReports.reports;
  readonly loading = this.battleReports.loading;
  readonly error = this.battleReports.error;
  readonly announcements = this.allianceState.announcements;

  constructor() {
    void this.battleReports.loadReports();
    void this.allianceState.loadAnnouncements();
  }

  announcementLabel(announcement: Announcement): string {
    switch (announcement.type) {
      case 'ALLIANCE_FORMED':
        return `${announcement.actorName} et ${announcement.targetName} sont désormais alliés`;
      case 'ALLIANCE_BROKEN':
        return `${announcement.actorName} et ${announcement.targetName} ont rompu leur alliance`;
      case 'BETRAYAL':
        return `${announcement.actorName} a trahi ${announcement.targetName}`;
      default:
        return 'Annonce';
    }
  }
}
