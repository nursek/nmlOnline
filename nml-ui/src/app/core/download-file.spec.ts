import { downloadJson, downloadText } from './download-file';

describe('downloadFile', () => {
  const click = jest.fn();
  const createObjectURL = jest.fn(() => 'blob:test');
  const revokeObjectURL = jest.fn();

  beforeEach(() => {
    click.mockClear();
    createObjectURL.mockClear();
    revokeObjectURL.mockClear();
    jest.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(click);
    Object.defineProperty(URL, 'createObjectURL', { value: createObjectURL, configurable: true });
    Object.defineProperty(URL, 'revokeObjectURL', { value: revokeObjectURL, configurable: true });
  });

  afterEach(() => jest.restoreAllMocks());

  it('télécharge un JSON et libère l’URL blob', () => {
    downloadJson('tour.json', { tour: 3 });

    expect(createObjectURL).toHaveBeenCalledWith(
      expect.objectContaining({ type: 'application/json' }),
    );
    expect(click).toHaveBeenCalledTimes(1);
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:test');
  });

  it('télécharge un texte brut', () => {
    downloadText('tour.log', 'ligne 1');

    expect(createObjectURL).toHaveBeenCalledWith(
      expect.objectContaining({ type: 'text/plain;charset=utf-8' }),
    );
    expect(click).toHaveBeenCalledTimes(1);
  });
});
