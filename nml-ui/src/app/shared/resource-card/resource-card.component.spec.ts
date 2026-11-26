import { TestBed } from '@angular/core/testing';
import { ResourceCardComponent } from './resource-card.component';
import type { PlayerResource } from '../../models';

describe('ResourceCardComponent', () => {
  function build(resource: PlayerResource): ResourceCardComponent {
    const fixture = TestBed.createComponent(ResourceCardComponent);
    fixture.componentRef.setInput('resource', resource);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [ResourceCardComponent] });
  });

  it('calcule la vente plafonnée à 9 et sa valeur', () => {
    const page = build({ id: 1, name: 'Auramite', quantity: 20, baseValue: 1700 });

    expect(page.saleQuantity()).toBe(9);
    expect(page.totalValue()).toBe(76_500);
    expect(page.saleLabel()).toContain('max 9');
    expect(page.image().src).toContain('auramite');
  });

  it('signale une image cassée', () => {
    const page = build({ id: 1, name: 'Bois', quantity: 2, baseValue: 2 });

    expect(page.imgError()).toBe(false);
    page.onImgError();
    expect(page.imgError()).toBe(true);
  });
});
