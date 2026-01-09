import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Player } from '../models/player.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class PlayerService {
  private readonly API_URL = `${environment.apiUrl || 'http://localhost:8080/api'}/players`;

  constructor(private http: HttpClient) {}

  /**
   * Récupère tous les joueurs
   */
  getAll(): Observable<Player[]> {
    return this.http.get<Player[]>(this.API_URL);
  }

  /**
   * Récupère un joueur par son ID
   */
  getById(id: number): Observable<Player> {
    return this.http.get<Player>(`${this.API_URL}/${id}`);
  }

  /**
   * Exporte un joueur
   */
  export(id: number): Observable<Player> {
    return this.http.get<Player>(`${this.API_URL}/${id}/export`);
  }

  /**
   * Crée un nouveau joueur
   */
  create(player: Player): Observable<Player> {
    return this.http.post<Player>(this.API_URL, player);
  }

  /**
   * Met à jour un joueur
   */
  update(id: number, player: Player): Observable<Player> {
    return this.http.put<Player>(`${this.API_URL}/${id}`, player);
  }

  /**
   * Supprime un joueur
   */
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }
}
