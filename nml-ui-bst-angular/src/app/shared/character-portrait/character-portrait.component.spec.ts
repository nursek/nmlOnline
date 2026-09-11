import { TestBed } from '@angular/core/testing';
import { CharacterPortraitComponent } from './character-portrait.component';
import { GameCharacter } from '../../models';

const character: GameCharacter = {
  id: 1,
  playerId: 1,
  name: 'Ratcatcher',
  baseAttack: 300,
  baseDefense: 250,
  basePdf: 100,
  basePdc: 0,
  baseArmor: 0,
  baseEvasion: 0,
  sectorNumber: 2,
};

describe('CharacterPortraitComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CharacterPortraitComponent],
    }).compileComponents();
  });

  function fixtureWith(interactive: boolean) {
    const fixture = TestBed.createComponent(CharacterPortraitComponent);
    fixture.componentRef.setInput('playerName', 'Nursek');
    fixture.componentRef.setInput('character', character);
    fixture.componentRef.setInput('sectorNumber', 2);
    fixture.componentRef.setInput('interactive', interactive);
    fixture.detectChanges();
    return fixture;
  }

  it("construit l'URL de l'illustration depuis le nom du compte, pas celui du personnage", () => {
    const img = fixtureWith(true).nativeElement.querySelector('img') as HTMLImageElement;

    expect(img.getAttribute('src')).toBe('assets/nursek/characters/portrait.png');
  });

  it("bascule sur le secours quand l'illustration est introuvable", () => {
    const fixture = fixtureWith(true);
    fixture.componentInstance.onImgError();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('img')).toBeNull();
    expect(fixture.nativeElement.querySelector('.cp-fallback').textContent).toContain('Ratcatcher');
  });

  it('émet activate au clic seulement quand le cadre est interactif', () => {
    const interactive = fixtureWith(true);
    const spy = jest.fn();
    interactive.componentInstance.activate.subscribe(spy);

    interactive.nativeElement.querySelector('.cp-frame').click();

    expect(spy).toHaveBeenCalledTimes(1);

    const staticFrame = fixtureWith(false);
    const staticSpy = jest.fn();
    staticFrame.componentInstance.activate.subscribe(staticSpy);

    staticFrame.nativeElement.querySelector('.cp-frame').click();

    expect(staticSpy).not.toHaveBeenCalled();
  });

  it('applique la classe de grande taille pour size="lg"', () => {
    const fixture = fixtureWith(false);
    fixture.componentRef.setInput('size', 'lg');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.cp-frame').classList).toContain('cp-lg');
  });
});
