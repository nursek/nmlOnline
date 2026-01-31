import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthResponse, LoginRequest, Player, Equipment, Board, RefreshResponse } from '../models';
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
}
