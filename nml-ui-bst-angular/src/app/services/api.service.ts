import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthResponse, LoginRequest, Player, Equipment, Board, RefreshResponse, ResourceSaleResponse, GameCharacter, Building, VehicleTypeInfo, BuyEquipmentItem, Vehicle } from '../models';
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

  moveBuilding(buildingId: number, newSectorNumber: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/buildings/${buildingId}/move`, {
      newSectorNumber
    });
  }

  // Admin endpoints
  adminGetAllPlayers(): Observable<Player[]> {
    return this.http.get<Player[]>(`${this.baseUrl}/admin/players`);
  }

  adminExportPlayer(id: number): Observable<Record<string, unknown>> {
    return this.http.get<Record<string, unknown>>(`${this.baseUrl}/admin/players/${id}/export`);
  }

  adminImportPlayer(file: File): Observable<Player> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Player>(`${this.baseUrl}/admin/players/import`, formData);
  }

  adminDeletePlayer(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/admin/players/${id}`);
  }

  // Véhicules
  getVehicleTypes(): Observable<VehicleTypeInfo[]> {
    return this.http.get<VehicleTypeInfo[]>(`${this.baseUrl}/vehicles/types`);
  }

  getPlayerVehicles(): Observable<Vehicle[]> {
    return this.http.get<Vehicle[]>(`${this.baseUrl}/vehicles/my`);
  }

  buyVehicle(vehicleTypeName: string, quantity: number = 1): Observable<Vehicle[]> {
    return this.http.post<Vehicle[]>(`${this.baseUrl}/vehicles/buy`, { vehicleType: vehicleTypeName, quantity });
  }

  placeVehicle(vehicleId: number, boardId: number, sectorNumber: number): Observable<Vehicle> {
    return this.http.post<Vehicle>(`${this.baseUrl}/vehicles/${vehicleId}/place`, { boardId, sectorNumber });
  }

  buyEquipments(items: BuyEquipmentItem[]): Observable<Player> {
    return this.http.post<Player>(`${this.baseUrl}/players/equipment/buy`, items);
  }
}
