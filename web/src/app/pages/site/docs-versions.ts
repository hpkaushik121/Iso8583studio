import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SitePage } from './site-page';
import { EXTERNAL } from '../../core/site-nav';
import { UiBadge, UiCtaPanel, UiIcon, UiReveal, UiWords } from '../../ui';
import { LONGFORM, LfBadge } from '../shared/longform';

interface Release { v: string; date: string; note: string; latest?: boolean; }

@Component({
  selector: 'page-docs-versions',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [...LONGFORM, RouterLink, UiBadge, UiCtaPanel, UiIcon, UiReveal, UiWords],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-versions' },
  template: `
    <lf-header [crumbs]="crumbs" [badge]="badge" title="Versions"
               lede="Every version with its date and changes. The 1.0.0 installers for Windows and macOS are on GitHub Releases; 1.0.14 is the current source release and Linux builds from source." />

    <lf-body>
      <section lfSection id="current" n="01" heading="Current release, v1.0.14" [hold]="false">
        <div class="lf-ver-grid">
          <div class="lf-spec-panel ds-hold" [style.--d]="260">
            <div class="lf-spec">
              <span class="lf-spec-ico"><ui-icon name="tag" [size]="16" /></span>
              <div class="lf-spec-text"><span class="lf-caps">Version</span><span class="lf-spec-value"><code>v1.0.14</code> · current source release · installers are v1.0.0</span></div>
            </div>
            <div class="lf-spec">
              <span class="lf-spec-ico"><ui-icon name="stack" [size]="16" /></span>
              <div class="lf-spec-text"><span class="lf-caps">Contents</span><span class="lf-spec-value">64 tools and 9 simulators — Host, HSM (payShield 10K), HSM Command Console, POS, APDU, and the in-development Switch, Issuer, ATM and ECR.</span></div>
            </div>
            <div class="lf-spec">
              <span class="lf-spec-ico"><ui-icon name="desktop" [size]="16" /></span>
              <div class="lf-spec-text"><span class="lf-caps">Platforms</span><span class="lf-spec-value">Windows 10+ · macOS 10.14+ · Linux (Ubuntu 18.04+) — single JAR, installers bundle a runtime · JDK 17+ to build.</span></div>
            </div>
            <div class="lf-spec">
              <span class="lf-spec-ico"><ui-icon name="scales" [size]="16" /></span>
              <div class="lf-spec-text"><span class="lf-caps">License</span><span class="lf-spec-value">AGPL v3 — free and open source, no license fees.</span></div>
            </div>
          </div>
          <div class="lf-latest ds-hold" [style.--d]="360">
            <div class="lf-latest-head">
              <span class="lf-caps">Latest source release</span>
              <span class="lf-latest-v">v1.0.14</span>
            </div>
            <a class="btn btn--primary btn--block btn--glow" [href]="releases">Download the studio <ui-icon name="arrow-up-right" [size]="16" /></a>
            <div class="lf-latest-block">
              <span class="lf-caps">Run it</span>
              <lf-code label="from source, JDK 17+" code="./gradlew run" />
            </div>
            <div class="lf-latest-block">
              <span class="lf-caps">Runs on</span>
              <div class="lf-pills">
                <span class="lf-pill"><ui-icon name="windows-logo" [size]="14" />Windows 10+</span>
                <span class="lf-pill"><ui-icon name="apple-logo" [size]="14" />macOS 10.14+</span>
                <span class="lf-pill"><ui-icon name="linux-logo" [size]="14" />Linux</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section lfSection id="history" n="02" heading="Release history" [hold]="false">
        <p class="lf-p ds-hold" [style.--d]="240">Every released version, from the version bumps in the project's Git history.</p>
        <div class="lf-releases">
          @for (r of history; track r.v; let i = $index) {
            <div class="lf-release ds-item" [class.lf-release--latest]="r.latest" [style.--d]="280 + i * 70">
              <span class="lf-release-v">{{ r.v }}</span>
              <div class="lf-release-text">
                <span class="lf-release-date">{{ r.date }}</span>
                <span class="lf-release-note">{{ r.note }}</span>
              </div>
            </div>
          }
        </div>
        <p class="lf-fine ds-hold" [style.--d]="520">Version numbers not listed (1.0.6, 1.0.8, 1.0.11, 1.0.13) were internal and never published.</p>
      </section>

      <section lfSection id="channel" n="03" heading="Release channel">
        <ul class="lf-ul lf-ul--def">
          <li><strong>Installers</strong> for 1.0.0 are on <a href="https://github.com/hpkaushik121/Iso8583studio/releases">GitHub Releases</a>; the later versions listed here were built from source and have not yet been published there.</li>
          <li><strong>What's next</strong> — the ATM, ECR, Switch and Issuer simulators are in active development; follow the <a href="https://github.com/users/hpkaushik121/projects/1">roadmap</a>.</li>
          <li><strong>Upgrading</strong> — replace the JAR; saved simulator configurations are kept alongside it and carry over.</li>
        </ul>
        <lf-note tone="neutral" icon="info" title="Note">New here? Start with the <a href="/docs/installation">installation guide</a>.</lf-note>
      </section>

      <div class="lf-cta">
        <ui-cta-panel>
          <div class="lf-cta-in" uiReveal>
            <div class="ds-hold"><ui-badge tone="blue" icon="sparkle">Pro</ui-badge></div>
            <h2><ui-words text="Need the same builds pinned for a team, with hosted endpoints?" /></h2>
            <p class="ds-hold" [style.--d]="320">Pro raises the CPS ceiling, unlocks the full algorithm set and deep simulator tweaks, plus hosted endpoints and priority support.</p>
            <div class="pro-nudge ds-hold" [style.--d]="440">
              <a class="btn btn--primary btn--lg btn--glow" routerLink="/pro">Register for Pro <ui-icon name="arrow-up-right" [size]="16" /></a>
            </div>
          </div>
        </ui-cta-panel>
      </div>
    </lf-body>
  `,
})
export class DocsVersionsPage {
  protected readonly releases = EXTERNAL.releases;
  protected readonly crumbs = [
    { label: 'Home', link: '/' }, { label: 'Documentation', link: '/docs' }, { label: 'Versions' },
  ];
  protected readonly badge: LfBadge = { tone: 'teal', label: 'Current: v1.0.14' };

  protected readonly history: Release[] = [
    { v: 'v1.0.14', date: 'Jun 12, 2026', note: 'HSM simulator fixes: payShield M4 and GW host-command handling.', latest: true },
    { v: 'v1.0.12', date: 'May 2, 2026', note: 'Maintenance release.' },
    { v: 'v1.0.10', date: 'Apr 29, 2026', note: 'Maintenance release.' },
    { v: 'v1.0.9', date: 'Apr 17, 2026', note: 'Maintenance release.' },
    { v: 'v1.0.7', date: 'Apr 16, 2026', note: 'First release with the HSM Command Console.' },
    { v: 'v1.0.5', date: 'Mar 30, 2026', note: 'Maintenance release.' },
    { v: 'v1.0.4', date: 'Mar 30, 2026', note: 'Maintenance release.' },
    { v: 'v1.0.3', date: 'Mar 29, 2026', note: 'Maintenance release.' },
    { v: 'v1.0.2', date: 'Mar 26, 2026', note: 'Maintenance release.' },
    { v: 'v1.0.1', date: 'Mar 20, 2026', note: 'First version bump after the initial release.' },
    { v: 'v1.0.0', date: '2025', note: 'Initial release: Host and HSM simulators, cipher tools, REST gateway mode, YAML config import/export (built May–Jun 2025).' },
  ];
}
