import { ExpPipe } from './exp.pipe';

describe('ExpPipe', () => {
  const pipe = new ExpPipe();

  it("n'affiche la décimale que si elle existe", () => {
    expect(pipe.transform(0)).toBe('0');
    expect(pipe.transform(9)).toBe('9');
    expect(pipe.transform(8.5)).toBe('8.5');
    expect(pipe.transform(8.55)).toBe('8.6');
  });

  it('absorbe les artefacts flottants et les valeurs nulles', () => {
    expect(pipe.transform(2.0000000000000004)).toBe('2');
    expect(pipe.transform(null)).toBe('0');
    expect(pipe.transform(undefined)).toBe('0');
  });
});
