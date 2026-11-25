import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { CharacterPanelComponent } from './character-panel.component';
import { CharacterAbilitiesDialogComponent } from './character-abilities-dialog.component';
import type { GameCharacter, Sector, Unit, Vehicle } from '../../models';

describe('CharacterPanelComponent', () => {
  const dialog = { open: jest.fn() };

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      imports: [CharacterPanelComponent],
      providers: [{ provide: MatDialog, useValue: dialog }],
    });
  });

  function build(sector: Sector | null, character: GameCharacter | null): CharacterPanelComponent {
    const fixture = TestBed.createComponent(CharacterPanelComponent);
    fixture.componentRef.setInput('playerName', 'lurio');
    fixture.componentRef.setInput('character', character);
    fixture.componentRef.setInput('sector', sector);
    fixture.componentRef.setInput('playerId', 1);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  const unit = {
    id: 1,
    number: 1,
    type: { name: 'LARBIN' },
    classes: [{ name: 'LEGER' }],
    attack: 10,
    defense: 10,
    pdf: 0,
    pdc: 0,
    armor: 0,
    evasion: 0,
    experience: 0,
    equipments: [],
    isInjured: false,
  } as unknown as Unit;
  const vehicle = {
    id: 5,
    displayName: 'VTT léger',
    pilotName: 'lurio',
    isDestroyed: false,
  } as Vehicle;
  const sector = {
    number: 5,
    name: 'Secteur 5',
    army: [unit],
    buildings: [],
    characters: [],
    vehicles: [vehicle],
  } as unknown as Sector;
  const character = { id: 1, name: 'Lurio', baseAttack: 50 } as GameCharacter;

  it('résume les forces du secteur et masque le bloc vide', () => {
    const empty = build({ ...sector, army: [], vehicles: [] }, character);
    expect(empty.isEmpty()).toBe(true);
    expect(empty.troops()).toEqual([]);

    const page = build(sector, character);
    expect(page.isEmpty()).toBe(false);
    expect(page.troops()).toHaveLength(1);
    expect(page.totals().length).toBeGreaterThan(0);
    expect(page.vehicleChips()).toEqual([
      { id: 5, label: 'VTT léger n°1', pilotName: 'lurio' },
    ]);
  });

  it('ouvre le dialogue des capacités du personnage', () => {
    const page = build(sector, character);

    page.openAbilities();

    expect(dialog.open).toHaveBeenCalledWith(
      CharacterAbilitiesDialogComponent,
      expect.objectContaining({ data: { playerName: 'lurio', character } }),
    );
  });

  it('n’ouvre rien sans personnage', () => {
    build(sector, null).openAbilities();
    expect(dialog.open).not.toHaveBeenCalled();
  });
});
