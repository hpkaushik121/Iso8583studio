import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SitePage } from './site-page';
import { EXTERNAL } from '../../core/site-nav';
import { Crumb, UiIcon } from '../../ui';
import {
  GuideCta, GuideFeature, GuideFeatures, GuideHero, GuideLayout, GuideRailItem, GuideRich, GuideSection,
} from '../shared/guide';

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
 *
 * The two installer anchors must keep their .dmg / .exe hrefs and the words
 * "Download .dmg" / "Download .exe" as their text: analytics reports both.
 * Built on the guide shell; the platform cards are in styles/bundles/docs.css.
 */
@Component({
  selector: 'page-download',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, UiIcon, GuideHero, GuideLayout, GuideSection, GuideFeatures, GuideRich, GuideCta],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-download' },
  template: `
    <div class="dx-page">
      <app-guide-hero heading="Download ISO8583Studio" [crumbs]="crumbs" meta="macOS · Windows · Linux from source" [dots]="true"
                      lede="Free and open source under the AGPL v3. Nine simulators (five shipping, four in development) and 64 tools for ISO 8583, EMV, HSM and key operations — pick your platform and start testing.">
        <a class="btn btn--secondary" [routerLink]="[]" fragment="installers">Pick your platform<ui-icon name="arrow-down" [size]="16" /></a>
      </app-guide-hero>

      <app-guide-layout [rail]="rail" badge="Installers">
        <app-guide-section anchor="installers" heading="Installers" [threshold]="0.1">
          <div class="dx-platforms">
            <div class="dx-platform-cell ds-item" [style.--d]="300">
              <div class="card dx-platform">
                <span class="dx-platform-ico"><ui-icon name="apple-logo" [size]="20" /></span>
                <div class="dx-platform-name"><strong>macOS</strong> <span class="dx-platform-req">(10.14+)</span></div>
                <p class="dx-platform-note">If Gatekeeper blocks the first launch, allow it under
                  <em>System Settings → Privacy &amp; Security</em>.</p>
                <div class="dx-platform-get">
                  <a class="btn btn--primary" href="${ASSET_BASE}/ISO8583Studio-${RELEASE}.dmg"><ui-icon name="download-simple" [size]="16" />Download .dmg</a>
                </div>
              </div>
            </div>
            <div class="dx-platform-cell ds-item" [style.--d]="400">
              <div class="card dx-platform">
                <span class="dx-platform-ico"><ui-icon name="windows-logo" [size]="20" /></span>
                <div class="dx-platform-name"><strong>Windows</strong> <span class="dx-platform-req">(10+)</span></div>
                <p class="dx-platform-note">Approve the Windows Firewall prompt so simulators can open server ports.</p>
                <div class="dx-platform-get">
                  <a class="btn btn--primary" href="${ASSET_BASE}/ISO8583Studio-${RELEASE}.exe"><ui-icon name="download-simple" [size]="16" />Download .exe</a>
                </div>
              </div>
            </div>
            <div class="dx-platform-cell ds-item" [style.--d]="500">
              <div class="card dx-platform">
                <span class="dx-platform-ico"><ui-icon name="linux-logo" [size]="20" /></span>
                <div class="dx-platform-name"><strong>Linux</strong></div>
                <p class="dx-platform-note">No prebuilt Linux package yet — build a .deb from source with one Gradle task;
                  see the <a class="dx-link" routerLink="/docs/installation">installation guide</a>.</p>
                <div class="dx-platform-get">
                  <a class="btn btn--secondary" href="${EXTERNAL.repo}/releases">All releases<ui-icon name="arrow-right" [size]="16" /></a>
                </div>
              </div>
            </div>
          </div>
        </app-guide-section>

        <app-guide-section anchor="verify" heading="Verify your download" [threshold]="0.15">
          <p class="gd-p">SHA-256 checksums of the v{{ release }} installers. Compare with <code>shasum -a 256 &lt;file&gt;</code> on macOS or
            <code>certutil -hashfile &lt;file&gt; SHA256</code> on Windows.</p>
          <div class="lf-table-wrap"><table class="lf-table dx-sums">
            <thead><tr><th>File</th><th>SHA-256</th></tr></thead>
            <tbody>
              @for (sum of sums; track sum.file) {
                <tr><td><code>{{ sum.file }}</code></td><td><code class="dx-sum">{{ sum.sha256 }}</code></td></tr>
              }
            </tbody>
          </table></div>
          <p class="gd-p">The installers are not yet code-signed, so Windows SmartScreen shows "Windows protected your PC" (choose
            <em>More info → Run anyway</em>) and macOS Gatekeeper asks you to allow the app once. Signing is on the roadmap; until then the
            checksums above are how you verify what you downloaded.</p>
        </app-guide-section>

        <app-guide-section anchor="whats-inside" heading="What's inside" [threshold]="0.15">
          <app-guide-features [items]="inside" />
          <p class="gd-p" [gdRich]="next"></p>
        </app-guide-section>

        <app-guide-cta heading="Try it on your own transactions" [pro]="false"
                       text="Free and open source. Download the studio and run it on your desk in minutes." />
      </app-guide-layout>
    </div>
  `,
})
export class DownloadPage {
  protected readonly crumbs: Crumb[] = [{ label: 'Home', link: '/' }, { label: 'Download' }];

  protected readonly rail: GuideRailItem[] = [
    { id: 'installers', label: 'Installers', icon: 'desktop' },
    { id: 'verify', label: 'Verify', icon: 'shield-check' },
    { id: 'whats-inside', label: 'What\'s inside', icon: 'package' },
    { id: 'download', label: 'Try it', icon: 'download-simple' },
  ];

  protected readonly release = RELEASE;
  /** sha256sum of the GitHub release assets, recomputed for every release. */
  protected readonly sums = [
    { file: `ISO8583Studio-${RELEASE}.dmg`, sha256: '6b1135882cabdd24ca9911d87dde6e4f48b84790b72bc4a0982b684262f3d817' },
    { file: `ISO8583Studio-${RELEASE}.exe`, sha256: '1b0d9fb525c86d316326c274661f8baf06b87b4ac46a5c7f9b12465c545787a1' },
  ];

  protected readonly inside: GuideFeature[] = [
    { icon: 'plugs-connected', name: 'Simulators', desc: 'Host, HSM (payShield-compatible), POS, APDU and more: server, client and proxy modes over TCP/IP, RS232 and REST.' },
    { icon: 'wrench', name: '64 tools', desc: 'EMV tag & cryptogram tools, PIN block and PVV calculators, DUKPT and TR-31 key tools, MAC generation, converters and validators.' },
    { icon: 'shield-check', name: 'Local and offline', desc: 'Your keys and test data never leave your machine.' },
  ];

  protected readonly next = 'New here? Start with the [installation guide](/docs/installation) or the [documentation hub](/docs).';
}
