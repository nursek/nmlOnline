import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  numberAttribute,
} from '@angular/core';
import { httpResource } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { GazetteIssue } from '../../models';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../services/auth.service';
import { InstanceService } from '../../services/instance.service';
import { GazetteTemplateComponent } from '../../shared/gazette/gazette-template.component';

@Component({
  selector: 'app-journal-issue',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    GazetteTemplateComponent,
  ],
  templateUrl: './journal-issue.component.html',
  styleUrls: ['./journal-issue.component.scss'],
})
export class JournalIssueComponent {
  private readonly auth = inject(AuthService);
  private readonly instance = inject(InstanceService);

  readonly turn = input<number | null>(null, { transform: numberAttribute });
  readonly issueRef = httpResource<GazetteIssue>(() =>
    this.turn() === null ? undefined : `${environment.apiBaseUrl}/game/gazette/${this.turn()}`,
  );
  readonly issue = computed(() => (this.issueRef.hasValue() ? this.issueRef.value() : null));
  readonly canAdmin = this.auth.canAdmin;

  retry(): void {
    this.issueRef.reload();
  }

  print(): void {
    window.print();
  }
}
