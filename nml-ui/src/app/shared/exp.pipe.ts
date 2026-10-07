import { Pipe, PipeTransform } from '@angular/core';

/** Expérience : « 8.5 » si la décimale existe, sinon « 9 » (jamais « 9.0 »). */
@Pipe({ name: 'exp' })
export class ExpPipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    // Arrondi au dixième d'abord : pare les artefacts flottants (2.0000000000000004 → « 2 »).
    const rounded = Math.round((value ?? 0) * 10) / 10;
    return Number.isInteger(rounded) ? String(rounded) : rounded.toFixed(1);
  }
}
