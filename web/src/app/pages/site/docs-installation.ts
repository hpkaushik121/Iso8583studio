import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { SitePage } from './site-page';

@Component({
  selector: 'page-docs-installation',
  changeDetection: ChangeDetectionStrategy.OnPush,
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-installation' },
  // <image-slot> is a styling-only element the design system owns; see
  // _components.css. Drop this schema once it becomes part of ui-figure.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<main class="doc-body">
        <div class="breadcrumb">
            <a href="/">Home</a><span class="breadcrumb-sep">/</span>
            <a href="/docs">Documentation</a><span class="breadcrumb-sep">/</span>
            <span>Installation</span>
        </div>

        <h1 class="page-title">Installation</h1>
        <p class="page-description">ISO8583Studio is a Kotlin Multiplatform / Compose Desktop app shipped as a single JAR — the same file runs on Windows, macOS and Linux on top of a Java runtime.</p>

        <section class="doc-section" id="prerequisites">
            <h2>Prerequisites</h2>
            <div class="table-wrapper"><table>
                <thead><tr><th>Requirement</th><th>Minimum</th><th>Notes</th></tr></thead>
                <tbody>
                    <tr><td><strong>Java Runtime</strong></td><td>JDK / JRE 11+</td><td>Compose Desktop runs on the JVM. Temurin, OpenJDK or Oracle all work; 17 LTS recommended.</td></tr>
                    <tr><td><strong>Operating System</strong></td><td>Windows 10+, macOS 10.14+, Ubuntu 18.04+</td><td>Any modern 64-bit desktop OS.</td></tr>
                    <tr><td><strong>Memory</strong></td><td>512 MB</td><td>2 GB recommended for load testing.</td></tr>
                    <tr><td><strong>Disk</strong></td><td>100 MB</td><td>Plus space for logs and configurations.</td></tr>
                </tbody>
            </table></div>
            <p>Check your Java version first:</p>
            <pre><code>java -version
# openjdk version "17.0.x" — anything 11+ is fine</code></pre>
        </section>

        <section class="doc-section" id="windows">
            <h2>Windows</h2>
            <ol class="steps">
                <li><strong>Install Java 11+</strong> — download <a href="https://adoptium.net/">Eclipse Temurin</a> (or run <code>winget install EclipseAdoptium.Temurin.17.JRE</code>).</li>
                <li><strong>Download the JAR</strong> — grab <code>ISO8583Studio.jar</code> from the <a href="https://github.com/hpkaushik121/Iso8583studio/releases/latest">latest release</a>.</li>
                <li><strong>Run it</strong> — double-click the JAR, or from PowerShell: <code>java -jar ISO8583Studio.jar</code>.</li>
                <li><strong>Allow networking</strong> — approve the Windows Firewall prompt so simulators can open server ports.</li>
            </ol>
        </section>

        <section class="doc-section" id="macos">
            <h2>macOS</h2>
            <ol class="steps">
                <li><strong>Install Java 11+</strong> — <code>brew install --cask temurin</code> (or download from Adoptium).</li>
                <li><strong>Download the JAR</strong> — from the <a href="https://github.com/hpkaushik121/Iso8583studio/releases/latest">latest release</a>.</li>
                <li><strong>Run it</strong> — <code>java -jar ISO8583Studio.jar</code> from Terminal.</li>
                <li><strong>Gatekeeper</strong> — if macOS blocks the first launch, allow it under <em>System Settings → Privacy &amp; Security</em>.</li>
            </ol>
        </section>

        <section class="doc-section" id="linux">
            <h2>Linux</h2>
            <ol class="steps">
                <li><strong>Install Java 11+</strong> — Debian/Ubuntu: <code>sudo apt install openjdk-17-jre</code> · Fedora: <code>sudo dnf install java-17-openjdk</code>.</li>
                <li><strong>Download the JAR</strong> — <code>wget https://github.com/hpkaushik121/Iso8583studio/releases/latest/download/ISO8583Studio.jar</code></li>
                <li><strong>Run it</strong> — <code>java -jar ISO8583Studio.jar</code>.</li>
            </ol>
        </section>

        <section class="doc-section" id="source">
            <h2>Build from Source (Kotlin Multiplatform)</h2>
            <p>The project builds with the Gradle wrapper — no IDE required. You need <strong>JDK 11+</strong> and Git.</p>
            <pre><code>git clone https://github.com/hpkaushik121/Iso8583studio.git
cd Iso8583studio
./gradlew build     # compile + tests
./gradlew run       # launch the desktop app</code></pre>
            <h3>Building the JAR</h3>
            <p>To produce a runnable <code>ISO8583Studio.jar</code> from the cloned source, use the Compose Desktop packaging tasks:</p>
            <pre><code># fat/uber JAR with all dependencies (recommended)
./gradlew :composeApp:packageUberJarForCurrentOS
# output: composeApp/build/compose/jars/ISO8583Studio-&lt;os&gt;-&lt;arch&gt;-1.0.14.jar

# run it
java -jar composeApp/build/compose/jars/ISO8583Studio-*.jar</code></pre>
            <p>Or build a native installer for your OS instead of a JAR:</p>
            <pre><code>./gradlew :composeApp:packageDistributionForCurrentOS
# output: composeApp/build/compose/binaries/main/
#   Windows → .msi · macOS → .dmg · Linux → .deb</code></pre>
            <div class="info-card tip"><div class="info-card-title">Tip</div><p>On Windows use <code>gradlew.bat</code> instead of <code>./gradlew</code>. For development, IntelliJ IDEA opens the project directly — see <a href="/docs/contributing">How to Contribute</a>.</p></div>
        </section>

        <section class="doc-section" id="troubleshooting">
            <h2>Troubleshooting</h2>
            <div class="table-wrapper"><table>
                <thead><tr><th>Symptom</th><th>Fix</th></tr></thead>
                <tbody>
                    <tr><td><code>UnsupportedClassVersionError</code></td><td>Your Java is older than 11 — install a newer JDK/JRE and re-check <code>java -version</code>.</td></tr>
                    <tr><td>Nothing happens on double-click</td><td>JARs aren't associated with Java — run <code>java -jar ISO8583Studio.jar</code> from a terminal.</td></tr>
                    <tr><td>"Connection refused" in simulators</td><td>Port in use or blocked by firewall — change the port in Transmission Settings.</td></tr>
                </tbody>
            </table></div>
        </section>

</main>

<aside class="pro-nudge"><span class="pn-tag">✦ Pro</span><p>Rather not install anything for CI? Pro raises the CPS ceiling, unlocks the full algorithm set and deep simulator tweaks, plus hosted endpoints and priority support.</p><a href="/pro">Register for Pro →</a></aside>`,
})
export class DocsInstallationPage {}
