import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { SitePage } from './site-page';

@Component({
  selector: 'page-docs-versions',
  changeDetection: ChangeDetectionStrategy.OnPush,
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-versions' },
  // <image-slot> is a styling-only element the design system owns; see
  // _components.css. Drop this schema once it becomes part of ui-figure.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<main class="doc-body">
        <div class="breadcrumb">
            <a href="/">Home</a><span class="breadcrumb-sep">/</span>
            <a href="/docs">Documentation</a><span class="breadcrumb-sep">/</span>
            <span>Versions</span>
        </div>

        <div class="page-title-row"><h1 class="page-title">Versions</h1><span class="badge badge-teal">Current: v1.0.14</span></div>
        <p class="page-description">One release channel, one artifact: the cross-platform <code>ISO8583Studio.jar</code> published on GitHub Releases.</p>

        <section class="doc-section" id="current">
            <h2>Current Release — v1.0.14</h2>
            <div class="spec-list">
                <div class="spec-row"><div class="k">Version</div><div class="v"><code>v1.0.14</code> · latest stable</div></div>
                <div class="spec-row"><div class="k">Contents</div><div class="v">64 tools and 9 simulators — Host, HSM (payShield 10K), HSM Command Console, POS, APDU, and the in-development Switch, Issuer, ATM and ECR.</div></div>
                <div class="spec-row"><div class="k">Platforms</div><div class="v">Windows 10+ · macOS 10.14+ · Linux (Ubuntu 18.04+) — single JAR, JDK 11+.</div></div>
                <div class="spec-row"><div class="k">License</div><div class="v">Apache — free and open source, no license fees.</div></div>
            </div>
            <p><a class="btn btn-blue" href="https://github.com/hpkaushik121/Iso8583studio/releases/latest">Download v1.0.14</a></p>
            <pre><code>java -jar ISO8583Studio.jar</code></pre>
        </section>

        <section class="doc-section" id="history">
            <h2>Release History</h2>
            <p>Every released version, from the version bumps in the project's Git history.</p>
            <div class="spec-list">
                <div class="spec-row"><div class="k"><code>v1.0.14</code></div><div class="v"><strong>Jun 12, 2026</strong> — HSM simulator fixes: payShield <code>M4</code> and <code>GW</code> host-command handling.</div></div>
                <div class="spec-row"><div class="k"><code>v1.0.12</code></div><div class="v"><strong>May 2, 2026</strong> — Maintenance release.</div></div>
                <div class="spec-row"><div class="k"><code>v1.0.10</code></div><div class="v"><strong>Apr 29, 2026</strong> — Maintenance release.</div></div>
                <div class="spec-row"><div class="k"><code>v1.0.9</code></div><div class="v"><strong>Apr 17, 2026</strong> — Maintenance release.</div></div>
                <div class="spec-row"><div class="k"><code>v1.0.7</code></div><div class="v"><strong>Apr 16, 2026</strong> — First release with the HSM Command Console.</div></div>
                <div class="spec-row"><div class="k"><code>v1.0.5</code></div><div class="v"><strong>Mar 30, 2026</strong> — Maintenance release.</div></div>
                <div class="spec-row"><div class="k"><code>v1.0.4</code></div><div class="v"><strong>Mar 30, 2026</strong> — Maintenance release.</div></div>
                <div class="spec-row"><div class="k"><code>v1.0.3</code></div><div class="v"><strong>Mar 29, 2026</strong> — Maintenance release.</div></div>
                <div class="spec-row"><div class="k"><code>v1.0.2</code></div><div class="v"><strong>Mar 26, 2026</strong> — Maintenance release.</div></div>
                <div class="spec-row"><div class="k"><code>v1.0.1</code></div><div class="v"><strong>Mar 20, 2026</strong> — First version bump after the initial release.</div></div>
                <div class="spec-row"><div class="k"><code>v1.0.0</code></div><div class="v"><strong>2025</strong> — Initial release: Host and HSM simulators, cipher tools, REST gateway mode, YAML config import/export (built May–Jun 2025).</div></div>
            </div>
            <p class="text-muted">Version numbers not listed (1.0.6, 1.0.8, 1.0.11, 1.0.13) were internal and never published.</p>
        </section>

        <section class="doc-section" id="channel">
            <h2>Release Channel</h2>
            <ul>
                <li><strong>Stable releases</strong> are tagged on <a href="https://github.com/hpkaushik121/Iso8583studio/releases">GitHub Releases</a> with full changelogs.</li>
                <li><strong>What's next</strong> — the ATM, ECR, Switch and Issuer simulators are in active development; follow the <a href="https://github.com/users/hpkaushik121/projects/1">roadmap</a>.</li>
                <li><strong>Upgrading</strong> — replace the JAR; saved simulator configurations are kept alongside it and carry over.</li>
            </ul>
            <div class="info-card note"><div class="info-card-title">Note</div><p>New here? Start with the <a href="/docs/installation">installation guide</a>.</p></div>
        </section>

</main>

<aside class="pro-nudge"><span class="pn-tag">✦ Pro</span><p>Need the same builds pinned for a team, with hosted endpoints? Pro raises the CPS ceiling, unlocks the full algorithm set and deep simulator tweaks, plus hosted endpoints and priority support.</p><a href="/pro">Register for Pro →</a></aside>`,
})
export class DocsVersionsPage {}
