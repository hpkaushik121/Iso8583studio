import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { SitePage } from './site-page';

@Component({
  selector: 'page-contact',
  changeDetection: ChangeDetectionStrategy.OnPush,
  hostDirectives: [SitePage],
  host: { class: 'static-page page-contact' },
  // <image-slot> is a styling-only element the design system owns; see
  // _components.css. Drop this schema once it becomes part of ui-figure.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<main class="doc-body">
        <div class="breadcrumb">
            <a href="/">Home</a><span class="breadcrumb-sep">/</span>
            <span>Contact</span>
        </div>

        <h1 class="page-title">Talk to us</h1>
        <p class="page-description">Certification engagements, middleware and kernel work, support, or just a question about the studio — pick the channel that fits.</p>

        <section class="doc-section" id="channels">
            <div class="hub-grid">
                <a class="hub-card" href="mailto:admin@iso8583.studio"><div class="hub-body"><div class="hub-title">Email <span class="badge badge-teal">Engagements</span></div><p class="hub-desc">For certification, middleware and kernel engagements, or anything private — write to us directly.</p><span class="hub-link">admin@iso8583.studio →</span></div></a>
                <a class="hub-card" href="https://github.com/hpkaushik121/Iso8583studio/issues"><div class="hub-body"><div class="hub-title">GitHub Issues <span class="badge badge-blue">Bugs &amp; features</span></div><p class="hub-desc">Found a bug or want a feature in a simulator or tool? Open an issue — it lands straight on the roadmap.</p><span class="hub-link">Open an issue →</span></div></a>
                <a class="hub-card" href="https://github.com/hpkaushik121/Iso8583studio/discussions"><div class="hub-body"><div class="hub-title">Discussions <span class="badge badge-blue">Q&amp;A</span></div><p class="hub-desc">Usage questions, ISO 8583 head-scratchers, and show-and-tell with the community.</p><span class="hub-link">Start a discussion →</span></div></a>
                <a class="hub-card" href="https://www.linkedin.com/company/iso8583-studio"><div class="hub-body"><div class="hub-title">LinkedIn <span class="badge badge-purple">Direct</span></div><p class="hub-desc">Follow the ISO8583Studio company page — announcements, releases, and consulting conversations.</p><span class="hub-link">Connect →</span></div></a>
            </div>
            <div class="info-card note"><div class="info-card-title">Response time</div><p>Issues and discussions are usually answered within a couple of days. For engagement email, expect a reply within one business week.</p></div>
        </section>
</main>

<aside class="pro-nudge"><span class="pn-tag">✦ Pro</span><p>Looking for a supported, hosted setup? Pro raises the CPS ceiling, unlocks the full algorithm set and deep simulator tweaks, plus hosted endpoints and priority support.</p><a href="/pro">Register for Pro →</a></aside>`,
})
export class ContactPage {}
