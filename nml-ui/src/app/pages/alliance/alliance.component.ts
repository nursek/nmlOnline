import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom, interval } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ApiService } from '../../services/api.service';
import { AllianceStateService } from '../../services/alliance-state.service';
import { PlayerService } from '../../services/player.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';
import { nextSelectedAllianceId } from './alliance.helpers';
import { Player } from '../../models';

const CHAT_POLL_MS = 8000;

@Component({
  selector: 'app-alliance',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatSelectModule,
  ],
  templateUrl: './alliance.component.html',
  styleUrls: ['./alliance.component.scss'],
})
export class AllianceComponent {
  private readonly allianceState = inject(AllianceStateService);
  private readonly playerService = inject(PlayerService);
  private readonly api = inject(ApiService);
  private readonly dialog = inject(MatDialog);
  private readonly dialogRef = inject(MatDialogRef<AllianceComponent>);
  private readonly snackBar = inject(MatSnackBar);

  readonly me = this.allianceState.me;
  readonly messages = this.allianceState.messages;
  readonly loading = this.allianceState.loading;
  readonly error = this.allianceState.error;
  readonly player = this.playerService.player;
  readonly players = signal<Player[]>([]);
  readonly targetPlayerId = signal<number | null>(null);
  readonly selectedAllianceId = signal<number | null>(null);
  readonly messageDraft = signal('');
  readonly selectedAlliance = computed(
    () => this.me()?.alliances.find((alliance) => alliance.id === this.selectedAllianceId()) ?? null,
  );

  constructor() {
    void this.allianceState.loadMe();
    void this.playerService.loadCurrent();
    void this.loadPlayers();

    effect(() => {
      const alliances = this.me()?.alliances ?? [];
      const next = nextSelectedAllianceId(alliances, this.selectedAllianceId());
      if (next !== this.selectedAllianceId()) {
        this.selectedAllianceId.set(next);
        if (next != null) {
          void this.allianceState.loadMessages(next);
        }
      }
    });

    interval(CHAT_POLL_MS)
      .pipe(takeUntilDestroyed())
      .subscribe(async () => {
        await this.allianceState.refreshMe();
        const allianceId = nextSelectedAllianceId(
          this.me()?.alliances ?? [],
          this.selectedAllianceId(),
        );
        if (allianceId !== this.selectedAllianceId()) {
          this.selectedAllianceId.set(allianceId);
        }
        if (allianceId != null) {
          void this.allianceState.loadMessages(allianceId);
        }
      });
  }

  selectAlliance(allianceId: number): void {
    this.selectedAllianceId.set(allianceId);
    void this.allianceState.loadMessages(allianceId);
  }

  close(): void {
    this.dialogRef.close();
  }

  async propose(): Promise<void> {
    const target = this.targetPlayerId();
    if (target == null) return;
    if (await this.allianceState.propose('ALLIANCE', target)) {
      this.notify('Proposition envoyée');
    }
  }

  async proposeRupture(allyPlayerId: number): Promise<void> {
    if (await this.allianceState.propose('RUPTURE', allyPlayerId)) {
      this.notify('Proposition de rupture envoyée');
    }
  }

  async accept(proposalId: number): Promise<void> {
    if (await this.allianceState.accept(proposalId)) {
      this.notify('Proposition acceptée');
    }
  }

  async decline(proposalId: number): Promise<void> {
    if (await this.allianceState.decline(proposalId)) {
      this.notify('Proposition refusée');
    }
  }

  async withdraw(proposalId: number): Promise<void> {
    if (await this.allianceState.withdraw(proposalId)) {
      this.notify('Proposition retirée');
    }
  }

  async betray(allianceId: number, allyName: string): Promise<void> {
    const confirmed = await this.confirm(
      'Trahir cette alliance ?',
      `Trahir ${allyName} donne un bonus de trahison (15 % + 10 % par tour d'alliance, ce tour uniquement) `
        + 'et sera annoncé publiquement au tour suivant. Vos unités restées chez lui se battront.',
      'Trahir',
    );
    if (!confirmed) return;
    if (await this.allianceState.betray(allianceId)) {
      this.notify(`Trahison de ${allyName} — annonce au tour suivant`);
    }
  }

  async sendMessage(): Promise<void> {
    const allianceId = this.selectedAllianceId();
    const body = this.messageDraft();
    if (allianceId == null || !body.trim()) return;
    if (await this.allianceState.sendMessage(allianceId, body)) {
      this.messageDraft.set('');
    }
  }

  private async loadPlayers(): Promise<void> {
    try {
      const page = await firstValueFrom(this.api.getPlayerSummaries());
      this.players.set(page.content);
    } catch {
      // Liste secondaire : le formulaire de proposition reste vide.
    }
  }

  private notify(message: string): void {
    this.snackBar.open(message, 'OK', { duration: 3000 });
  }

  private confirm(title: string, message: string, confirmLabel: string): Promise<boolean> {
    return new Promise((resolve) => {
      this.dialog
        .open(ConfirmDialogComponent, { data: { title, message, confirmLabel } })
        .afterClosed()
        .subscribe((result) => resolve(result === true));
    });
  }
}
