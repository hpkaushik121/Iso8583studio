import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EXTERNAL } from '../../../core/site-nav';
import { UiCtaPanel, UiIcon, UiReveal, UiWords } from '../../../ui';

/**
 * GuideCta — the closing download panel of a guide, built on <ui-cta-panel>
 * (which renders <section class="cta cta-panel" data-sect="final_cta">, the
 * hook for final_cta_click).
 *
 *   <app-guide-cta heading="Try it on your own transactions"
 *                  text="Free and open source. Download the studio and run these calculators on your desk in minutes." />
 *
 * What it renders, inside <div class="gd-cta" id="download">:
 *   · the heading (h2) and text;
 *   · a primary anchor to EXTERNAL.releases ("Download Studio", arrow-up-right) — the href the
 *     download events and the Pro interstitial key on;
 *   · a secondary anchor (default "Installation guide" → /docs/installation);
 *   · unless [pro]="false", one "Register for Pro" link to /pro inside a .pro-nudge block, so
 *     the click reports pro_click. It sits beside the copy when the main column is 900px or
 *     wider and as a strip underneath otherwise.
 *
 * Inputs
 *   heading         string, required
 *   text            string, required
 *   anchor          string            id of the wrapper, so the rail can link to it (default 'download')
 *   secondaryLabel  string            default 'Installation guide'
 *   secondaryLink   string            default '/docs/installation'
 *   pro             boolean           default true
 *   proText         string            the sentence in the Pro block
 *
 * The wrapper is a <div>, not a <section>: the panel inside is the section, and sections must
 * not nest. It reports section_view as "final_cta".
 */
@Component({
  selector: 'app-guide-cta',
  imports: [RouterLink, UiCtaPanel, UiIcon, UiReveal, UiWords],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'gd-cta-host' },
  template: `
    <div class="gd-cta" [class.gd-cta--solo]="!pro()" [id]="anchor()" uiReveal [uiRevealDelay]="200">
      <ui-cta-panel>
        <div class="gd-cta-grid">
          <div class="gd-cta-main">
            <h2><ui-words [text]="heading()" /></h2>
            <p class="gd-cta-text ds-hold" [style.--d]="350">{{ text() }}</p>
            <div class="cta-actions ds-hold" [style.--d]="450">
              <a class="btn btn--primary btn--glow" [href]="releases">Download Studio<ui-icon name="arrow-up-right" [size]="16" /></a>
              <a class="btn btn--secondary" [routerLink]="secondaryLink()">{{ secondaryLabel() }}<ui-icon name="arrow-right" [size]="16" /></a>
            </div>
          </div>
          @if (pro()) {
            <div class="pro-nudge gd-cta-pro ds-hold" [style.--d]="220">
              <span class="gd-cta-pro-tag">✦ Pro</span>
              <p class="gd-cta-pro-text">{{ proText() }}</p>
              <a class="gd-cta-pro-link" routerLink="/pro">Register for Pro<ui-icon name="arrow-right" [size]="14" /></a>
            </div>
          }
        </div>
      </ui-cta-panel>
    </div>
  `,
})
export class GuideCta {
  readonly heading = input.required<string>();
  readonly text = input.required<string>();
  readonly anchor = input('download');
  readonly secondaryLabel = input('Installation guide');
  readonly secondaryLink = input('/docs/installation');
  readonly pro = input(true);
  readonly proText = input('Testing with a team, or certifying with a scheme? Pro raises the CPS ceiling, unlocks the full algorithm set and deep simulator tweaks, plus hosted endpoints and priority support.');

  protected readonly releases = EXTERNAL.releases;
}
