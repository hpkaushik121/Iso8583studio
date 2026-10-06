import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SitePage } from './site-page';
import { UiIcon } from '../../ui';
import { LONGFORM, LfBadge, LfTocEntry } from '../shared/longform';

@Component({
  selector: 'page-docs-contributing',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [...LONGFORM, RouterLink, UiIcon],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-contributing' },
  template: `
    <lf-header [crumbs]="crumbs" [badge]="badge" title="How to Contribute" [meta]="meta"
               lede="ISO8583Studio is AGPL v3-licensed and built in the open. Bug fixes, new tools, simulator work and docs are all welcome." />

    <lf-body [toc]="toc">
      <section lfSection id="prereqs" n="01" heading="Prerequisites (Kotlin Multiplatform)">
        <div class="lf-table-wrap"><table class="lf-table">
          <thead><tr><th>Tool</th><th>Version</th><th>Purpose</th></tr></thead>
          <tbody>
            <tr><td><strong>JDK</strong></td><td>11+ (17 LTS recommended)</td><td>Compiles Kotlin and runs Compose Desktop.</td></tr>
            <tr><td><strong>Git</strong></td><td>any recent</td><td>Clone, branch, PR.</td></tr>
            <tr><td><strong>IntelliJ IDEA</strong></td><td>Community is fine</td><td>Recommended IDE — opens the Gradle project directly with Kotlin/Compose support.</td></tr>
            <tr><td><strong>Gradle</strong></td><td>bundled wrapper</td><td>No separate install — use <code>./gradlew</code>.</td></tr>
          </tbody>
        </table></div>
      </section>

      <section lfSection id="setup" n="02" heading="Development setup">
        <lf-code label="clone, build, run" [code]="setup" />
        <h3 class="lf-h3">Project layout</h3>
        <ul class="lf-ul lf-ul--def">
          <li><strong>composeApp/</strong> — The Compose Desktop application — UI screens, simulator services, HSM/payShield engine (<code>src/desktopMain/kotlin</code>).</li>
          <li><strong>iso-core-lib/</strong> — Core ISO 8583 message library.</li>
          <li><strong>cryptocalc/</strong> — Cryptography calculators module.</li>
          <li><strong>docs/</strong> — This website.</li>
        </ul>
      </section>

      <section lfSection id="style" n="03" heading="Code style">
        <p class="lf-p">Follow standard Kotlin conventions:</p>
        <lf-code label="naming" [code]="naming" />
      </section>

      <section lfSection id="pr" n="04" heading="Submitting changes">
        <ol class="lf-steps">
          <li><strong>Fork</strong> the repository on GitHub.</li>
          <li><strong>Branch</strong> <code class="lf-step-code">git checkout -b feature/new-feature</code></li>
          <li><strong>Build &amp; test</strong> — make your change and run the full build. <code class="lf-step-code">./gradlew build</code></li>
          <li><strong>Commit</strong> with a descriptive message. <code class="lf-step-code">git commit -m "Add REST API support"</code></li>
          <li><strong>Push &amp; open a Pull Request</strong> — then open the PR against <code>main</code>. <code class="lf-step-code">git push origin feature/new-feature</code></li>
        </ol>
      </section>

      <section lfSection id="issues" n="05" heading="Reporting issues">
        <p class="lf-p">Open a <a href="https://github.com/hpkaushik121/Iso8583studio/issues">GitHub issue</a> and include:</p>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Environment</strong> — OS, Java version, app version.</li>
          <li><strong>Steps to reproduce</strong> — exact clicks/config that trigger it.</li>
          <li><strong>Expected vs actual behaviour</strong> — plus logs, configuration files or screenshots.</li>
        </ul>
      </section>

      <section lfSection id="contact" n="06" heading="Where to start" [hold]="false">
        <p class="lf-p ds-hold" [style.--d]="260">Not sure where to start? Questions and ideas are welcome in <a href="https://github.com/hpkaushik121/Iso8583studio/discussions">Discussions</a>, or reach us via the <a href="/contact">contact page</a>.</p>
        <div class="lf-cards">
          <lf-link-card class="ds-item" [style.--d]="320" icon="git-pull-request" eyebrow="Good first change" title="Open a pull request"
                        body="Fork, branch, build, and open the PR against main. Small fixes land fastest."
                        cta="Open the repository" href="https://github.com/hpkaushik121/Iso8583studio" />
          <lf-link-card class="ds-item" [style.--d]="460" icon="chats-circle" eyebrow="Questions &amp; ideas" title="Discussions"
                        body="Ask before you build if a change is large, or float an idea for a new tool."
                        cta="Start a discussion" href="https://github.com/hpkaushik121/Iso8583studio/discussions" />
          <lf-link-card class="ds-item" [style.--d]="600" icon="bug" eyebrow="Something broken" title="Issues"
                        body="Report a bug with environment, steps and logs, or request a feature."
                        cta="Open an issue" href="https://github.com/hpkaushik121/Iso8583studio/issues" />
        </div>
        <div class="lf-foot ds-hold" [style.--d]="380">
          <span class="lf-foot-line">AGPL v3 · JDK 11+ · Gradle wrapper included</span>
          <a class="lf-foot-link" routerLink="/contact">Contact us <ui-icon name="arrow-right" [size]="13" /></a>
        </div>
      </section>
    </lf-body>
  `,
})
export class DocsContributingPage {
  protected readonly crumbs = [
    { label: 'Home', link: '/' }, { label: 'Documentation', link: '/docs' }, { label: 'How to Contribute' },
  ];
  protected readonly badge: LfBadge = { tone: 'teal', label: 'Open source · AGPL v3' };
  protected readonly meta = [
    ['Language', 'Kotlin Multiplatform'], ['Build', './gradlew'], ['JDK', '11+ (17 LTS)'],
  ] as const;
  protected readonly toc: LfTocEntry[] = [
    { id: 'prereqs', n: '01', label: 'Prerequisites' },
    { id: 'setup', n: '02', label: 'Development setup' },
    { id: 'style', n: '03', label: 'Code style' },
    { id: 'pr', n: '04', label: 'Submitting changes' },
    { id: 'issues', n: '05', label: 'Reporting issues' },
    { id: 'contact', n: '06', label: 'Where to start' },
  ];

  protected readonly setup = [
    'git clone https://github.com/hpkaushik121/Iso8583studio.git',
    'cd Iso8583studio',
    './gradlew build     # compile everything + run tests',
    './gradlew run       # launch the desktop app',
    './gradlew test      # tests only',
  ].join('\n');

  protected readonly naming = [
    'class GatewayConfiguration        // classes: PascalCase',
    'fun processTransaction()          // functions: camelCase',
    'const val DEFAULT_TIMEOUT = 30    // constants: UPPER_SNAKE_CASE',
    'val connectionManager = ...       // variables: camelCase',
  ].join('\n');
}
