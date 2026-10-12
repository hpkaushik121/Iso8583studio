import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SitePage } from './site-page';
import { AUTHOR, EXTERNAL } from '../../core/site-nav';
import { RELEASE } from '../../core/release';
import { BLOG_POSTS } from '../../content/blog-index';
import { UiBadge, UiCtaPanel, UiIcon, UiReveal, UiWords } from '../../ui';
import { LONGFORM, LfBadge, LfTocEntry } from '../shared/longform';

interface Milestone { when: string; what: string; }
interface Principle { icon: string; title: string; body: string; }

/** The licence text itself, as the design links it, rather than the repository's copy. */
const AGPL_URL = 'https://www.gnu.org/licenses/agpl-3.0.html#license-text';

/**
 * About (/about): what the studio is, why it exists, who builds it, how it is
 * built, the milestones so far and how to reach the people behind it.
 * Ported from the prototype's about.jsx on the legal-page shell: a
 * definition list, a person card, feature cards, a timeline and link cards,
 * each a longform block.
 *
 * Every figure on the page is read from the same place the rest of the site
 * reads it — the release facts, the author constants, the blog index — so this
 * page cannot quietly fall behind the product.
 */
@Component({
  selector: 'page-about',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [...LONGFORM, RouterLink, UiBadge, UiCtaPanel, UiIcon, UiReveal, UiWords],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-about' },
  template: `
    <lf-header [crumbs]="crumbs" [badge]="badge" title="About ISO8583Studio" [meta]="meta"
               lede="A desktop workbench for payment engineers, built by one of them. ISO8583Studio simulates the hosts, HSMs, terminals and cards a payment integration talks to, so the integration can be developed, tested and certified without a lab." />

    <lf-body [toc]="toc">
      <section lfSection id="what" n="01" heading="What it is">
        <p class="lf-p">ISO8583Studio is a free, open-source desktop application for working with ISO 8583 payment messages and the cryptography around them. It bundles nine simulators (five shipping, four in development) and 64 tools: an authorization host you can script, a Thales payShield 10K emulation with 41 documented host commands, an Android terminal emulator, an APDU card session, and calculators for PIN blocks, DUKPT, TR-31, EMV cryptograms, MACs and key check values.</p>
        <p class="lf-p">It runs on Windows and macOS as a native installer and builds from source on Linux. It is written in Kotlin Multiplatform with Compose Desktop and released under the GNU AGPL v3. Keys and test data never leave the machine it runs on.</p>
        <ul class="lf-ul lf-ul--def">
          <li><strong>Simulators:</strong> Host, HSM, HSM Command Console, POS and APDU shipping; Switch, Issuer, ATM and ECR in development. <a routerLink="/simulator">All nine</a>.</li>
          <li><strong>Tools:</strong> 64 across six disciplines: EMV and card, cryptography, key management, PIN and payment utilities, MAC, data converters. <a routerLink="/docs">Documentation</a>.</li>
          <li><strong>Release:</strong> v{{ release.version }} · {{ release.dateLabel }} · installers are v{{ release.installers }} · <a routerLink="/docs/versions">every version</a>.</li>
          <li><strong>License:</strong> GNU AGPL v3 — free to use, study, modify and redistribute; no trial, no seat limit. <a [href]="agplUrl" rel="noopener">Read the licence</a>.</li>
        </ul>
      </section>

      <section lfSection id="why" n="02" heading="Why it exists">
        <p class="lf-p">Most of a payment engineer's day is spent waiting for something that is not code: a test host that is down, a lab slot three weeks out, a scheme contact who has not replied, a physical HSM that someone else is using. The integration itself is usually a few hundred lines; proving it is the hard part.</p>
        <p class="lf-p">The studio moves that proof onto the engineer's own laptop. Stand up an authorization host, point a terminal at it, send a 0200, script the 0210, verify the cryptogram against a simulated payShield, and read every field beside the hex it came from. The lab is still needed at the end, but it is the end, not the middle.</p>
        <p class="lf-p">Everything in it is documented to the byte, because a tool that hides what it is doing is no use for debugging a transaction that one bank parses differently from another.</p>
      </section>

      <section lfSection id="who" n="03" heading="Who builds it">
        <div class="lf-person">
          <span class="lf-avatar" aria-hidden="true">{{ author.initials }}</span>
          <div class="lf-person-text">
            <span class="lf-person-name"><a [routerLink]="author.path">{{ author.name }}</a></span>
            <span class="lf-person-role">Payments engineer. Maintains the studio, writes its {{ guides }} guides and runs the certification and middleware engagements behind it.</span>
            <span class="lf-person-links">
              <span class="lf-person-links-k">Profile</span>
              <a [routerLink]="author.path"><ui-icon name="user-circle" [size]="14" />On this site</a>
              <a [href]="author.github" rel="noopener"><ui-icon name="github-logo" [size]="14" />GitHub</a>
              <a [href]="author.linkedin" rel="noopener"><ui-icon name="linkedin-logo" [size]="14" />LinkedIn</a>
            </span>
          </div>
        </div>
        <p class="lf-p">The studio is published by AiCortex Solutions Pvt. Ltd., India, which also offers <a routerLink="/emv-certification">EMV certification</a>, <a routerLink="/middleware">payment middleware</a>, <a routerLink="/kernel">kernel development</a> and <a routerLink="/cloud-simulators">hosted simulators</a> built on the same engine. AiCortex Solutions is the data controller for the website and the application; the <a routerLink="/privacy-policy">privacy policy</a> and <a routerLink="/terms-and-conditions">terms</a> name it.</p>
      </section>

      <section lfSection id="built" n="04" heading="How it is built">
        <div class="lf-features">
          @for (p of principles; track p.title; let i = $index) {
            <div class="lf-highlight ds-item" [style.--d]="300 + i * 110">
              <span class="lf-ico lf-ico--blue"><ui-icon [name]="p.icon" [size]="17" /></span>
              <span class="lf-highlight-title">{{ p.title }}</span>
              <span class="lf-highlight-body">{{ p.body }}</span>
            </div>
          }
        </div>
      </section>

      <section lfSection id="milestones" n="05" heading="Milestones">
        <ol class="lf-timeline">
          @for (m of milestones; track m.when; let last = $last) {
            <li>
              <span class="lf-timeline-date">{{ m.when }}</span>
              <span class="lf-timeline-rail" aria-hidden="true"><span class="lf-timeline-dot"></span>@if (!last) {<span class="lf-timeline-line"></span>}</span>
              <span class="lf-timeline-text">{{ m.what }}</span>
            </li>
          }
        </ol>
        <p class="lf-p">Release notes for every version are on the <a routerLink="/docs/versions">versions page</a>; the roadmap is on <a [href]="repo" rel="noopener">GitHub</a>.</p>
      </section>

      <section lfSection id="contact" n="06" heading="Get in touch" [hold]="false">
        <p class="lf-p ds-hold" [style.--d]="260">Bugs and feature requests go to GitHub, where they land on the roadmap. Engagements, support and anything private go to email.</p>
        <div class="lf-cards">
          <lf-link-card class="ds-item" [style.--d]="320" icon="envelope-simple" eyebrow="Email" title="Engagements"
                        body="Certification, middleware and kernel work, or anything you would rather not post publicly."
                        cta="admin@iso8583.studio" href="mailto:admin@iso8583.studio" />
          <lf-link-card class="ds-item" [style.--d]="430" icon="bug" eyebrow="GitHub Issues" title="Bugs &amp; features"
                        body="Open an issue with your environment, steps and logs. Every issue is read by the person who wrote the code."
                        cta="Open an issue" [href]="repo + '/issues'" />
          <lf-link-card class="ds-item" [style.--d]="540" icon="linkedin-logo" eyebrow="LinkedIn" title="Company page"
                        body="Releases, announcements and the occasional trace worth talking about."
                        cta="Follow" [href]="linkedin" />
        </div>
        <div class="lf-foot ds-hold" [style.--d]="380">
          <span class="lf-foot-line">AiCortex Solutions Pvt. Ltd. · India · {{ release.license }}</span>
          <a class="lf-foot-link" routerLink="/contact">All the ways to reach us <ui-icon name="arrow-right" [size]="13" /></a>
        </div>
      </section>

      <div class="lf-cta">
        <ui-cta-panel>
          <div class="lf-cta-in" uiReveal>
            <div class="ds-hold"><ui-badge tone="teal" [dot]="true">Free and open source</ui-badge></div>
            <h2><ui-words text="Download the studio, or read the source." /></h2>
            <p class="ds-hold" [style.--d]="320">Installers for Windows and macOS, build from source on Linux. Star the repository to follow the releases.</p>
            <div class="cta-actions ds-hold" [style.--d]="440">
              <a class="btn btn--primary btn--lg btn--glow" [href]="releases">Download Studio<ui-icon name="arrow-up-right" [size]="16" /></a>
              <a class="btn btn--secondary btn--lg" [href]="repo" target="_blank" rel="noopener"><ui-icon name="star" [size]="16" />Star on GitHub</a>
            </div>
          </div>
        </ui-cta-panel>
      </div>
    </lf-body>
  `,
})
export class AboutPage {
  protected readonly author = AUTHOR;
  protected readonly release = RELEASE;
  protected readonly repo = EXTERNAL.repo;
  protected readonly releases = EXTERNAL.releases;
  protected readonly linkedin = EXTERNAL.linkedin;
  protected readonly agplUrl = AGPL_URL;
  protected readonly guides = BLOG_POSTS.length;

