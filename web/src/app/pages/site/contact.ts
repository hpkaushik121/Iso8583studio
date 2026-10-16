import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SitePage } from './site-page';
import { UiBadge, UiCtaPanel, UiIcon, UiReveal, UiWords } from '../../ui';
import { LONGFORM } from '../shared/longform';
import { LeadForm } from '../shared/lead-form';

interface Channel { icon: string; eyebrow: string; title: string; body: string; cta: string; href: string; wide?: boolean; }

@Component({
  selector: 'page-contact',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [...LONGFORM, LeadForm, RouterLink, UiBadge, UiCtaPanel, UiIcon, UiReveal, UiWords],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-contact' },
  template: `
    <lf-header layout="split" [crumbs]="crumbs" title="Talk to us"
               lede="Certification engagements, middleware and kernel work, support, or just a question about the studio — pick the channel that fits." />

    <lf-body>
      <app-lead-form surface="contact"
                     heading="Send us an enquiry"
                     lede="Certification, middleware or kernel work — tell us the shape of it and we will come back with specifics." />

      <section id="channels" uiReveal [uiRevealThreshold]="0.08">
        <div class="lf-channels">
          @for (ch of channels; track ch.href; let i = $index) {
            <lf-link-card class="ds-item" [style.--d]="520 + i * 120" [wide]="!!ch.wide" [icon]="ch.icon"
                          [eyebrow]="ch.eyebrow" [title]="ch.title" [body]="ch.body" [cta]="ch.cta" [href]="ch.href" />
          }
        </div>
        <lf-note class="ds-hold" [style.--d]="1000" tone="neutral" icon="clock" title="Response time">Issues and discussions are usually answered within a couple of days. For engagement email, expect a reply within one business week.</lf-note>
      </section>

      <div class="lf-cta">
        <ui-cta-panel>
          <div class="lf-cta-in" uiReveal>
            <div class="ds-hold"><ui-badge tone="blue" icon="sparkle">Pro</ui-badge></div>
            <h2><ui-words text="Looking for a supported, hosted setup?" /></h2>
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
export class ContactPage {
  protected readonly crumbs = [{ label: 'Home', link: '/' }, { label: 'Contact' }];

  /** Eyebrow + title are the old cards' title + badge, which analytics reports. */
  protected readonly channels: Channel[] = [
    { icon: 'envelope-simple', eyebrow: 'Email', title: 'Engagements', body: 'For certification, middleware and kernel engagements, or anything private — write to us directly.', cta: 'admin@iso8583.studio', href: 'mailto:admin@iso8583.studio', wide: true },
    { icon: 'bug', eyebrow: 'GitHub Issues', title: 'Bugs & features', body: 'Found a bug or want a feature in a simulator or tool? Open an issue — it lands straight on the roadmap.', cta: 'Open an issue', href: 'https://github.com/hpkaushik121/Iso8583studio/issues' },
    { icon: 'chats-circle', eyebrow: 'Discussions', title: 'Q&A', body: 'Usage questions, ISO 8583 head-scratchers, and show-and-tell with the community.', cta: 'Start a discussion', href: 'https://github.com/hpkaushik121/Iso8583studio/discussions' },
    { icon: 'linkedin-logo', eyebrow: 'LinkedIn', title: 'Direct', body: 'Follow the ISO8583Studio company page — announcements, releases, and consulting conversations.', cta: 'Connect', href: 'https://www.linkedin.com/company/iso8583-studio' },
  ];
}
