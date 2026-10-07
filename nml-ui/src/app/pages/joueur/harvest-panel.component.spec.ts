import { TestBed } from '@angular/core/testing';
import { HarvestPanelComponent } from './harvest-panel.component';
import type { SectorHarvestState } from './joueur.helpers';
import type { Sector } from '../../models';

function state(number: number, resource: string | null): SectorHarvestState {
  return {
    sector: { number, name: `Secteur ${number}` } as unknown as Sector,
    action: null,
    choice: null,
    money: 100,
    resource,
  };
}

describe('HarvestPanelComponent — récolte en lot', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [HarvestPanelComponent] }).compileComponents();
  });

  it('« Tout en ressource » ignore les secteurs sans ressource', () => {
    const fixture = TestBed.createComponent(HarvestPanelComponent);
    fixture.componentRef.setInput('states', [state(1, 'Or'), state(2, null), state(3, 'Ivoire')]);
    const emitted: number[][] = [];
    fixture.componentInstance.harvest.subscribe((event) => emitted.push(event.sectorNumbers));

    fixture.componentInstance.harvestAll('HARVEST_RESOURCE');

    expect(emitted).toEqual([[1, 3]]);
    expect(fixture.componentInstance.pendingWithResource().length).toBe(2);
  });
});
