import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Announcement, GameStatistics } from '../../models';
import { environment } from '../../../environments/environment';
import { ActiveBoardService } from '../../services/active-board.service';
import { AllianceStateService } from '../../services/alliance-state.service';
import { PlayerDirectoryService } from '../../services/player-directory.service';
import { RankingsPanelComponent } from './rankings-panel.component';

@Component({
  selector: 'app-accueil',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatCardModule, MatIconModule, MatProgressSpinnerModule, RankingsPanelComponent],
  templateUrl: './accueil.component.html',
  styleUrls: ['./accueil.component.scss'],
})
export class AccueilComponent {
  private readonly activeBoard = inject(ActiveBoardService);
  private readonly playerDirectory = inject(PlayerDirectoryService);
  private readonly allianceState = inject(AllianceStateService);

  private readonly turnRef = httpResource<number>(() => `${environment.apiBaseUrl}/turn/current`);
  private readonly statisticsRef = httpResource<GameStatistics>(
    () => `${environment.apiBaseUrl}/statistics`,
  );

  readonly loading = computed(() => this.activeBoard.loading() || this.playerDirectory.loading());
  readonly boardError = this.activeBoard.error;
  readonly turn = this.turnRef.value;
  readonly players = this.playerDirectory.players;
  readonly playersTruncated = this.playerDirectory.truncated;
  readonly playerColors = this.playerDirectory.colorMap;
  readonly announcements = this.allianceState.announcements;
  readonly statistics = computed(() =>
    this.statisticsRef.hasValue() ? this.statisticsRef.value() : null,
  );
  readonly statisticsError = computed(() => this.statisticsRef.error() !== undefined);
  readonly unitDeaths = computed(
    () => this.statistics()?.units.reduce((total, entry) => total + entry.count, 0) ?? 0,
  );
  readonly vehicleDeaths = computed(
    () => this.statistics()?.vehicles.reduce((total, entry) => total + entry.count, 0) ?? 0,
  );

  readonly totalSectors = computed(
    () => Object.values(this.activeBoard.board()?.sectors ?? {}).length,
  );
  readonly conqueredSectors = computed(
    () => Object.values(this.activeBoard.board()?.sectors ?? {}).filter((s) => s.ownerId).length,
  );
  readonly neutralSectors = computed(
    () => Object.values(this.activeBoard.board()?.sectors ?? {}).filter((s) => !s.ownerId).length,
  );

  constructor() {
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
