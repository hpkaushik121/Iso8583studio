import { ChangeDetectionStrategy, Component } from '@angular/core';
import { SitePage } from './site-page';
import { SimulatorPreview } from '../shared/preview';
import { SWITCH_PREVIEW } from '../data/simulator-preview.data';

/** /simulator/payment-switch — a concept guide: the simulator is still in development. */
@Component({
  selector: 'page-docs-payment-switch',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [SimulatorPreview],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-payment-switch' },
  template: `<app-simulator-preview [page]="page" />`,
})
export class DocsPaymentSwitchPage {
  protected readonly page = SWITCH_PREVIEW;
}
