import { ChangeDetectionStrategy, Component } from '@angular/core';
import { SitePage } from './site-page';
import { SimulatorPreview } from '../shared/preview';
import { ATM_PREVIEW } from '../data/simulator-preview.data';

/** /simulator/atm — a concept guide: the simulator is still in development. */
@Component({
  selector: 'page-docs-atm-simulator',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [SimulatorPreview],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-atm-simulator' },
  template: `<app-simulator-preview [page]="page" />`,
})
export class DocsAtmSimulatorPage {
  protected readonly page = ATM_PREVIEW;
}
