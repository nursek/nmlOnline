import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ConflictPreviewService } from '../../services/conflict-preview.service';

@Component({
  selector: 'app-conflicts',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatIconModule, MatProgressBarModule],
  templateUrl: './conflicts.component.html',
  styleUrls: ['./conflicts.component.scss'],
})
export class ConflictsComponent {
  private readonly preview = inject(ConflictPreviewService);

  readonly report = this.preview.report;
  readonly previewing = this.preview.previewing;
  readonly error = this.preview.error;

  readonly hasReportContent = computed(() => {
    const r = this.report();
    return (
      !!r &&
      (r.resolved.length > 0 ||
        r.blocked.length > 0 ||
        r.hasConflicts ||
        r.hasTransitCombats ||
        r.capturedSectors.length > 0)
    );
  });

  constructor() {
    void this.preview.loadPreview();
  }
}
