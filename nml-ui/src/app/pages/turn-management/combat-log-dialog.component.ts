import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { highlightIcon, highlightTone, sideLabel } from '../../core/battle-report-labels';
import { BattlePlayerForce, ResolvedBattle } from '../../models';
import { groupBattleLog } from './turn-export.helpers';

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

      @if (battle.highlights?.length) {
        <div class="section">
          <h3>Points clés</h3>
          <ul class="highlight-list">
            @for (highlight of battle.highlights; track $index) {
              <li [class]="'highlight ' + highlightTone(highlight.kind)">
                <mat-icon>{{ highlightIcon(highlight.kind) }}</mat-icon>
                <span>{{ highlight.message }}</span>
                @if (highlight.visibility === 'ADMIN') {
                  <em class="admin-badge">debug</em>
                }
              </li>
            }
          </ul>
        </div>
      }

      @if (battle.forces?.length) {
        <div class="section">
          <h3>Forces en présence</h3>
          <div class="forces">
            @for (force of battle.forces; track force.playerId) {
              <article class="force">
                <header>
                  <strong>{{ force.playerName ?? '#' + force.playerId }}</strong>
                  <span class="side">{{ sideLabel(force.side) }}</span>
                </header>
                <p>{{ forceSummary(force) }}</p>
              </article>
            }
          </div>
        </div>
      }

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
      .section {
        margin-bottom: 1rem;
      }
      .section h3 {
        margin: 0 0 0.35rem;
        font-size: 0.8rem;
        font-weight: 700;
        text-transform: uppercase;
        letter-spacing: 0.04em;
        color: var(--ink-3);
      }
      .highlight-list {
        list-style: none;
        margin: 0;
        padding: 0;
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
      }
      .highlight {
        display: flex;
        align-items: center;
        gap: 0.4rem;
        font-size: 0.86rem;
        color: var(--ink);
      }
      .highlight mat-icon {
        font-size: 16px;
        width: 16px;
        height: 16px;
        color: var(--ink-3);
      }
      .highlight.character_lost,
      .highlight.heavy_losses {
        color: var(--danger-deep);
        font-weight: 600;
      }
      .highlight.winner,
      .highlight.promotion {
        color: var(--forest);
        font-weight: 600;
      }
      .admin-badge {
        margin-left: auto;
        padding: 0 0.3rem;
        border: 1px solid color-mix(in srgb, var(--ink) 18%, transparent);
        border-radius: var(--r-xs);
        color: var(--muted);
        font-size: 0.7rem;
        font-style: normal;
        text-transform: uppercase;
        letter-spacing: 0.04em;
      }
      .forces {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
        gap: 0.5rem;
      }
      .force {
        padding: 0.4rem 0.6rem;
        border: 1px solid color-mix(in srgb, var(--ink) 12%, transparent);
        border-radius: var(--r-sm);
      }
      .force header {
        display: flex;
        align-items: center;
        gap: 0.4rem;
        font-size: 0.88rem;
      }
      .force .side {
        margin-left: auto;
        color: var(--cobalt-deep);
        font-size: 0.72rem;
        text-transform: uppercase;
        letter-spacing: 0.03em;
      }
      .force p {
        margin: 0.2rem 0 0;
        color: var(--ink-3);
        font-size: 0.82rem;
      }
      .entry.crit {
        color: var(--rust);
        font-weight: 600;
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

  readonly highlightIcon = highlightIcon;
  readonly highlightTone = highlightTone;
  readonly sideLabel = sideLabel;

  forceSummary(force: BattlePlayerForce): string {
    const parts: string[] = [];
    if (force.characters.length) {
      parts.push(`Personnages : ${force.characters.map((unit) => unit.displayName).join(', ')}`);
    }
    if (force.vehicles.length) {
      parts.push(`Véhicules : ${force.vehicles.map((vehicle) => vehicle.displayName).join(', ')}`);
    }
    if (force.infantry.length) {
      parts.push(`Infanterie : ${force.infantry.map((unit) => unit.displayName).join(', ')}`);
    }
    if (force.buildings.length) {
      parts.push(`Bâtiments : ${force.buildings.map((building) => building.displayName).join(', ')}`);
    }
    return parts.join(' · ');
  }

  readonly groups = computed(() => groupBattleLog(this.battle.battleLog));

  close(): void {
    this.dialogRef.close();
  }
}
