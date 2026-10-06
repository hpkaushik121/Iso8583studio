import { ChangeDetectionStrategy, Component, afterNextRender, signal } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { UiButton, UiIcon } from '../../ui';

interface Destination {
  label: string;
  link: string;
  icon: string;
  desc: string;
}

/**
 * The 404. Prerendered to /404.html and also served for any unmatched client
 * route, so it has to read correctly both as a standalone document and as an
 * in-app navigation.
 *
 * Its routes load no stylesheet bundle, so everything it needs beyond the
 * global primitives (.btn, .card) is in the component styles.
 */
@Component({
  selector: 'app-not-found',
  imports: [RouterLink, UiButton, UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`
    /* Short pages otherwise leave the footer floating mid-viewport. */
    .nf {
      position: relative;
      display: grid;
      /* minmax(0, …) so the nowrap request line cannot widen the whole page. */
      grid-template-columns: minmax(0, 1fr);
      align-content: center;
      justify-items: center;
      min-height: 100svh;
      max-width: var(--wrap-max);
      margin: 0 auto;
      padding: calc(var(--nav-h) + var(--sp-16)) var(--gutter) var(--sp-20);
      text-align: center;
    }

    /* The status, in the register of the app's own logs. */
    .nf-code {
      margin: 0;
      font-family: var(--mono);
      font-size: clamp(96px, 20vw, 184px);
      font-weight: 500;
      line-height: .9;
      letter-spacing: -.04em;
      background: linear-gradient(180deg, var(--text) 10%, rgba(151, 168, 214, .14) 96%);
      -webkit-background-clip: text;
      background-clip: text;
      color: transparent;
    }

    .nf-title {
      margin: var(--sp-6) 0 0;
      font-size: var(--fs-3xl);
      font-weight: 500;
      letter-spacing: var(--ls-tight);
      line-height: 1.12;
      color: var(--text);
    }

    .nf-desc {
      max-width: 520px;
      margin: var(--sp-4) 0 0;
      color: var(--muted);
      font-size: var(--fs-md);
      line-height: var(--lh-snug);
      text-wrap: pretty;
    }

    /* The request that failed. */
    .nf-request {
      display: flex;
      align-items: center;
      gap: var(--sp-3);
      width: 100%;
      max-width: 560px;
      min-width: 0;
      margin: var(--sp-8) 0 0;
      padding: var(--sp-3) var(--sp-4);
      border: 1px solid var(--line);
      border-radius: var(--r-lg);
      background: var(--card-deep);
      box-shadow: var(--sh-glass);
      font-family: var(--mono);
      font-size: var(--fs-sm);
      text-align: left;
    }

    .nf-method { flex: none; color: var(--teal-hi); }

    .nf-path {
      flex: 1;
      min-width: 0;
      overflow: hidden;
      color: var(--text);
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .nf-actions {
      display: flex;
      flex-wrap: wrap;
      justify-content: center;
      gap: 10px;
      margin-top: var(--sp-8);
    }

    .nf-label {
      margin: var(--sp-16) 0 var(--sp-5);
      font-family: var(--mono);
      font-size: var(--fs-xs);
      letter-spacing: var(--ls-wide);
      text-transform: uppercase;
      color: var(--faint);
    }

    /* Fixed column counts rather than auto-fit: four destinations wrap to a
       lone orphan card on any width that fits three. */
    .nf-links {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 14px;
      width: 100%;
      text-align: left;
    }

    .nf-card { display: grid; gap: 10px; align-content: start; height: 100%; }
    .nf-card .ui-icon { color: var(--blue-hi); }
    .nf-card-title { font-size: var(--fs-lg); font-weight: 500; letter-spacing: var(--ls-snug); color: var(--text); }
    .nf-card-desc { color: var(--muted); font-size: var(--fs-sm); line-height: 1.6; text-wrap: pretty; }

    @media (max-width: 999px) {
      .nf-links { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    }

    @media (max-width: 560px) {
      .nf { min-height: 0; padding-bottom: var(--sp-16); }
      .nf-label { margin-top: var(--sp-12); }
      .nf-links { grid-template-columns: minmax(0, 1fr); }
    }
  `],
  template: `
    <div class="nf">
      <p class="nf-code" aria-hidden="true">404</p>
      <h1 class="nf-title">Page not found</h1>
      <p class="nf-desc">
        Nothing on this site answers to that address. It may have been renamed,
        moved into the documentation, or never existed at all.
      </p>

      <p class="nf-request">
        <span class="nf-method">GET</span>
        <span class="nf-path">{{ path() }}</span>
        <span class="badge badge--red badge--mono">404</span>
      </p>

      <div class="nf-actions">
        <ui-button routerLink="/docs" iconRight="arrow-right">Browse the docs</ui-button>
        <ui-button variant="secondary" routerLink="/">Go home</ui-button>
      </div>

      <p class="nf-label">Popular destinations</p>
      <div class="nf-links">
        @for (d of destinations; track d.link) {
          <a class="card card--interactive nf-card" [routerLink]="d.link">
            <ui-icon [name]="d.icon" [size]="22" />
            <span class="nf-card-title">{{ d.label }}</span>
            <span class="nf-card-desc">{{ d.desc }}</span>
          </a>
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
    { label: 'Documentation', link: '/docs', icon: 'book-open', desc: 'Install, configure and run every module.' },
    { label: 'Payment Simulators', link: '/simulator', icon: 'plugs-connected', desc: 'Host, HSM, POS, ATM and switch endpoints.' },
    { label: 'EMV & Card Tools', link: '/tools/emv-tools', icon: 'cards', desc: 'Cryptograms, SDA/DDA, ATR, tags and CVV.' },
    { label: 'Blog', link: '/blogs', icon: 'article', desc: 'Guides on testing, cryptography and EMV.' },
  ];

  constructor() {
    afterNextRender(() => {
      const loc = this.doc.defaultView?.location;
      if (loc) this.path.set(loc.pathname + loc.search);
    });
  }
}
