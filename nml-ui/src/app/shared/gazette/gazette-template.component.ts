import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { GazetteIssue } from '../../models';
import { PlayerPhotoComponent } from '../player-photo/player-photo.component';
import { editionLabel, isUpdated, paragraphs, weatherLine } from './gazette.helpers';

@Component({
  selector: 'app-gazette-template',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, MatIconModule, PlayerPhotoComponent],
  templateUrl: './gazette-template.component.html',
  styleUrls: ['./gazette-template.component.scss'],
})
export class GazetteTemplateComponent {
  readonly issue = input.required<GazetteIssue>();

  readonly lead = computed(() => this.issue().frontPage?.[0] ?? null);
  readonly secondary = computed(() => this.issue().frontPage?.slice(1) ?? []);
  readonly briefs = computed(() => this.issue().briefs ?? []);
  readonly edition = computed(() => editionLabel(this.issue()));
  readonly weather = computed(() => weatherLine(this.issue().masthead.weather));
  readonly updated = computed(() => isUpdated(this.issue()));
  readonly paragraphs = paragraphs;
}
