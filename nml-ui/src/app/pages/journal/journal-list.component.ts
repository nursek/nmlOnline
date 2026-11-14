import { ChangeDetectionStrategy, Component, computed } from '@angular/core';
import { DatePipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { GazetteIssueSummary } from '../../models';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-journal-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, RouterLink, MatButtonModule, MatCardModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './journal-list.component.html',
  styleUrls: ['./journal-list.component.scss'],
})
export class JournalListComponent {
  readonly issuesRef = httpResource<GazetteIssueSummary[]>(
    () => `${environment.apiBaseUrl}/game/gazette`,
  );
  readonly issues = computed(() => (this.issuesRef.hasValue() ? this.issuesRef.value() : []));

  retry(): void {
    this.issuesRef.reload();
  }
}
