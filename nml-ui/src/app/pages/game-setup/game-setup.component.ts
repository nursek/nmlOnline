import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  effect,
  inject,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { firstValueFrom } from 'rxjs';
import { ApiService } from '../../services/api.service';
import { AdminService } from '../../services/admin.service';
import { ActiveBoardService } from '../../services/active-board.service';
import { InstanceService } from '../../services/instance.service';
import { BoardAdminEntry, BoardPreset, Player, AdminPreparationStatus } from '../../models';
import { httpErrorMessage } from '../../core/http-error.interceptor';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';
import {
  StartingSectorsDialogComponent,
  StartingSectorsData,
} from './starting-sectors-dialog.component';
import {
  TemplatePlayerDialogComponent,
  TemplatePlayerData,
} from './template-player-dialog.component';
import { UserPickerDialogComponent, UserPickerData } from './user-picker-dialog.component';

@Component({
  selector: 'app-game-setup',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatSnackBarModule,
  ],
  templateUrl: './game-setup.component.html',
  styleUrls: ['./game-setup.component.scss'],
})
export class GameSetupComponent {
  private readonly api = inject(ApiService);
  private readonly admin = inject(AdminService);
  private readonly activeBoard = inject(ActiveBoardService);
  private readonly instances = inject(InstanceService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  readonly board = this.activeBoard.board;
  readonly boardLoading = this.activeBoard.loading;
  readonly players = this.admin.players;
  readonly playersLoading = this.admin.loading;
  readonly instance = this.instances.selected;
  readonly currentTurn = computed(() => this.instance()?.currentTurn ?? null);
  readonly preparation = computed(() => this.currentTurn() === 0);

  readonly admins = signal<BoardAdminEntry[]>([]);
  readonly preset = signal<BoardPreset | null>(null);
  readonly starting = signal(false);
  readonly prepStatuses = signal<AdminPreparationStatus[]>([]);
  readonly readyCount = computed(() => this.prepStatuses().filter((status) => status.ready).length);

  readonly spots = computed(() => this.preset()?.spots ?? []);
  readonly playerNames = computed(() => {
    const names: Record<number, string> = {};
    for (const player of this.players()) {
      if (player.id !== null) names[player.id] = player.name;
    }
    return names;
  });

  constructor() {
    this.admin.reloadPlayers();
    this.activeBoard.reload();
    void this.loadAdmins();
    void this.loadPreparationStatuses();
    effect(() => {
      const key = this.board()?.presetKey;
      if (key && this.preset()?.key !== key) {
        void this.loadPreset(key);
      }
    });
    const refresh = setInterval(() => {
      if (this.preparation() && !this.starting()) {
        this.admin.reloadPlayers();
        void this.loadPreparationStatuses();
      }
    }, 15_000);
    this.destroyRef.onDestroy(() => clearInterval(refresh));
  }

  private async loadPreset(key: string): Promise<void> {
    try {
      this.preset.set(await firstValueFrom(this.api.adminGetBoardPreset(key)));
    } catch {
      this.preset.set(null);
    }
  }

  private async loadAdmins(): Promise<void> {
    try {
      this.admins.set(await firstValueFrom(this.api.adminGetBoardAdmins()));
    } catch {
      // Le reste de l'écran reste utilisable ; l'erreur est visible sur l'action.
    }
  }

  private async loadPreparationStatuses(): Promise<void> {
    try {
      this.prepStatuses.set(await firstValueFrom(this.api.adminGetPreparationStatus()));
    } catch {
      // Indicateur secondaire : le démarrage reste possible avec l'avertissement générique.
    }
  }

  refreshPreparation(): void {
    this.admin.reloadPlayers();
    void this.loadPreparationStatuses();
  }

  statusFor(player: Player): AdminPreparationStatus | null {
    return this.prepStatuses().find((status) => status.playerId === player.id) ?? null;
  }

  private describeStatus(status: AdminPreparationStatus): string {
    const parts: string[] = [];
    if (!status.accountLinked) parts.push('compte non relié');
    if (status.sectorCount === 0) parts.push('aucun secteur');
    if (!status.characterPlaced) parts.push('personnage non placé');
    if (status.buildingsPlaced < status.buildingsTotal) {
      parts.push(`${status.buildingsTotal - status.buildingsPlaced} bâtiment(s) non placé(s)`);
    }
    if (status.unitsUnclassified > 0) {
      parts.push(`${status.unitsUnclassified} unité(s) non classée(s)`);
    }
    return parts.join(', ') || 'placement incomplet';
  }

  private fallbackLabel(statuses: AdminPreparationStatus[]): string {
    const buildings = statuses.reduce(
      (total, status) => total + (status.buildingsTotal - status.buildingsPlaced),
      0,
    );
    const characters = statuses.filter((status) => !status.characterPlaced).length;
    const units = statuses.reduce((total, status) => total + status.unitsUnclassified, 0);
    if (buildings === 0 && characters === 0 && units === 0) return '';
    const parts: string[] = [];
    if (buildings > 0) parts.push(`${buildings} bâtiment(s)`);
    if (characters > 0) parts.push(`${characters} personnage(s)`);
    if (units > 0) parts.push(`${units} unité(s) non classée(s)`);
    return `Attention : ${parts.join(', ')} seront placés automatiquement (premier secteur / LÉGER).`;
  }

  private startWarning(statuses: AdminPreparationStatus[]): string {
    if (statuses.length === 0) {
      const unplaced = this.players()
        .map((player) => ({ name: player.name, count: this.unplacedCount(player) }))
        .filter((entry) => entry.count > 0);
      return unplaced.length === 0
        ? ' Vérifiez que chaque joueur a terminé son placement.'
        : ' Attention : ' +
            unplaced
              .map(
                (entry) => `${entry.name} : ${entry.count} bâtiment(s)/personnage(s) non placé(s)`,
              )
              .join(', ') +
            '. Les éléments non placés iront sur le premier secteur du joueur ' +
            'et les unités non classées deviendront LÉGER.';
    }
    const ready = statuses.filter((status) => status.ready).length;
    const notReady = statuses.filter((status) => !status.ready);
    const fallback = this.fallbackLabel(statuses);
    if (notReady.length === 0) {
      return (
        ` Tous les joueurs sont prêts (${ready}/${statuses.length}).` +
        (fallback ? ' ' + fallback : '')
      );
    }
    return (
      ` Attention : ${ready}/${statuses.length} prêts. ` +
      notReady.map((status) => `${status.name} (${this.describeStatus(status)})`).join('; ') +
      (fallback ? ' ' + fallback : '.')
    );
  }

  private async run(action: () => Promise<unknown>, success: string): Promise<boolean> {
    try {
      await action();
      this.snackBar.open(success, 'OK', { duration: 3000 });
      return true;
    } catch (error) {
      this.snackBar.open(httpErrorMessage(error, 'Action impossible'), 'Fermer', {
        duration: 6000,
        panelClass: 'error-snackbar',
      });
      return false;
    }
  }

  async startGame(): Promise<void> {
    if (!this.preparation() || this.starting()) return;
    await this.loadPreparationStatuses();
    const warning = this.startWarning(this.prepStatuses());
    const confirmed = await this.confirm(
      'Démarrer la partie',
      'Le tour 0 se termine : les comptes ne pourront plus être reliés et les placements de préparation seront fermés.' +
        warning,
      'Démarrer',
    );
    if (!confirmed) return;
    this.starting.set(true);
    const done = await this.run(() => firstValueFrom(this.api.adminStartGame()), 'Partie démarrée');
    this.starting.set(false);
    if (done) {
      this.instances.invalidate();
      window.location.assign('/admin/tour');
    }
  }

  async createTemplatePlayer(): Promise<void> {
    const board = this.board();
    if (!board || !this.preparation()) return;
    const data: TemplatePlayerData = {
      board,
      spots: this.spots(),
    };
    const request = await firstValueFrom(
      this.dialog
        .open(TemplatePlayerDialogComponent, { data, width: '560px', maxWidth: '95vw' })
        .afterClosed(),
    );
    if (!request) return;
    if (
      await this.run(
        () => firstValueFrom(this.api.adminCreateTemplatePlayer(request)),
        'Joueur prêt créé',
      )
    ) {
      this.admin.reloadPlayers();
      this.activeBoard.reload();
      void this.loadPreparationStatuses();
    }
  }

  async resetPreparation(player: Player): Promise<void> {
    if (player.id === null || !this.preparation()) return;
    const confirmed = await this.confirm(
      'Réinitialiser le placement',
      `Retirer les bâtiments, le personnage et les unités placées de ${player.name} ?`,
      'Réinitialiser',
    );
    if (!confirmed) return;
    if (
      await this.run(
        () => firstValueFrom(this.api.adminResetPreparation(player.id!)),
        'Placement réinitialisé',
      )
    ) {
      this.admin.reloadPlayers();
      this.activeBoard.reload();
      void this.loadPreparationStatuses();
    }
  }

  async assignSectors(player: Player): Promise<void> {
    const board = this.board();
    if (!board || player.id === null || !this.preparation()) return;
    const data: StartingSectorsData = {
      playerId: player.id,
      playerName: player.name,
      current: (player.sectors ?? [])
        .map((sector) => sector.number)
        .filter((number): number is number => number !== null),
      board,
      spots: this.spots(),
      playerNames: this.playerNames(),
    };
    const sectors = await firstValueFrom(
      this.dialog
        .open(StartingSectorsDialogComponent, { data, width: '680px', maxWidth: '95vw' })
        .afterClosed(),
    );
    if (!sectors) return;
    if (
      await this.run(
        () => firstValueFrom(this.api.adminAssignStartingSectors(player.id!, sectors)),
        'Secteurs de départ enregistrés',
      )
    ) {
      this.admin.reloadPlayers();
      this.activeBoard.reload();
      void this.loadPreparationStatuses();
    }
  }

  async linkAccount(player: Player): Promise<void> {
    if (player.id === null || player.userId) return;
    const data: UserPickerData = {
      title: `Relier un compte à ${player.name}`,
      actionLabel: 'Relier',
      excludeUserIds: [...this.linkedUserIds(), ...this.mjUserIds()],
      excludeRoles: ['ADMIN'],
    };
    const user = await firstValueFrom(
      this.dialog
        .open(UserPickerDialogComponent, { data, width: '560px', maxWidth: '95vw' })
        .afterClosed(),
    );
    if (!user) return;
    if (
      await this.run(
        () => firstValueFrom(this.api.adminLinkAccount(player.id!, user.username)),
        'Compte relié',
      )
    ) {
      this.admin.reloadPlayers();
      void this.loadPreparationStatuses();
    }
  }

  async unlinkAccount(player: Player): Promise<void> {
    if (player.id === null || !player.userId) return;
    const confirmed = await this.confirm(
      'Délier le compte',
      `Délier le compte de ${player.name} ? Le joueur ne verra plus la partie.`,
      'Délier',
    );
    if (!confirmed) return;
    if (
      await this.run(() => firstValueFrom(this.api.adminUnlinkAccount(player.id!)), 'Compte délié')
    ) {
      this.admin.reloadPlayers();
      void this.loadPreparationStatuses();
    }
  }

  private linkedUserIds(): number[] {
    return this.players()
      .map((player) => player.userId)
      .filter((userId): userId is number => userId !== null);
  }

  private mjUserIds(): number[] {
    return this.admins().map((admin) => admin.userId);
  }

  async addMj(): Promise<void> {
    const board = this.board();
    if (!board) return;
    const data: UserPickerData = {
      title: 'Nommer un maître du jeu',
      actionLabel: 'Nommer',
      excludeUserIds: [...this.linkedUserIds(), ...this.mjUserIds()],
      excludeRoles: ['ADMIN'],
    };
    const user = await firstValueFrom(
      this.dialog
        .open(UserPickerDialogComponent, { data, width: '560px', maxWidth: '95vw' })
        .afterClosed(),
    );
    if (!user) return;
    if (
      await this.run(
        () => firstValueFrom(this.api.adminGrantBoardAdmin(board.id, user.id)),
        'Rôle MJ attribué',
      )
    ) {
      await this.loadAdmins();
    }
  }

  async removeMj(entry: BoardAdminEntry): Promise<void> {
    const board = this.board();
    if (!board) return;
    const confirmed = await this.confirm(
      'Retirer le rôle MJ',
      `Retirer le rôle MJ de ${entry.username ?? '#' + entry.userId} ?`,
      'Retirer',
    );
    if (!confirmed) return;
    if (
      await this.run(
        () => firstValueFrom(this.api.adminRevokeBoardAdmin(board.id, entry.userId)),
        'Rôle MJ retiré',
      )
    ) {
      await this.loadAdmins();
    }
  }

  unplacedCount(player: Player): number {
    const buildings = (player.buildings ?? []).filter(
      (building) => building.sectorNumber === null,
    ).length;
    const character = player.character && player.character.sectorNumber === null ? 1 : 0;
    return buildings + character;
  }

  sectorNumbers(player: Player): number[] {
    return (player.sectors ?? [])
      .map((sector) => sector.number)
      .filter((number): number is number => number !== null)
      .sort((a, b) => a - b);
  }

  spotOwner(spot: number[]): string {
    for (const number of spot) {
      const sector = this.board()?.sectors[number];
      if (sector?.ownerId) {
        return this.playerNames()[sector.ownerId] ?? `#${sector.ownerId}`;
      }
    }
    return 'Libre';
  }

  private async confirm(title: string, message: string, confirmLabel: string): Promise<boolean> {
    return firstValueFrom(
      this.dialog
        .open(ConfirmDialogComponent, {
          data: { title, message, confirmLabel, cancelLabel: 'Annuler' },
        })
        .afterClosed(),
    ).then((result) => result === true);
  }
}
