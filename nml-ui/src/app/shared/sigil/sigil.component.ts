import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-sigil',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg
      [attr.width]="size()"
      [attr.height]="size()"
      viewBox="0 0 64 64"
      aria-hidden="true"
      focusable="false"
    >
      <g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="32" cy="32" r="25" stroke-width="5" stroke-dasharray="5 8" />
        <circle cx="32" cy="32" r="20.5" stroke-width="1.5" opacity="0.65" />
        <path
          d="M32 17.5 43.5 22.5 V32.5 C43.5 40 38.5 45.5 32 48.5 C25.5 45.5 20.5 40 20.5 32.5 V22.5 Z"
          stroke-width="2.5"
        />
        <path d="M26.5 29 H37.5 M26.5 33 H37.5 M26.5 37 H33" stroke-width="2" />
      </g>
    </svg>
  `,
  styles: [
    `
      :host {
        display: inline-flex;
        line-height: 0;
      }
    `,
  ],
})
export class SigilComponent {
  readonly size = input(32);
}
