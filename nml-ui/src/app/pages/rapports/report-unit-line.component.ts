import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { BattleUnitForce } from '../../models';
import { ExpPipe } from '../../shared/exp.pipe';
import { battleStatTokens, evasionPercent } from './rapports.helpers';

@Component({
  selector: 'app-report-unit-line',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe, ExpPipe],
  template: `
    <div class="unit-line" [class.injured]="unit().injured">
      @if (unit().classCode) {
        <span class="class-code">({{ unit().classCode }})</span>
      }
      <span class="unit-name">{{ unit().displayName }}</span>
      @if (unit().experience !== null) {
        <span class="unit-exp">({{ unit().experience | exp }} Exp)</span>
      }
      @if (tag()) {
        <span class="unit-tag">{{ tag() }}</span>
      }
      @if (unit().injured) {
        <span class="badge danger">Blessé</span>
      }
      @if (unit().equipmentNames?.length) {
        <span class="equipment">{{ unit().equipmentNames?.join(' · ') }}</span>
      }
      <span class="stats">
        @for (s of stats(); track s.label) {
          {{ s.separator }}{{ s.value | number: '1.0-1' }} {{ s.label }}
        }
        @if (dodge(); as value) {
          <span class="dodge">Esquive : {{ value }} %</span>
        }
      </span>
    </div>
  `,
  styles: [
    `
      :host {
        display: block;
      }

      .unit-line {
        display: flex;
        align-items: baseline;
        flex-wrap: wrap;
        gap: 0.35rem 0.5rem;
        padding: 0.45rem 0.6rem;
        background: var(--surface);
        border: 1.5px solid var(--line-2);
        border-radius: var(--r-xs);
      }

      .unit-line.injured {
        opacity: 0.8;
      }

      .class-code {
        font-size: 0.75rem;
        font-weight: 700;
        color: var(--accent);
      }

      .unit-name {
        font-size: 0.875rem;
        font-weight: 600;
        color: var(--ink);
      }

      .unit-exp,
      .unit-tag {
        font-family: var(--font-mono);
        font-size: 0.72rem;
        color: var(--muted);
      }

      .unit-tag {
        text-transform: uppercase;
        letter-spacing: 0.06em;
      }

      .equipment {
        font-size: 0.78rem;
        color: var(--ink-3);
      }

      .badge {
        display: inline-block;
        padding: 0.05rem 0.4rem;
        border: 1px solid currentColor;
        border-radius: var(--r-xs);
        font-family: var(--font-mono);
        font-size: 0.65rem;
        font-weight: 600;
        letter-spacing: 0.05em;
        text-transform: uppercase;
      }

      .badge.danger {
        align-self: center;
        background: var(--danger-tint);
        color: var(--danger);
      }

      .stats {
        margin-left: auto;
        font-size: 0.8rem;
        font-weight: 600;
        color: var(--ink);
        font-variant-numeric: tabular-nums;
        white-space: nowrap;
      }

      .dodge {
        margin-left: 0.45rem;
        color: var(--sun);
        font-weight: 500;
      }

      @media (max-width: 640px) {
        .stats {
          margin-left: 0;
          white-space: normal;
        }
      }
    `,
  ],
})
export class ReportUnitLineComponent {
  readonly unit = input.required<BattleUnitForce>();
  readonly tag = input('');

  readonly stats = computed(() => battleStatTokens(this.unit().stats));
  readonly dodge = computed(() => evasionPercent(this.unit().stats));
}
