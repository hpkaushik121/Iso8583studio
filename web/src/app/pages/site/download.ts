import { ChangeDetectionStrategy, Component } from '@angular/core';
import { SitePage } from './site-page';
import { UiSection } from '../../ui/section';
import { EXTERNAL } from '../../core/site-nav';

/** The versioned release the installer links pin to. Bump with each release. */
const RELEASE = '1.0.0';
const ASSET_BASE = `${EXTERNAL.repo}/releases/download/${RELEASE}`;

/**
 * An on-site download page with direct installer links.
 *
 * Every other download CTA on the site leaves for the GitHub releases page,
 * which ends the session unmeasured. Landing paid traffic here instead keeps
 * the click on a page we control, and the direct .dmg/.exe links fire the
 * real `app_download` event (the DOWNLOAD_FILE branch in AnalyticsService)
 * with a file_extension — the one download signal that is not a proxy.
 */
@Component({
  selector: 'page-download',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiSection],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-download' },
  template: `<main class="doc-body">
        <div class="breadcrumb">
            <a href="/">Home</a><span class="breadcrumb-sep">/</span>
            <span>Download</span>
        </div>

        <h1 class="page-title">Download ISO8583Studio</h1>
        <p class="page-description">Free and open source under Apache 2.0. Nine simulators and 64 tools
           for ISO 8583, EMV, HSM and key operations — pick your platform and start testing.</p>

        <ui-section anchor="installers" heading="Installers">
            <div class="table-wrapper"><table>
                <thead><tr><th>Platform</th><th>Download</th><th>Notes</th></tr></thead>
                <tbody>
                    <tr>
                        <td><strong>macOS</strong> (10.14+)</td>
                        <td><a class="btn btn--primary" href="${ASSET_BASE}/ISO8583Studio-${RELEASE}.dmg">⬇ Download .dmg</a></td>
                        <td>If Gatekeeper blocks the first launch, allow it under
                            <em>System Settings → Privacy &amp; Security</em>.</td>
                    </tr>
                    <tr>
                        <td><strong>Windows</strong> (10+)</td>
                        <td><a class="btn btn--primary" href="${ASSET_BASE}/ISO8583Studio-${RELEASE}.exe">⬇ Download .exe</a></td>
                        <td>Approve the Windows Firewall prompt so simulators can open server ports.</td>
                    </tr>
                    <tr>
                        <td><strong>Linux</strong></td>
                        <td><a href="${EXTERNAL.repo}/releases">All releases →</a></td>
                        <td>Run the cross-platform JAR on JDK 11+, or build from source —
                            see the <a href="/docs/installation">installation guide</a>.</td>
                    </tr>
                </tbody>
            </table></div>
        </ui-section>

        <ui-section anchor="whats-inside" heading="What's inside">
            <ul>
                <li><strong>Simulators</strong> — Host, HSM (payShield-compatible), POS, APDU and more:
                    server, client and proxy modes over TCP/IP, RS232 and REST.</li>
                <li><strong>64 tools</strong> — EMV tag &amp; cryptogram tools, PIN block and PVV calculators,
                    DUKPT and TR-31 key tools, MAC generation, converters and validators.</li>
                <li><strong>Local and offline</strong> — your keys and test data never leave your machine.</li>
            </ul>
            <p>New here? Start with the <a href="/docs/installation">installation guide</a> or the
               <a href="/docs">documentation hub</a>.</p>
        </ui-section>
    </main>`,
})
export class DownloadPage {}
