import {
  selectAdminState,
  selectAdminPlayers,
  selectAdminLoading,
  selectAdminImporting,
  selectAdminError,
  selectAdminSuccessMessage,
} from './admin.selectors';
import { AdminState } from './admin.reducer';
import { Player } from '../../models';

describe('Admin Selectors', () => {
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

  const populatedState: { admin: AdminState } = {
    admin: {
      players: [mockPlayer],
      loading: false,
      importing: false,
      error: null,
      successMessage: 'Joueur importé',
    },
  };

  const loadingState: { admin: AdminState } = {
    admin: {
      players: [],
      loading: true,
      importing: false,
      error: null,
      successMessage: null,
    },
  };

  const importingState: { admin: AdminState } = {
    admin: {
      players: [],
      loading: false,
      importing: true,
      error: null,
      successMessage: null,
    },
  };

  const errorState: { admin: AdminState } = {
    admin: {
      players: [],
      loading: false,
      importing: false,
      error: 'Erreur serveur',
      successMessage: null,
    },
  };

  it('should select admin state', () => {
    expect(selectAdminState(populatedState as any)).toEqual(populatedState.admin);
  });

  it('should select players', () => {
    expect(selectAdminPlayers(populatedState as any)).toEqual([mockPlayer]);
    expect(selectAdminPlayers(loadingState as any)).toEqual([]);
  });

  it('should select loading', () => {
    expect(selectAdminLoading(loadingState as any)).toBe(true);
    expect(selectAdminLoading(populatedState as any)).toBe(false);
  });

  it('should select importing', () => {
    expect(selectAdminImporting(importingState as any)).toBe(true);
    expect(selectAdminImporting(populatedState as any)).toBe(false);
  });

  it('should select error', () => {
    expect(selectAdminError(errorState as any)).toBe('Erreur serveur');
    expect(selectAdminError(populatedState as any)).toBeNull();
  });

  it('should select successMessage', () => {
    expect(selectAdminSuccessMessage(populatedState as any)).toBe('Joueur importé');
    expect(selectAdminSuccessMessage(errorState as any)).toBeNull();
  });
});
