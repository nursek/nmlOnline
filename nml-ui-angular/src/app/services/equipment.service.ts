import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Equipment } from '../models/equipment.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class EquipmentService {
  private readonly API_URL = `${environment.apiUrl || 'http://localhost:8080/api'}/equipment`;

  constructor(private http: HttpClient) {}

  /**
   * Récupère tous les équipements
   */
  getAll(): Observable<Equipment[]> {
    return this.http.get<Equipment[]>(this.API_URL);
  }

  /**
   * Récupère un équipement par son ID
   */
  getById(id: number): Observable<Equipment> {
    return this.http.get<Equipment>(`${this.API_URL}/${id}`);
  }

  /**
   * Crée un nouvel équipement
   */
  create(equipment: Equipment): Observable<Equipment> {
    return this.http.post<Equipment>(this.API_URL, equipment);
  }

  /**
   * Supprime un équipement
   */
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }
}
