import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthResponse, LoginRequest, Player, Equipment, Board, RefreshResponse, ResourceSaleResponse, GameCharacter, Building } from '../models';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  // Auth endpoints
  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/login`, credentials, { withCredentials: true });
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/auth/logout`, {}, { withCredentials: true });
  }

  refreshToken(): Observable<RefreshResponse> {
    return this.http.post<RefreshResponse>(`${this.baseUrl}/auth/refresh`, {}, { withCredentials: true });
  }

  // Player endpoints
  getPlayer(username: string): Observable<Player> {
    return this.http.get<Player>(`${this.baseUrl}/players/${username}`);
  }

  getAllPlayers(): Observable<Player[]> {
    return this.http.get<Player[]>(`${this.baseUrl}/players`);
  }

  // Equipment endpoints
  getEquipments(): Observable<Equipment[]> {
    return this.http.get<Equipment[]>(`${this.baseUrl}/equipment`);
  }

  // Board endpoints
  getAllBoards(): Observable<Board[]> {
    return this.http.get<Board[]>(`${this.baseUrl}/boards`);
  }

  getBoardById(id: number): Observable<Board> {
    return this.http.get<Board>(`${this.baseUrl}/boards/${id}`);
  }

  getBoardByName(name: string): Observable<Board> {
    return this.http.get<Board>(`${this.baseUrl}/boards/name/${name}`);
  }

  // Resource endpoints
  sellResource(resourceId: number, quantity: number): Observable<ResourceSaleResponse> {
    return this.http.post<ResourceSaleResponse>(
      `${this.baseUrl}/players/resources/sell/${resourceId}`,
      null,
      { params: { quantity: quantity.toString() } }
    );
  }

  // === Character endpoints ===

  getCharacterByPlayerId(playerId: number): Observable<GameCharacter> {
    return this.http.get<GameCharacter>(`${this.baseUrl}/characters/player/${playerId}`);
  }

  getCharacterByName(name: string): Observable<GameCharacter> {
    return this.http.get<GameCharacter>(`${this.baseUrl}/characters/name/${name}`);
  }

  // === Building endpoints ===

  getHeadquarters(playerId: number): Observable<Building> {
    return this.http.get<Building>(`${this.baseUrl}/buildings/headquarters/${playerId}`);
  }

  getBank(playerId: number): Observable<Building> {
    return this.http.get<Building>(`${this.baseUrl}/buildings/bank/${playerId}`);
  }

  getWeaponCaches(playerId: number): Observable<Building[]> {
    return this.http.get<Building[]>(`${this.baseUrl}/buildings/weapon-caches/${playerId}`);
  }

  isHeadquartersOperational(playerId: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/buildings/headquarters/${playerId}/operational`);
  }

  reconstructHeadquartersSameLocation(playerId: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/buildings/headquarters/${playerId}/reconstruct-same`, {});
  }

  moveBuilding(buildingId: number, newSectorNumber: number, currentTurn: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/buildings/${buildingId}/move`, {
      newSectorNumber,
      currentTurn
    });
  }
}
