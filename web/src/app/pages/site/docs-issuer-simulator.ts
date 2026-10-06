import { ChangeDetectionStrategy, Component } from '@angular/core';
import { SitePage } from './site-page';
import { SimulatorPreview } from '../shared/preview';
import { ISSUER_PREVIEW } from '../data/simulator-preview.data';

/** /simulator/issuer — a concept guide: the simulator is still in development. */
@Component({
  selector: 'page-docs-issuer-simulator',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [SimulatorPreview],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-issuer-simulator' },
  template: `<app-simulator-preview [page]="page" />`,
})
export class DocsIssuerSimulatorPage {
  protected readonly page = ISSUER_PREVIEW;
}
