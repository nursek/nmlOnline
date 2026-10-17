import { readFileSync } from 'node:fs';
import { join } from 'node:path';

// Sans inlineCritical:false, Beasties défère le CSS complet derrière un script inline que la CSP prod bloque (thème mort).
describe('angular.json (build prod)', () => {
  it('désactive inlineCritical', () => {
    const json = JSON.parse(readFileSync(join(__dirname, '../../../angular.json'), 'utf8'));
    const production = json.projects['nml-ui-copilot-angular'].architect.build.configurations.production;
    expect(production.optimization.styles.inlineCritical).toBe(false);
  });
});
