import { ChangeDetectionStrategy, Component } from '@angular/core';
import { SitePage } from './site-page';
import { POS_SIMULATOR_GUIDE } from '../data/pos-simulator.data';
import { PgScreen } from '../shared/simglass-pos/pos-glass';
import { SimGuide } from '../shared/simglass-pos/sim-guide';

/**
 * /simulator/pos — the POS Simulator guide. Content: data/pos-simulator.data.ts;
 * glass screens: shared/simglass-pos/pos-glass.ts; layout: shared/simglass-pos/sim-guide.ts.
 */
@Component({
  selector: 'page-docs-pos-simulator',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [SimGuide, PgScreen],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-pos-simulator' },
  template: `
    <sg-sim-guide [guide]="guide">
      <ng-template let-id let-active="active"><pg-screen [screen]="id" [active]="active" /></ng-template>
    </sg-sim-guide>
  `,
})
export class DocsPosSimulatorPage {
  protected readonly guide = POS_SIMULATOR_GUIDE;
}
