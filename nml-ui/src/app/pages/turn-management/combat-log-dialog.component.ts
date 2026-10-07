import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { ResolvedBattle } from '../../models';

interface LogGroup {
  phase: string;
  entries: { outcome: string; message: string }[];
}

@Component({
  selector: 'app-combat-log-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <h2 mat-dialog-title class="dialog-title">
      <mat-icon>receipt_long</mat-icon>
      Combat — secteur {{ battle.sectorNumber }}
      @if (battle.standoff) {
        <span class="standoff-badge">Impasse mexicaine</span>
      }
    </h2>

    <mat-dialog-content>
      <div class="camps">
        @if (battle.standoff) {
          @for (p of battle.participants ?? []; track p.playerId) {
            <span class="camp" [class.eliminated]="p.eliminated">{{
              p.playerName ?? '#' + p.playerId
            }}</span>
          }
        } @else {
          <span class="camp attacker">{{
            battle.attackerName ?? '#' + battle.attackerPlayerId
          }}</span>
          <span class="vs">vs</span>
          <span class="camp defender">{{
            battle.defenderName ?? '#' + battle.defenderPlayerId
          }}</span>
        }
        @if (battle.winnerName) {
          <span class="winner">Vainqueur : {{ battle.winnerName }}</span>
        } @else {
          <span class="winner none">Aucun vainqueur</span>
        }
      </div>

      @for (group of groups(); track $index) {
        <div class="phase">
          <h3>{{ group.phase }}</h3>
          @for (entry of group.entries; track $index) {
            <div class="entry" [class]="'entry ' + entry.outcome.toLowerCase()">
              {{ entry.message }}
            </div>
          }
        </div>
      }
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-flat-button color="primary" (click)="close()">Fermer</button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      mat-dialog-content {
        max-height: 65vh;
        min-width: 420px;
      }
      .dialog-title {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 1.15rem;
      }
      .standoff-badge {
        background: rgba(195,75,28, 0.15);
        color: var(--accent-ink);
        padding: 0.1rem 0.5rem;
        border-radius: var(--r-xs);
        font-size: 0.75rem;
        font-weight: 600;
      }
      .camps {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        flex-wrap: wrap;
        margin-bottom: 1rem;
        font-size: 0.9rem;
      }
      .camp {
        padding: 0.15rem 0.55rem;
        border-radius: var(--r-xs);
        background: rgba(93,86,69, 0.18);
        color: var(--ink);
      }
      .camp.eliminated {
        opacity: 0.6;
        text-decoration: line-through;
      }
      .camp.attacker {
        background: rgba(195,75,28, 0.15);
        color: var(--accent-ink);
      }
      .camp.defender {
        background: rgba(38,64,121, 0.15);
        color: var(--cobalt-deep);
      }
      .vs {
        color: var(--muted);
        font-size: 0.8rem;
      }
      .winner {
        margin-left: auto;
        color: var(--forest);
        font-weight: 600;
      }
      .winner.none {
        color: var(--muted);
        font-weight: 400;
      }
      .phase {
        margin-bottom: 0.9rem;
      }
      .phase h3 {
        margin: 0 0 0.3rem;
        font-size: 0.8rem;
        font-weight: 700;
        text-transform: uppercase;
        letter-spacing: 0.04em;
        color: var(--ink-3);
      }
      .entry {
        font-size: 0.85rem;
        color: var(--ink);
        padding: 0.1rem 0;
      }
      .entry.dodge {
        color: var(--cobalt);
      }
      .entry.damage {
        color: var(--sun-deep);
      }
      .entry.destroyed {
        color: var(--danger-deep);
        font-weight: 600;
      }
      .entry.loss {
        color: var(--danger-deep);
      }
      .entry.gain {
        color: var(--forest);
        font-weight: 600;
      }
      .entry.winner {
        color: var(--forest);
        font-weight: 600;
      }
      .entry.info {
        color: var(--ink-3);
      }
    `,
  ],
})
export class CombatLogDialogComponent {
  readonly dialogRef = inject(MatDialogRef<CombatLogDialogComponent>);
  readonly battle = inject<ResolvedBattle>(MAT_DIALOG_DATA);

  readonly groups = computed<LogGroup[]>(() => {
    const groups: LogGroup[] = [];
    for (const entry of this.battle.battleLog ?? []) {
      let group = groups.at(-1);
      if (!group || group.phase !== entry.phase) {
        group = { phase: entry.phase, entries: [] };
        groups.push(group);
      }
      group.entries.push({ outcome: entry.outcome, message: entry.message });
    }
    return groups;
  });

  close(): void {
    this.dialogRef.close();
  }
}
