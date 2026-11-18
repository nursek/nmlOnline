import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ReglesComponent } from './regles.component';

describe('ReglesComponent — recherche', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReglesComponent],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('filtre les sections, déplie le résultat et restaure tout à l’effacement', () => {
    const fixture = TestBed.createComponent(ReglesComponent);
    fixture.detectChanges();
    const composant = fixture.componentInstance;
    const racine = fixture.nativeElement as HTMLElement;
    const panneaux = racine.querySelectorAll('mat-expansion-panel');
    expect(panneaux.length).toBe(composant.sommaire.length);

    composant.changerRecherche('adamentium');
    fixture.detectChanges();

    expect(composant.sommaireFiltre().map((section) => section.id)).toEqual(['ressources']);
    expect(racine.querySelectorAll('.recherche-masquee').length).toBe(panneaux.length - 1);
    const ressources = racine.querySelector('#ressources') as HTMLElement;
    expect(ressources.classList.contains('recherche-masquee')).toBe(false);
    expect(ressources.classList.contains('mat-expanded')).toBe(true);

    composant.effacerRecherche();
    fixture.detectChanges();

    expect(composant.sommaireFiltre().length).toBe(composant.sommaire.length);
    expect(racine.querySelectorAll('.recherche-masquee').length).toBe(0);
    fixture.destroy();
  });
});
