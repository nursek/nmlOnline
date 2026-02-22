import { adminReducer, initialState, AdminState } from './admin.reducer';
import { AdminActions } from './admin.actions';
import { Player } from '../../models';

describe('AdminReducer', () => {
  const mockPlayer: Player = {
    id: 1,
    name: 'TestPlayer',
    stats: {
      money: 1000,
      totalIncome: 100,
      totalVehiclesValue: 0,
      totalEquipmentValue: 0,
      totalOffensivePower: 0,
      totalDefensivePower: 0,
      globalPower: 0,
      totalEconomyPower: 0,
      totalAtk: 0,
      totalPdf: 0,
      totalPdc: 0,
      totalDef: 0,
      totalArmor: 0,
    },
    equipments: [],
    resources: [],
    sectors: [],
    character: null,
    buildings: [],
  };

  const mockPlayer2: Player = { ...mockPlayer, id: 2, name: 'Player2' };

  describe('initial state', () => {
    it('should return the initial state', () => {
      const state = adminReducer(undefined, { type: 'NOOP' } as any);
      expect(state).toEqual(initialState);
    });

    it('should start with empty players and no loading', () => {
      expect(initialState.players).toEqual([]);
      expect(initialState.loading).toBe(false);
      expect(initialState.importing).toBe(false);
      expect(initialState.error).toBeNull();
      expect(initialState.successMessage).toBeNull();
    });
  });

  describe('Fetch Admin Players', () => {
    it('should set loading to true on fetchAdminPlayers', () => {
      const state = adminReducer(initialState, AdminActions.fetchAdminPlayers());
      expect(state.loading).toBe(true);
      expect(state.error).toBeNull();
    });

    it('should set players on fetchAdminPlayersSuccess', () => {
      const players = [mockPlayer, mockPlayer2];
      const state = adminReducer(
        { ...initialState, loading: true },
        AdminActions.fetchAdminPlayersSuccess({ players })
      );
      expect(state.loading).toBe(false);
      expect(state.players).toEqual(players);
    });

    it('should set error on fetchAdminPlayersFailure', () => {
      const state = adminReducer(
        { ...initialState, loading: true },
        AdminActions.fetchAdminPlayersFailure({ error: 'Erreur serveur' })
      );
      expect(state.loading).toBe(false);
      expect(state.error).toBe('Erreur serveur');
    });
  });

  describe('Import Player', () => {
    it('should set importing to true on importPlayer', () => {
      const file = new File(['{}'], 'player.json', { type: 'application/json' });
      const state = adminReducer(initialState, AdminActions.importPlayer({ file }));
      expect(state.importing).toBe(true);
      expect(state.error).toBeNull();
      expect(state.successMessage).toBeNull();
    });

    it('should set successMessage on importPlayerSuccess', () => {
      const state = adminReducer(
        { ...initialState, importing: true },
        AdminActions.importPlayerSuccess({ player: mockPlayer })
      );
      expect(state.importing).toBe(false);
      expect(state.successMessage).toBe('Joueur "TestPlayer" importé avec succès');
    });

    it('should set error on importPlayerFailure', () => {
      const state = adminReducer(
        { ...initialState, importing: true },
        AdminActions.importPlayerFailure({ error: 'Format invalide' })
      );
      expect(state.importing).toBe(false);
      expect(state.error).toBe('Format invalide');
    });
  });

  describe('Delete Player', () => {
    it('should set loading to true on deletePlayer', () => {
      const state = adminReducer(
        { ...initialState, players: [mockPlayer, mockPlayer2] },
        AdminActions.deletePlayer({ playerId: 1 })
      );
      expect(state.loading).toBe(true);
      expect(state.error).toBeNull();
      expect(state.successMessage).toBeNull();
    });

    it('should remove player on deletePlayerSuccess', () => {
      const state = adminReducer(
        { ...initialState, players: [mockPlayer, mockPlayer2], loading: true },
        AdminActions.deletePlayerSuccess({ playerId: 1 })
      );
      expect(state.loading).toBe(false);
      expect(state.players).toEqual([mockPlayer2]);
      expect(state.successMessage).toBe('Joueur supprimé avec succès');
    });

    it('should set error on deletePlayerFailure', () => {
      const state = adminReducer(
        { ...initialState, loading: true },
        AdminActions.deletePlayerFailure({ error: 'Joueur introuvable' })
      );
      expect(state.loading).toBe(false);
      expect(state.error).toBe('Joueur introuvable');
    });
  });

  describe('Clear Admin Messages', () => {
    it('should clear error and successMessage', () => {
      const state = adminReducer(
        { ...initialState, error: 'Erreur', successMessage: 'OK' },
        AdminActions.clearAdminMessages()
      );
      expect(state.error).toBeNull();
      expect(state.successMessage).toBeNull();
    });
  });

  describe('Reducer purity', () => {
    it('should not modify the original state', () => {
      const original: AdminState = { ...initialState };
      adminReducer(original, AdminActions.fetchAdminPlayersSuccess({ players: [mockPlayer] }));
      expect(original).toEqual(initialState);
    });
  });
});
