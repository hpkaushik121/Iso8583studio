import { ChangeDetectionStrategy, Component, afterNextRender, signal } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { inject } from '@angular/core';
import { UiButton, UiCard, UiIconTile } from '../../ui';

interface Destination {
  label: string;
  link: string;
  glyph: string;
  desc: string;
}

/**
 * The 404. Prerendered to /404.html and also served for any unmatched client
 * route, so it has to read correctly both as a standalone document and as an
 * in-app navigation.
 */
@Component({
  selector: 'app-not-found',
  imports: [UiButton, UiCard, UiIconTile],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`
    /* Short pages otherwise leave the footer floating mid-viewport. */
    .nf {
      min-height: calc(100svh - var(--nav-h) - var(--sp-20));
      display: grid;
      /* minmax(0, …) so the nowrap request line cannot widen the whole page. */
      grid-template-columns: minmax(0, 1fr);
      align-content: center;
      text-align: center;
    }

    /* Grid items with auto margins are sized to fit-content, so these two
       need an explicit width before max-width can centre them. */
    .nf-head { width: 100%; max-width: 640px; margin-inline: auto; }
    .nf-kicker { justify-content: center; }
    .nf-title { margin-bottom: var(--sp-4); }
    .nf-desc { margin-inline: auto; margin-bottom: var(--sp-8); }

    /* The request that failed, in the same register as the app's own logs. */
    .nf-request {
      display: flex;
      align-items: center;
      gap: var(--sp-3);
      width: 100%;
      max-width: 560px;
      min-width: 0;
      margin: 0 auto var(--sp-8);
      padding: var(--sp-3) var(--sp-4);
      background: var(--card-deep);
      border: 1px solid var(--line);
      border-radius: var(--r-md);
      font-family: var(--mono);
      font-size: var(--fs-sm);
      text-align: left;
    }

    .nf-method { color: var(--teal-hi); flex: none; }

    .nf-path {
      color: var(--text);
      flex: 1;
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .nf-status {
      flex: none;
      padding: 2px var(--sp-2);
      border-radius: var(--r-sm);
      background: rgba(229, 72, 77, .12);
      color: var(--red-hi);
      font-size: var(--fs-badge);
      font-weight: 600;
      letter-spacing: .04em;
    }

    .nf-actions {
      display: flex;
      gap: var(--sp-3);
      justify-content: center;
      flex-wrap: wrap;
      margin-bottom: var(--sp-16);
    }

    .nf-label {
      justify-content: center;
      margin-bottom: var(--sp-5);
    }

    /* Fixed column counts rather than auto-fit: four destinations wrap to a
       lone orphan card on any width that fits three. */
    .nf-links {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      /* Equal rows, so a two-line title does not make one card taller. */
      grid-auto-rows: 1fr;
      gap: var(--sp-4);
      text-align: left;
    }

    @media (min-width: 1000px) {
      .nf-links { grid-template-columns: repeat(4, minmax(0, 1fr)); }
    }

    @media (max-width: 560px) {
      .nf-links { grid-template-columns: minmax(0, 1fr); }
    }

    .nf-card-head {
      display: flex;
      align-items: center;
      gap: var(--sp-3);
      margin-bottom: var(--sp-3);
    }

    @media (max-width: 768px) {
      .nf { min-height: 0; }
      .nf-actions { margin-bottom: var(--sp-12); }
    }
  `],
  template: `
    <div class="doc-body nf">
      <div class="nf-head">
        <p class="kicker nf-kicker">Error 404</p>
        <h1 class="page-title nf-title">Page not found</h1>
        <p class="page-description nf-desc">
          Nothing on this site answers to that address. It may have been renamed,
          moved into the documentation, or never existed at all.
        </p>
      </div>

      <p class="nf-request">
        <span class="nf-method">GET</span>
        <span class="nf-path">{{ path() }}</span>
        <span class="nf-status">404</span>
      </p>

      <div class="nf-actions">
        <ui-button routerLink="/docs">Browse the docs</ui-button>
        <ui-button variant="ghost" routerLink="/">Go home</ui-button>
      </div>

      <p class="kicker nf-label">Popular destinations</p>
      <div class="nf-links">
        @for (d of destinations; track d.link) {
          <ui-card variant="tile" [routerLink]="d.link">
            <span class="nf-card-head">
              <ui-icon-tile [glyph]="d.glyph" />
              <span class="ui-card-title" style="margin-bottom:0">{{ d.label }}</span>
            </span>
            <span class="ui-card-desc" style="margin-bottom:0">{{ d.desc }}</span>
          </ui-card>
        }
      </div>
    </div>
  `,
})
export class NotFound {
  private readonly doc = inject(DOCUMENT);

  /** Filled in after hydration so the prerendered 404.html reports the address
   *  the visitor actually asked for, not the /404 it was rendered at. */
  protected readonly path = signal('/404');

  protected readonly destinations: Destination[] = [
    { label: 'Documentation', link: '/docs', glyph: '▤', desc: 'Install, configure and run every module.' },
    { label: 'Payment Simulators', link: '/simulator', glyph: '⇄', desc: 'Host, HSM, POS, ATM and switch endpoints.' },
    { label: 'EMV & Card Tools', link: '/tools/emv-tools', glyph: '▣', desc: 'Cryptograms, SDA/DDA, ATR, tags and CVV.' },
    { label: 'Blog', link: '/blogs', glyph: '✎', desc: 'Guides on testing, cryptography and EMV.' },
  ];

  constructor() {
    afterNextRender(() => {
      const loc = this.doc.defaultView?.location;
      if (loc) this.path.set(loc.pathname + loc.search);
    });
  }
}
