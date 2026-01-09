export interface Equipment {
  id?: number;
  name: string;
  type: string;
  category?: string;
  description?: string;
  price: number;
  attack?: number;
  defense?: number;
  pdf?: number;
  pdc?: number;
  armor?: number;
  evasion?: number;
}
