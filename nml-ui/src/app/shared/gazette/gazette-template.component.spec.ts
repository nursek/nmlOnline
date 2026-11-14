import { TestBed } from '@angular/core/testing';
import { GazetteTemplateComponent } from './gazette-template.component';
import { GAZETTE_ISSUE } from './gazette.fixture';

describe('GazetteTemplateComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GazetteTemplateComponent],
    }).compileComponents();
  });

  it('rend le masthead, les unes, les brèves et le pied de page', () => {
    const fixture = TestBed.createComponent(GazetteTemplateComponent);
    fixture.componentRef.setInput('issue', GAZETTE_ISSUE);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('.journal-name')?.textContent).toContain('Brigade des Morts');
    expect(element.querySelector('.weather')?.textContent).toContain('Ciel de plomb');

    const fakeUrl = element.querySelector('.fake-url');
    expect(fakeUrl?.tagName).toBe('SPAN');
    expect(fakeUrl?.textContent).toContain('www.brigade-des-morts.nml/vol-5/n-2');

    expect(element.querySelector('.lead .headline')?.textContent).toContain('LA CHUTE DE LURIO');
    expect(element.querySelector('.lead .quote')?.textContent).toContain('Le QG est tombé');
    expect(element.querySelector('.lead .byline')?.textContent).toContain('Frère Caïn');
    expect(element.querySelectorAll('.lead .body p').length).toBe(2);
    expect(element.querySelectorAll('.secondary').length).toBe(1);
    expect(element.querySelectorAll('.brief').length).toBe(1);
    expect(element.textContent).toContain('Publié le');
    expect(element.querySelector('a')).toBeNull();
  });

  it('signale un brouillon non publié', () => {
    const fixture = TestBed.createComponent(GazetteTemplateComponent);
    fixture.componentRef.setInput('issue', { ...GAZETTE_ISSUE, publishedAt: null });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Brouillon non publié');
  });
});
