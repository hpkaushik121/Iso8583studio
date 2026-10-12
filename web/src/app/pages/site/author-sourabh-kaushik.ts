import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SitePage } from './site-page';
import { AUTHOR, EXTERNAL } from '../../core/site-nav';
import { BLOG_POSTS } from '../../content/blog-index';
import { longDate } from '../blog/blog-meta';
import { UiIcon } from '../../ui';
import { LONGFORM, LfBadge, LfTocEntry } from '../shared/longform';

interface Area { icon: string; title: string; body: string; link: string; }

/**
 * Author profile (/authors/sourabh-kaushik): who writes the guides and
 * maintains the studio. Every blog byline and the Person node in the
 * structured data point here, so the name on a post resolves to a page on
 * this site rather than to a GitHub profile.
 *
 * The guide list is the live blog index, newest revision first, so the page
 * never shows a stale "latest".
 */
@Component({
  selector: 'page-author',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [...LONGFORM, RouterLink, UiIcon],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-author' },
  template: `
    <lf-header [crumbs]="crumbs" [badge]="badge" [title]="author.name" [meta]="meta"
               lede="Payments engineer. Builds and maintains ISO8583Studio, writes its guides, and runs the EMV certification and middleware engagements behind it at AiCortex Solutions." />

    <lf-body [toc]="toc">
      <section lfSection id="about" n="01" heading="About" [hold]="false">
        <div class="lf-person ds-hold" [style.--d]="240">
          <span class="lf-avatar" aria-hidden="true">{{ author.initials }}</span>
          <div class="lf-person-text">
            <span class="lf-person-name">{{ author.name }}</span>
            <span class="lf-person-role">Maintainer of ISO8583Studio · author of {{ posts.length }} guides on ISO 8583, EMV, HSM and key management</span>
            <span class="lf-person-links">
              <span class="lf-person-links-k">Profile</span>
              <a [href]="author.github" rel="noopener me"><ui-icon name="github-logo" [size]="14" />GitHub</a>
              <a [href]="author.linkedin" rel="noopener me"><ui-icon name="linkedin-logo" [size]="14" />LinkedIn</a>
              <a routerLink="/contact"><ui-icon name="envelope-simple" [size]="14" />Contact</a>
            </span>
          </div>
        </div>
        <p class="lf-p ds-hold" [style.--d]="340">Sourabh builds the tools he needed on payment integrations and did not have: a host that answers a <code>0200</code> the way a real acquirer does, a payShield that can be run on a laptop, a terminal that boots in an emulator, and calculators that show their work byte by byte. ISO8583Studio is that toolbox, released under the AGPL v3 so that other engineers can read exactly what it does.</p>
        <p class="lf-p ds-hold" [style.--d]="400">The guides on this site come from the same work: the bitmap you misread, the PIN block format a switch expected, the TR-31 header a key-management team could not agree on. Each is written to be checked against a trace, not taken on trust.</p>
        <h3 class="lf-h3 ds-hold" [style.--d]="460">Works on</h3>
        <div class="lf-cards">
          @for (a of areas; track a.title; let i = $index) {
            <lf-link-card class="ds-item" [style.--d]="500 + i * 90" [icon]="a.icon" eyebrow="Area" [title]="a.title"
                          [body]="a.body" cta="See the tools" [href]="a.link" />
          }
        </div>
      </section>

      <section lfSection id="writing" n="02" heading="Writing" [hold]="false">
        <p class="lf-p ds-hold" [style.--d]="240">The most recently revised guides. Each post shows its published and updated dates, and the four reference posts were re-checked against the standards in October 2026.</p>
        <div class="lf-cards">
          @for (p of latest; track p.slug; let i = $index) {
            <lf-link-card class="ds-item" [style.--d]="300 + i * 90" icon="article" [eyebrow]="p.category + ' · ' + when(p)"
                          [title]="p.title" [body]="p.description" cta="Read the guide" [href]="p.path" />
          }
        </div>
        <div class="lf-foot ds-hold" [style.--d]="860">
          <span class="lf-foot-line">{{ posts.length }} guides · {{ topics }} topics</span>
          <a class="lf-foot-link" routerLink="/blogs">All guides <ui-icon name="arrow-right" [size]="13" /></a>
        </div>
      </section>

      <section lfSection id="elsewhere" n="03" heading="Elsewhere" [hold]="false">
        <div class="lf-cards">
          <lf-link-card class="ds-item" [style.--d]="260" icon="github-logo" eyebrow="GitHub" title="hpkaushik121"
                        body="The ISO8583Studio repository, issues and discussions. Pull requests are reviewed by the author." cta="Open GitHub" [href]="author.github" />
          <lf-link-card class="ds-item" [style.--d]="370" icon="linkedin-logo" eyebrow="LinkedIn" title="Sourabh Kaushik"
                        body="Professional profile and the place for engagement enquiries that start as a conversation." cta="Connect" [href]="author.linkedin" />
          <lf-link-card class="ds-item" [style.--d]="480" icon="buildings" eyebrow="Company" title="AiCortex Solutions"
                        body="The publisher of ISO8583Studio: EMV certification, payment middleware, kernel work and hosted simulators." cta="About the studio" href="/about" />
        </div>
      </section>
    </lf-body>
  `,
})
export class AuthorSourabhKaushikPage {
  protected readonly author = AUTHOR;
  protected readonly repo = EXTERNAL.repo;

  protected readonly posts = BLOG_POSTS.filter((p) => p.author === AUTHOR.name);
  protected readonly topics = new Set(this.posts.map((p) => p.topicId)).size;
  /** Six guides, most recently revised first. */
  protected readonly latest = [...this.posts]
    .sort((a, b) => (b.updated || b.date).localeCompare(a.updated || a.date))
    .slice(0, 6);

  protected readonly crumbs = [{ label: 'Home', link: '/' }, { label: 'About', link: '/about' }, { label: AUTHOR.name }];
  protected readonly badge: LfBadge = { tone: 'teal', label: 'Maintainer · ISO8583Studio' };
  protected readonly meta = [
    ['Role', 'Maintainer & author'], ['Guides', String(this.posts.length)], ['GitHub', '@hpkaushik121'],
  ] as const;
  protected readonly toc: LfTocEntry[] = [
    { id: 'about', n: '01', label: 'About' },
    { id: 'writing', n: '02', label: 'Writing' },
    { id: 'elsewhere', n: '03', label: 'Elsewhere' },
  ];

  protected readonly areas: Area[] = [
    { icon: 'arrows-left-right', title: 'ISO 8583 host and switch simulation', body: 'Acquirer and issuer hosts in server, client and proxy mode; transaction rules, templates and unsolicited messages.', link: '/simulator/host' },
    { icon: 'lock-key', title: 'Thales payShield HSM emulation', body: 'Forty-one documented host commands for key management, PIN translation and verification, MAC, RSA and LMK storage.', link: '/simulator/hsm' },
    { icon: 'cards', title: 'EMV kernels and certification', body: 'APDU dialogues, cryptogram and SDA/DDA verification, and L2/L3 certification preparation.', link: '/tools/emv-tools' },
    { icon: 'key', title: 'PIN, key and MAC cryptography', body: 'ISO 9564 PIN blocks, DUKPT, TR-31 key blocks, ISO 9797 MACs and key check values, each with worked examples.', link: '/tools/key-tools' },
  ];

  protected when(p: { date: string; updated: string }): string {
    return p.updated ? `Updated ${longDate(p.updated)}` : longDate(p.date);
  }
}
