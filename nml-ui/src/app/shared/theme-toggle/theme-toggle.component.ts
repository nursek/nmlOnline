import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ThemeService } from '../../core/theme.service';

@Component({
  selector: 'app-theme-toggle',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatIconModule],
  template: `
    <button mat-icon-button type="button" [attr.aria-label]="label()" (click)="theme.toggle()">
      <mat-icon>{{ theme.theme() === 'dark' ? 'light_mode' : 'dark_mode' }}</mat-icon>
    </button>
  `,
})
export class ThemeToggleComponent {
  readonly theme = inject(ThemeService);
  readonly label = computed(() =>
    this.theme.theme() === 'dark' ? 'Passer au thème clair' : 'Passer au thème sombre',
  );
}
