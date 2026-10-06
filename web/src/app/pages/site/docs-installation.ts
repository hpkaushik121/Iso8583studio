import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SitePage } from './site-page';
import { UiIcon } from '../../ui';
import { LONGFORM, LfBadge, LfHighlight, LfTocEntry } from '../shared/longform';

@Component({
  selector: 'page-docs-installation',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [...LONGFORM, RouterLink, UiIcon],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-installation' },
  template: `
    <lf-header [crumbs]="crumbs" [badge]="badge" title="Installation" [meta]="meta" [highlights]="highlights"
               lede="ISO8583Studio is a Kotlin Multiplatform / Compose Desktop app shipped as a single JAR — the same file runs on Windows, macOS and Linux on top of a Java runtime." />

    <lf-body [toc]="toc">
      <section lfSection id="prerequisites" n="01" heading="Prerequisites">
        <div class="lf-table-wrap"><table class="lf-table">
          <thead><tr><th>Requirement</th><th>Minimum</th><th>Notes</th></tr></thead>
          <tbody>
            <tr><td><strong>Java Runtime</strong></td><td>JDK / JRE 11+</td><td>Compose Desktop runs on the JVM. Temurin, OpenJDK or Oracle all work; 17 LTS recommended.</td></tr>
            <tr><td><strong>Operating System</strong></td><td>Windows 10+, macOS 10.14+, Ubuntu 18.04+</td><td>Any modern 64-bit desktop OS.</td></tr>
            <tr><td><strong>Memory</strong></td><td>512 MB</td><td>2 GB recommended for load testing.</td></tr>
            <tr><td><strong>Disk</strong></td><td>100 MB</td><td>Plus space for logs and configurations.</td></tr>
          </tbody>
        </table></div>
        <p class="lf-p">Check your Java version first:</p>
        <lf-code label="check the runtime" [code]="javaVersion" />
      </section>

      <section lfSection id="windows" n="02" heading="Windows">
        <ol class="lf-steps">
          <li><strong>Install Java 11+</strong> — download <a href="https://adoptium.net/">Eclipse Temurin</a> (or run <code>winget install EclipseAdoptium.Temurin.17.JRE</code>).</li>
          <li><strong>Download the JAR</strong> — grab <code>ISO8583Studio.jar</code> from the <a href="https://github.com/hpkaushik121/Iso8583studio/releases/latest">latest release</a>.</li>
          <li><strong>Run it</strong> — double-click the JAR, or from PowerShell: <code>java -jar ISO8583Studio.jar</code>.</li>
          <li><strong>Allow networking</strong> — approve the Windows Firewall prompt so simulators can open server ports.</li>
        </ol>
      </section>

      <section lfSection id="macos" n="03" heading="macOS">
        <ol class="lf-steps">
          <li><strong>Install Java 11+</strong> — <code>brew install --cask temurin</code> (or download from Adoptium).</li>
          <li><strong>Download the JAR</strong> — from the <a href="https://github.com/hpkaushik121/Iso8583studio/releases/latest">latest release</a>.</li>
          <li><strong>Run it</strong> — <code>java -jar ISO8583Studio.jar</code> from Terminal.</li>
          <li><strong>Gatekeeper</strong> — if macOS blocks the first launch, allow it under <em>System Settings → Privacy &amp; Security</em>.</li>
        </ol>
      </section>

      <section lfSection id="linux" n="04" heading="Linux">
        <ol class="lf-steps">
          <li><strong>Install Java 11+</strong> — Debian/Ubuntu: <code>sudo apt install openjdk-17-jre</code> · Fedora: <code>sudo dnf install java-17-openjdk</code>.</li>
          <li><strong>Download the JAR</strong> — <code class="lf-step-code">wget https://github.com/hpkaushik121/Iso8583studio/releases/latest/download/ISO8583Studio.jar</code></li>
          <li><strong>Run it</strong> — <code>java -jar ISO8583Studio.jar</code>.</li>
        </ol>
      </section>

      <section lfSection id="source" n="05" heading="Build from Source (Kotlin Multiplatform)">
        <p class="lf-p">The project builds with the Gradle wrapper — no IDE required. You need <strong>JDK 11+</strong> and Git.</p>
        <lf-code label="clone, build, run" [code]="cloneAndRun" />
        <h3 class="lf-h3">Building the JAR</h3>
        <p class="lf-p">To produce a runnable <code>ISO8583Studio.jar</code> from the cloned source, use the Compose Desktop packaging tasks:</p>
        <lf-code label="package the JAR" [code]="buildJar" />
        <p class="lf-p">Or build a native installer for your OS instead of a JAR:</p>
        <lf-code label="package a native installer" [code]="buildInstaller" />
        <lf-note tone="teal" icon="lightbulb" title="Tip">On Windows use <code>gradlew.bat</code> instead of <code>./gradlew</code>. For development, IntelliJ IDEA opens the project directly — see <a href="/docs/contributing">How to Contribute</a>.</lf-note>
      </section>

      <section lfSection id="customise" n="06" heading="Change the package">
        <p class="lf-p">Packaging is declared in <code>composeApp/build.gradle.kts</code>, in the <code>nativeDistributions</code> block of <code>compose.desktop.application</code>.</p>
        <ul class="lf-ul lf-ul--def">
          <li><strong>packageName and packageVersion:</strong> installer name and version; macOS reuses the version for <code>dmgPackageVersion</code>.</li>
          <li><strong>targetFormats(…):</strong> currently Dmg, Msi, Deb and Exe. Add or drop formats here.</li>
          <li><strong>Icons:</strong> <code>composeApp/resources/windows/app.ico</code>, <code>resources/mac/app.icns</code> and <code>resources/linux/app.png</code>.</li>
          <li><strong>macOS signing:</strong> <code>sign</code> is set to false. Set it to true and supply a Developer ID identity to ship a signed build.</li>
        </ul>
      </section>

      <section lfSection id="troubleshooting" n="07" heading="Troubleshooting">
        <div class="lf-table-wrap"><table class="lf-table">
          <thead><tr><th>Symptom</th><th>Fix</th></tr></thead>
          <tbody>
            <tr><td><code>UnsupportedClassVersionError</code></td><td>Your Java is older than 11 — install a newer JDK/JRE and re-check <code>java -version</code>.</td></tr>
            <tr><td>Nothing happens on double-click</td><td>JARs aren't associated with Java — run <code>java -jar ISO8583Studio.jar</code> from a terminal.</td></tr>
            <tr><td>"Connection refused" in simulators</td><td>Port in use or blocked by firewall — change the port in Transmission Settings.</td></tr>
          </tbody>
        </table></div>
      </section>

      <section lfSection id="contact" n="08" heading="Next steps" [hold]="false">
        <p class="lf-p ds-hold" [style.--d]="260">Installed and running? Pick up the release notes, the development setup, or tell us what broke.</p>
        <div class="lf-cards">
          <lf-link-card class="ds-item" [style.--d]="320" icon="download-simple" eyebrow="Latest build" title="GitHub Releases"
                        body="The cross-platform JAR for every tagged release, with a changelog per tag."
                        cta="Open releases" href="https://github.com/hpkaushik121/Iso8583studio/releases/latest" />
          <lf-link-card class="ds-item" [style.--d]="460" icon="git-pull-request" eyebrow="Development" title="Contribute"
                        body="Prerequisites, project layout, code style and the pull-request flow."
                        cta="Read the guide" href="/docs/contributing" />
          <lf-link-card class="ds-item" [style.--d]="600" icon="bug" eyebrow="Something broken" title="Issues"
                        body="Report an install or build failure with your OS, Java version and the Gradle output."
                        cta="Open an issue" href="https://github.com/hpkaushik121/Iso8583studio/issues" />
        </div>
        <div class="lf-foot ds-hold" [style.--d]="380">
          <span class="lf-foot-line">JDK 11+ · single JAR · Windows · macOS · Linux</span>
          <a class="lf-foot-link" routerLink="/docs/versions">Versions <ui-icon name="arrow-right" [size]="13" /></a>
        </div>
      </section>

      <aside class="pro-nudge lf-nudge">
        <p>Rather not install anything for CI? Pro raises the CPS ceiling, unlocks the full algorithm set and deep simulator tweaks, plus hosted endpoints and priority support.</p>
        <a routerLink="/pro">Register for Pro <ui-icon name="arrow-right" [size]="14" /></a>
      </aside>
    </lf-body>
  `,
})
export class DocsInstallationPage {
  protected readonly crumbs = [
    { label: 'Home', link: '/' }, { label: 'Documentation', link: '/docs' }, { label: 'Installation' },
  ];
  protected readonly badge: LfBadge = { tone: 'teal', label: 'v1.0.14 · AGPL v3' };
  protected readonly meta = [
    ['Latest', 'v1.0.14'], ['Runtime', 'JDK / JRE 11+'], ['Artifact', 'ISO8583Studio.jar'],
  ] as const;
  protected readonly highlights: LfHighlight[] = [
    { icon: 'windows-logo', tone: 'blue', title: 'Windows', body: 'Install Temurin, download the JAR, then double-click it or run it from PowerShell. Windows 10+.' },
    { icon: 'apple-logo', tone: 'blue', title: 'macOS', body: 'Temurin from Homebrew, then run the JAR from Terminal. Gatekeeper may ask once. macOS 10.14+.' },
    { icon: 'linux-logo', tone: 'teal', title: 'Linux', body: 'OpenJDK from apt or dnf, wget the JAR and run it. Ubuntu 18.04+.' },
  ];
  protected readonly toc: LfTocEntry[] = [
    { id: 'prerequisites', n: '01', label: 'Prerequisites' },
    { id: 'windows', n: '02', label: 'Windows' },
    { id: 'macos', n: '03', label: 'macOS' },
    { id: 'linux', n: '04', label: 'Linux' },
    { id: 'source', n: '05', label: 'Build from source' },
    { id: 'customise', n: '06', label: 'Change the package' },
    { id: 'troubleshooting', n: '07', label: 'Troubleshooting' },
    { id: 'contact', n: '08', label: 'Next steps' },
  ];

  protected readonly javaVersion = [
    'java -version',
    '# openjdk version "17.0.x" — anything 11+ is fine',
  ].join('\n');

  protected readonly cloneAndRun = [
    'git clone https://github.com/hpkaushik121/Iso8583studio.git',
    'cd Iso8583studio',
    './gradlew build     # compile + tests',
    './gradlew run       # launch the desktop app',
  ].join('\n');

  protected readonly buildJar = [
    '# fat/uber JAR with all dependencies (recommended)',
    './gradlew :composeApp:packageUberJarForCurrentOS',
    '# output: composeApp/build/compose/jars/ISO8583Studio-<os>-<arch>-1.0.14.jar',
    '',
    '# run it',
    'java -jar composeApp/build/compose/jars/ISO8583Studio-*.jar',
  ].join('\n');

  protected readonly buildInstaller = [
    './gradlew :composeApp:packageDistributionForCurrentOS',
    '# output: composeApp/build/compose/binaries/main/',
    '#   Windows → .msi · macOS → .dmg · Linux → .deb',
  ].join('\n');
}
