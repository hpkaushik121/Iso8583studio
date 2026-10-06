import { ChangeDetectionStrategy, Component } from '@angular/core';
import { SitePage } from './site-page';
import { SimulatorPreview } from '../shared/preview';
import { ECR_PREVIEW } from '../data/simulator-preview.data';

/** /simulator/ecr — a concept guide: the simulator is still in development. */
@Component({
  selector: 'page-docs-ecr-simulator',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [SimulatorPreview],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-ecr-simulator' },
  template: `<app-simulator-preview [page]="page" />`,
})
export class DocsEcrSimulatorPage {
  protected readonly page = ECR_PREVIEW;
}
