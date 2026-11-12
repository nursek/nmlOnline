import type { Board } from './board.model';
import type { PageResult } from './index';

export interface RegistrationMode {
  enabled: boolean;
  inviteRequired: boolean;
}

export interface RegisterRequest {
  username: string;
  password: string;
  inviteCode?: string;
}

export interface AdminUser {
  id: number;
  username: string;
  role: string;
}

export interface BoardAdminEntry {
  userId: number;
  username: string | null;
}

export interface BoardPresetSummary {
  key: string;
  name: string;
  sectorCount: number;
  spotCount: number;
}

export interface BoardPreset {
  key: string;
  name: string;
  sectorCount: number;
  spots: number[][];
  board: Board;
}

export interface TemplatePlayerRequest {
  name: string;
  race: string;
  sectorNumbers: number[];
  characterName?: string;
}

export type AdminUserPage = PageResult<AdminUser>;
