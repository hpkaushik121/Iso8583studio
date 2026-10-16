import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { UiIcon } from '../../ui';

const CATEGORIES: [icon: string, name: string, count: string][] = [
  ['wifi-high', 'Payment Simulators', '9 tools'],
  ['credit-card', 'EMV & Card Tools', '12 tools'],
  ['key', 'Cryptographic Tools', '7 tools'],
  ['key', 'Key Management', '10 tools'],
  ['credit-card', 'Payment Utilities', '21 tools'],
  ['crop', 'Data Converters', '6 tools'],
];

const STATS: [icon: string, value: string, label: string][] = [
  ['wrench', '64', 'Tools'],
  ['wifi-high', '9', 'Simulators'],
  ['shield', '15+', 'HSM Vendors'],
  ['seal-check', '100%', 'ISO Compliant'],
];

const TOOLS: [icon: string, name: string, desc: string][] = [
  ['shield', 'HSM_SIMULATOR', 'Hardware Security Module simulation'],
  ['wifi-high', 'HOST_SIMULATOR', 'Payment host response simulation'],
  ['lock', 'DES_CALCULATOR', 'DES/3DES operations'],
  ['lock', 'EMV_41_CRYPTO', 'EMV 4.1 cryptogram validation'],
  ['lock', 'AES_CALCULATOR', 'AES encryption/decryption'],
  ['key', 'DUKPT_ISO_9797', 'Derived unique key per transaction (ISO 9797)'],
  ['share-network', 'PAYMENT_SWITCH', 'Transaction routing simulation'],
  ['code', 'BASE64_ENCODER', 'Base64 encoding/decoding'],
  ['credit-card', 'PIN_BLOCK_TOOL', 'ISO 9564 PIN block build and verify'],
  ['key', 'TR31_KEYBLOCK', 'Key block wrap and unwrap'],
  ['identification-card', 'APDU_SIMULATOR', 'Contact and contactless card dialogue'],
  ['crop', 'BITMAP_DECODER', 'Primary and secondary bitmap fields'],
];

/**
 * The ISO8583Studio home screen, rebuilt in DOM to fill the hero frame in
 * place of a screenshot: sidebar categories, the stat row, the Quick Access
 * grid and the card the payment journey later lifts out of it.
 *
 * It is authored on a 1160px canvas (600px once the frame is under 560px) and
 * every length is a multiple of --u, a container-query unit equal to one
 * design pixel at the frame's current width. So it scales with the frame in
 * CSS alone: nothing is measured, and the prerendered markup is already the
 * settled layout. Which blocks the narrow canvas drops, and how many tool rows
 * fit, are container queries in home.css.
 *
 * Its contents arrive layer by layer on load (.ds-layer / .ds-tile); `delay`
 * is when the first layer starts, in ms.
 */
@Component({
  selector: 'home-hero-dashboard',
  imports: [UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="hero-dash" role="img"
         aria-label="The ISO8583Studio home screen: tool categories, platform stats and quick access to the simulators and calculators">
      <div class="hd-in">
        <div class="hd-side ds-layer" [style.--d]="delay()">
          <div class="hd-brand">
            <span class="hd-logo"><ui-icon name="wifi-high" /></span>
            <span><span class="hd-brand-name">ISO8583Studio</span><span class="hd-brand-sub">Professional Payment Testing</span></span>
          </div>
          <div class="hd-label hd-side-label">Tool Categories</div>
          <div class="hd-cats">
            @for (c of categories; track c[1]; let first = $first) {
              <div class="hd-cat" [class.is-on]="first">
                <ui-icon [name]="c[0]" />
                <span><span class="hd-cat-name">{{ c[1] }}</span><span class="hd-cat-n">{{ c[2] }}</span></span>
              </div>
            }
          </div>
        </div>

        <div class="hd-main">
          <div class="hd-head ds-layer" [style.--d]="delay() + 80">
            <div class="hd-head-l">
              <span class="hd-logo hd-narrow"><ui-icon name="wifi-high" /></span>
              <div>
                <div class="hd-title">ISO8583 Studio</div>
                <div class="hd-tagline">Payment Testing &amp; Simulation Platform</div>
              </div>
            </div>
            <div class="hd-head-r">
              <div class="hd-search hd-wide"><ui-icon name="magnifying-glass" /><span>Search across all 64 tools…</span></div>
              <span class="hd-version">v1.0.14</span>
            </div>
          </div>

          <div class="hd-search hd-narrow ds-layer" [style.--d]="delay() + 160">
            <ui-icon name="magnifying-glass" /><span>Search across all 64 tools…</span>
          </div>

          <div class="hd-stats">
            @for (s of stats; track s[2]; let i = $index) {
              <div class="hd-stat hd-panel ds-tile" [style.--d]="delay() + 300 + i * 130">
                <div class="hd-stat-top"><span>{{ s[2] }}</span><ui-icon [name]="s[0]" /></div>
                <div class="hd-stat-value">{{ s[1] }}</div>
              </div>
            }
          </div>

          <div class="hd-quick hd-panel ds-rise" [style.--d]="delay() + 640">
            <div class="hd-quick-head ds-tile" [style.--d]="delay() + 700">
              <div>
                <div class="hd-quick-title">Quick Access</div>
                <div class="hd-quick-sub">Your most frequently used tools</div>
              </div>
              <span class="hd-label">64 tools</span>
            </div>
            <div class="hd-tools">
              @for (t of tools; track t[1]; let i = $index) {
                <div class="hd-tool ds-tile" [style.--d]="delay() + 800 + i * 60">
                  <div class="hd-tool-top">
                    <ui-icon [name]="t[0]" />
                    <span class="hd-tool-name">{{ t[1] }}</span>
                    <ui-icon class="hd-tool-go" name="arrow-up-right" />
                  </div>
                  <div class="hd-tool-desc">{{ t[2] }}</div>
                </div>
              }
            </div>
            <div class="hd-foot">
              <div class="hero-card-view ds-tile" [style.--d]="delay() + 1450">
                <div class="hero-card-face">
                  <div class="hd-card-row"><span class="hd-card-kind">DEBIT</span><ui-icon name="wifi-high" /></div>
                  <div class="hd-card-pan"><span class="hd-card-chip"></span><span>•••• 4242</span></div>
                  <div class="hd-card-row hd-card-row--end"><span class="hd-card-exp">09/29</span><ui-icon name="credit-card" /></div>
                </div>
                <span class="hero-departed-note"></span>
              </div>
              <div class="hd-sunk hd-note ds-tile" [style.--d]="delay() + 1500">
                <div class="hd-label hd-label--teal">Daily Wisdom</div>
                <div class="hd-note-text hd-note-text--quote">“The best payment experience is the one you don't notice.”</div>
              </div>
              <div class="hd-sunk hd-note hd-wide ds-tile" [style.--d]="delay() + 1600">
                <div class="hd-label">What's New</div>
                <div class="hd-note-text">v1.0.14 is out — check the latest additions.</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class HomeHeroDashboard {
  /** When the first layer starts to arrive, in ms after load. */
  readonly delay = input(2700);

  protected readonly categories = CATEGORIES;
  protected readonly stats = STATS;
  protected readonly tools = TOOLS;
}
