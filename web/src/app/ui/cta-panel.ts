import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * The closing call-to-action band: a raised rounded surface with two dotted
 * planets bleeding off its edges.
 *
 * It renders <section class="cta">, which is what analytics keys
 * final_cta_click and the 'final_cta' download location on, so every page's
 * closing CTA goes through this component. Links placed inside must be
 * anchors, not buttons — the click listener only reports anchors.
 */
@Component({
  selector: 'ui-cta-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { style: 'display: block' },
  template: `
    <section class="cta cta-panel" data-sect="final_cta">
      <div class="cta-planet cta-planet--left" aria-hidden="true"></div>
      <div class="cta-planet cta-planet--right" aria-hidden="true"></div>
      <div class="cta-panel-in"><ng-content /></div>
    </section>
  `,
})
export class UiCtaPanel {}
