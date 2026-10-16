import { ChangeDetectionStrategy, Component } from '@angular/core';
import { SitePage } from './site-page';
import { APDU_SIMULATOR_GUIDE } from '../data/apdu-simulator.data';
import { AgScreen } from '../shared/simglass-pos/apdu-glass-cfg';
import { SimGuide } from '../shared/simglass-pos/sim-guide';

/**
 * /simulator/apdu — the APDU Simulator guide. Content: data/apdu-simulator.data.ts;
 * glass screens: shared/simglass-pos/apdu-glass.ts and apdu-glass-cfg.ts;
 * layout: shared/simglass-pos/sim-guide.ts.
 */
@Component({
  selector: 'page-docs-apdu-simulator',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [SimGuide, AgScreen],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-apdu-simulator' },
  template: `
    <sg-sim-guide [guide]="guide">
      <ng-template let-id let-active="active"><ag-screen [screen]="id" [active]="active" /></ng-template>
    </sg-sim-guide>
  `,
})
export class DocsApduSimulatorPage {
  protected readonly guide = APDU_SIMULATOR_GUIDE;
}
