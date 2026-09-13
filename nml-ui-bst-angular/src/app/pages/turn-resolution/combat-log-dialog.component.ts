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
        background: rgba(249, 115, 22, 0.15);
        color: #9a3412;
        padding: 0.1rem 0.5rem;
        border-radius: 999px;
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
        border-radius: 999px;
        background: rgba(148, 163, 184, 0.18);
        color: #334155;
      }
      .camp.eliminated {
        opacity: 0.6;
        text-decoration: line-through;
      }
      .camp.attacker {
        background: rgba(249, 115, 22, 0.15);
        color: #9a3412;
      }
      .camp.defender {
        background: rgba(59, 130, 246, 0.15);
        color: #1e40af;
      }
      .vs {
        color: #94a3b8;
        font-size: 0.8rem;
      }
      .winner {
        margin-left: auto;
        color: #15803d;
        font-weight: 600;
      }
      .winner.none {
        color: #64748b;
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
        color: #475569;
      }
      .entry {
        font-size: 0.85rem;
        color: #334155;
        padding: 0.1rem 0;
      }
      .entry.dodge {
        color: #0369a1;
      }
      .entry.damage {
        color: #b45309;
      }
      .entry.destroyed {
        color: #b91c1c;
        font-weight: 600;
      }
      .entry.loss {
        color: #b91c1c;
      }
      .entry.gain {
        color: #15803d;
        font-weight: 600;
      }
      .entry.winner {
        color: #15803d;
        font-weight: 600;
      }
      .entry.info {
        color: #475569;
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