  protected readonly crumbs = [{ label: 'Home', link: '/' }, { label: 'About' }];
  protected readonly badge: LfBadge = { tone: 'teal', label: 'Open source · AGPL v3' };
  protected readonly meta = [
    ['Since', 'May 2025'], ['Maintainer', AUTHOR.name], ['Publisher', 'AiCortex Solutions Pvt. Ltd.'], ['Release', `v${RELEASE.version}`],
  ] as const;
  protected readonly toc: LfTocEntry[] = [
    { id: 'what', n: '01', label: 'What it is' },
    { id: 'why', n: '02', label: 'Why it exists' },
    { id: 'who', n: '03', label: 'Who builds it' },
    { id: 'built', n: '04', label: 'How it is built' },
    { id: 'milestones', n: '05', label: 'Milestones' },
    { id: 'contact', n: '06', label: 'Get in touch' },
  ];

  protected readonly principles: Principle[] = [
    { icon: 'code', title: 'Open source, all of it', body: 'The simulators, the HSM engine and the calculators are in one AGPL v3 repository. What the studio does to a byte can be read, not guessed.' },
    { icon: 'lock-key', title: 'Keys stay on your machine', body: 'The desktop build makes no network calls of its own. LMKs, BDKs and test PANs are processed locally and are never sent anywhere.' },
    { icon: 'book-open-text', title: 'Documented to the byte', body: 'Every tool and simulator has a reference page with worked examples: real request and response bytes, not just field names.' },
    { icon: 'desktop', title: 'Native desktop, three platforms', body: 'Kotlin Multiplatform and Compose Desktop; installers for Windows and macOS with the runtime bundled, a Gradle build for Linux.' },
  ];

  protected readonly milestones: Milestone[] = [
    { when: 'Jan 2025', what: 'The first guides are published: ISO 8583 message format, bitmaps and response codes.' },
    { when: 'May 2025', what: 'v1.0.0, the first public release: Host and HSM simulators, cipher tools, REST gateway mode and YAML configuration import and export, with Windows and macOS installers.' },
    { when: 'Apr 2026', what: 'v1.0.7 adds the HSM Command Console, a host-command client for payShield, Futurex, Luna, Utimaco and nShield.' },
    { when: RELEASE.dateLabel.replace(/^(\w+) \d+, /, '$1 '), what: `v${RELEASE.version}, the current release: payShield M4 and GW host-command fixes in the HSM simulator.` },
  ];
}
