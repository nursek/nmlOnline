import { TestBed } from '@angular/core/testing';
import { PlayerPhotoComponent } from './player-photo.component';

describe('PlayerPhotoComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [PlayerPhotoComponent] }).compileComponents();
  });

  it('utilise le portrait du joueur quand le nom est un slug valide', () => {
    const fixture = TestBed.createComponent(PlayerPhotoComponent);
    fixture.componentRef.setInput('playerName', 'nursek');
    fixture.componentRef.setInput('caption', 'Portrait de nursek');
    fixture.detectChanges();

    const img = fixture.nativeElement.querySelector('img') as HTMLImageElement;
    expect(img).not.toBeNull();
    expect(img.getAttribute('src')).toContain('/assets/_opt/nursek/characters/portrait.320.webp');
    expect(img.getAttribute('alt')).toBe('Portrait de nursek');
    expect(fixture.nativeElement.querySelector('figcaption')?.textContent).toContain(
      'Portrait de nursek',
    );
  });

  it("bascule sur la plaque d'archives si l'image échoue", () => {
    const fixture = TestBed.createComponent(PlayerPhotoComponent);
    fixture.componentRef.setInput('playerName', 'nursek');
    fixture.detectChanges();
    fixture.componentInstance.imgError.set(true);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('img')).toBeNull();
    expect(fixture.nativeElement.querySelector('.fallback')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.archive')?.textContent).toContain(
      "Photo d'archives",
    );
  });

  it('refuse un nom qui ne peut pas être un dossier d’assets', () => {
    const fixture = TestBed.createComponent(PlayerPhotoComponent);
    fixture.componentRef.setInput('playerName', '../evil');
    fixture.detectChanges();

    expect(fixture.componentInstance.portrait()).toBeNull();
    expect(fixture.nativeElement.querySelector('img')).toBeNull();
    expect(fixture.nativeElement.querySelector('.fallback')).not.toBeNull();
  });
});
