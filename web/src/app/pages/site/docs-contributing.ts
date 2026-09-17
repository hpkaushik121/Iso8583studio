import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { SitePage } from './site-page';
import { UiSection } from '../../ui/section';

@Component({
  selector: 'page-docs-contributing',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiSection],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-contributing' },
  // <image-slot> is a styling-only element the design system owns; see
  // _components.css. Drop this schema once it becomes part of ui-figure.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<main class="doc-body">
        <div class="breadcrumb">
            <a href="/">Home</a><span class="breadcrumb-sep">/</span>
            <a href="/docs">Documentation</a><span class="breadcrumb-sep">/</span>
            <span>How to Contribute</span>
        </div>

        <h1 class="page-title">How to Contribute</h1>
        <p class="page-description">ISO8583Studio is Apache-licensed and built in the open. Bug fixes, new tools, simulator work and docs are all welcome.</p>

        <ui-section anchor="prereqs" heading="Prerequisites (Kotlin Multiplatform)">
            <div class="table-wrapper"><table>
                <thead><tr><th>Tool</th><th>Version</th><th>Purpose</th></tr></thead>
                <tbody>
                    <tr><td><strong>JDK</strong></td><td>11+ (17 LTS recommended)</td><td>Compiles Kotlin and runs Compose Desktop.</td></tr>
                    <tr><td><strong>Git</strong></td><td>any recent</td><td>Clone, branch, PR.</td></tr>
                    <tr><td><strong>IntelliJ IDEA</strong></td><td>Community is fine</td><td>Recommended IDE — opens the Gradle project directly with Kotlin/Compose support.</td></tr>
                    <tr><td><strong>Gradle</strong></td><td>bundled wrapper</td><td>No separate install — use <code>./gradlew</code>.</td></tr>
                </tbody>
            </table></div>
        </ui-section>

        <ui-section anchor="setup" heading="Development Setup">
            <pre><code>git clone https://github.com/hpkaushik121/Iso8583studio.git
cd Iso8583studio
./gradlew build     # compile everything + run tests
./gradlew run       # launch the desktop app
./gradlew test      # tests only</code></pre>
            <h3>Project Layout</h3>
            <div class="spec-list">
                <div class="spec-row"><div class="k">composeApp/</div><div class="v">The Compose Desktop application — UI screens, simulator services, HSM/payShield engine (<code>src/desktopMain/kotlin</code>).</div></div>
                <div class="spec-row"><div class="k">iso-core-lib/</div><div class="v">Core ISO 8583 message library.</div></div>
                <div class="spec-row"><div class="k">cryptocalc/</div><div class="v">Cryptography calculators module.</div></div>
                <div class="spec-row"><div class="k">docs/</div><div class="v">This website.</div></div>
            </div>
        </ui-section>

        <ui-section anchor="style" heading="Code Style">
            <p>Follow standard Kotlin conventions:</p>
            <pre><code>class GatewayConfiguration        // classes: PascalCase
fun processTransaction()          // functions: camelCase
const val DEFAULT_TIMEOUT = 30    // constants: UPPER_SNAKE_CASE
val connectionManager = ...       // variables: camelCase</code></pre>
        </ui-section>

        <ui-section anchor="pr" heading="Submitting Changes">
            <ol class="steps">
                <li><strong>Fork</strong> the repository on GitHub.</li>
                <li><strong>Branch</strong> — <code>git checkout -b feature/new-feature</code>.</li>
                <li><strong>Build &amp; test</strong> — make your change and run <code>./gradlew build</code>.</li>
                <li><strong>Commit</strong> with a descriptive message — e.g. <code>git commit -m "Add REST API support"</code>.</li>
                <li><strong>Push &amp; open a Pull Request</strong> — <code>git push origin feature/new-feature</code>, then open the PR against <code>main</code>.</li>
            </ol>
        </ui-section>

        <ui-section anchor="issues" heading="Reporting Issues">
            <p>Open a <a href="https://github.com/hpkaushik121/Iso8583studio/issues">GitHub issue</a> and include:</p>
            <ul>
                <li><strong>Environment</strong> — OS, Java version, app version.</li>
                <li><strong>Steps to reproduce</strong> — exact clicks/config that trigger it.</li>
                <li><strong>Expected vs actual behaviour</strong> — plus logs, configuration files or screenshots.</li>
            </ul>
            <div class="info-card tip"><div class="info-card-title">Tip</div><p>Not sure where to start? Questions and ideas are welcome in <a href="https://github.com/hpkaushik121/Iso8583studio/discussions">Discussions</a>, or reach us via the <a href="/contact">contact page</a>.</p></div>
        </ui-section>

</main>`,
})
export class DocsContributingPage {}
