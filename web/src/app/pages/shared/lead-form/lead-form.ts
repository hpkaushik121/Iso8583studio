import {
  ChangeDetectionStrategy, Component, DOCUMENT, ElementRef, PLATFORM_ID, afterNextRender, inject,
  input, signal, viewChild,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { AnalyticsService } from '../../../core/analytics';
import { Observability } from '../../../core/observability';
import { LEADS } from '../../../content/leads-config';
import { UiIcon } from '../../../ui';

/** Good enough to catch a typo; the real check is whether the reply arrives. */
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

/** Set before the form navigates, so a pasted ?enquiry=sent proves nothing. */
const PENDING_KEY = 'iso8583_enquiry_pending';

/**
 * The enquiry form. A real form POST to Web3Forms, which emails it on.
 *
 * A top-level form submission is not subject to CORS, which is the whole
 * reason it is shaped this way: a fetch to the same endpoint is, and it fails
 * wherever Web3Forms declines to answer with an allow-origin header. The
 * browser navigates to Web3Forms, which processes the message and sends the
 * visitor back to `redirect`.
 *
 * That return is the confirmation. It is the same shape as the payment return
 * in CheckoutOutcome, and it is believed on the same terms: the page only
 * treats `?enquiry=sent` as real when this browser set the pending flag before
 * leaving. Otherwise a pasted or bookmarked URL would report a lead that never
 * happened, and `generate_lead` is a Google Ads conversion.
 *
 * So the funnel reports what actually occurred rather than what was hoped: the
 * submission is counted when it leaves, and the lead is counted only once the
 * visitor comes back from Web3Forms having had it accepted.
 *
 * New Relic gets the funnel and none of the submitted fields. Those are a
 * lead, not a diagnostic — they go to the inbox, where someone can reply. The
 * GA4 layer is kept clear of them too.
 */
@Component({
  selector: 'app-lead-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiIcon],
  /* data-sect here rather than on each page: SitePage's observer selects
     '[data-sect], section, .doc-section', so the form reports its own
     section_view wherever it is dropped in. It renders no <section> of its
     own, so nothing is double-counted. */
  host: { 'class': 'lead-form-host', 'data-sect': 'enquiry' },
  template: `
    <div class="lead-form" [id]="anchor()">
      <div class="lead-form-head">
        <h2 class="lead-form-title">{{ heading() }}</h2>
        <p class="lead-form-lede">{{ lede() }}</p>
      </div>

      @switch (state()) {
        @case ('sent') {
          <div class="lead-box lead-box--ok" role="status">
            <span class="lead-box-title"><ui-icon name="check-circle" [size]="20" />Thanks — that's with us</span>
            <span class="lead-box-text">We answer engagement enquiries within one business week, usually sooner.</span>
          </div>
        }
        @case ('offline') {
          <div class="lead-box lead-box--off" role="status">
            <span class="lead-box-title"><ui-icon name="envelope-simple" [size]="20" />Write to us directly</span>
            <span class="lead-box-text">
              The enquiry form is not connected yet. Email
              <a href="mailto:{{ inbox }}">{{ inbox }}</a> and we will pick it up.
            </span>
          </div>
        }
        @default {
          <form class="lead-fields" method="POST" [action]="endpoint" novalidate
                (submit)="onSubmit($event)">
            <input type="hidden" name="access_key" [value]="key">
            <input type="hidden" name="from_name" value="ISO8583Studio website">
            <input type="hidden" name="interest" [value]="surface()">
            <!-- Filled in at submit time: both depend on what was typed and on
                 the page the form is sitting on. -->
            <input #subject type="hidden" name="subject" value="Website enquiry">
            <input #replyto type="hidden" name="replyto" value="">
            <input #redirect type="hidden" name="redirect" value="">

            <!-- Web3Forms drops a submission whose botcheck is set, so this is
                 checked at their end as well as here. Off-screen rather than
                 hidden: a form-filler fills what it can reach. -->
            <div class="lead-trap" aria-hidden="true">
              <label>Leave this empty
                <input #trap type="checkbox" name="botcheck" tabindex="-1" autocomplete="off">
              </label>
            </div>

            <label class="lead-field" [class.is-invalid]="invalid().includes('name')">
              <span class="lead-label">Name</span>
              <input #name type="text" name="name" autocomplete="name" maxlength="80"
                     placeholder="Your name" (input)="started()">
            </label>

            <label class="lead-field" [class.is-invalid]="invalid().includes('email')">
              <span class="lead-label">Work email</span>
              <input #email type="email" name="email" autocomplete="email" maxlength="100"
                     placeholder="you&#64;company.com" (input)="started()">
            </label>

            <label class="lead-field" [class.is-invalid]="invalid().includes('company')">
              <span class="lead-label">Company</span>
              <input #company type="text" name="company" autocomplete="organization" maxlength="200"
                     placeholder="Acme Payments" (input)="started()">
            </label>

            <label class="lead-field">
              <span class="lead-label">Phone <span class="lead-opt">optional</span></span>
              <input type="tel" name="phone" autocomplete="tel" maxlength="50"
                     placeholder="+91 98765 43210" (input)="started()">
            </label>

            <label class="lead-field lead-field--wide">
              <span class="lead-label">What do you need?</span>
              <textarea name="message" rows="4" maxlength="4000" (input)="started()"
                        placeholder="The integration, the certification scope, or the problem you are trying to test."></textarea>
            </label>

            @if (error(); as e) { <p class="lead-err" role="alert">{{ e }}</p> }

            <div class="lead-actions">
              <button class="btn btn--primary btn--glow" type="submit">
                Send enquiry<ui-icon name="arrow-right" [size]="16" />
              </button>
              <span class="lead-fineprint">Goes straight to a human. No newsletter, no sharing.</span>
            </div>
          </form>
        }
      }
    </div>
  `,
})
export class LeadForm {
  /** Which page this sits on; reported as cta_location and sent with the enquiry. */
  readonly surface = input.required<string>();
  readonly heading = input('Talk to us');
  readonly lede = input('Tell us what you are building and we will come back with specifics, not a brochure.');
  /** Id on the wrapper, so a page rail or a CTA can link straight to it. */
  readonly anchor = input('enquiry');

  private readonly analytics = inject(AnalyticsService);
  private readonly obs = inject(Observability);
  private readonly doc = inject(DOCUMENT);
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

  private readonly subjectEl = viewChild<ElementRef<HTMLInputElement>>('subject');
  private readonly replytoEl = viewChild<ElementRef<HTMLInputElement>>('replyto');
  private readonly redirectEl = viewChild<ElementRef<HTMLInputElement>>('redirect');
  private readonly trapEl = viewChild<ElementRef<HTMLInputElement>>('trap');
  private readonly nameEl = viewChild<ElementRef<HTMLInputElement>>('name');
  private readonly emailEl = viewChild<ElementRef<HTMLInputElement>>('email');
  private readonly companyEl = viewChild<ElementRef<HTMLInputElement>>('company');

  protected readonly endpoint = LEADS.endpoint;
  protected readonly key = LEADS.key;
  protected readonly inbox = LEADS.inbox;
  protected readonly state = signal<'form' | 'sent' | 'offline'>(
    LEADS.configured ? 'form' : 'offline');
  protected readonly invalid = signal<string[]>([]);
  protected readonly error = signal<string | null>(null);

  constructor() {
    afterNextRender(() => this.resolveReturn());
  }

  /**
   * Reads the return from Web3Forms, and only believes it when this browser
   * started the submission — see PENDING_KEY.
   */
  private resolveReturn(): void {
    const win = this.doc.defaultView;
    if (!win) return;

    const params = new URLSearchParams(win.location.search);
    if (params.get('enquiry') !== 'sent') return;

    let pending: string | null = null;
    try {
      pending = win.sessionStorage.getItem(PENDING_KEY);
      win.sessionStorage.removeItem(PENDING_KEY);
    } catch { /* private mode: treated as not pending */ }

    // Clear the flag off the URL either way, so a reload cannot replay it and
    // the address bar does not keep a state the page has already consumed.
    const clean = win.location.pathname + win.location.hash;
    try { win.history.replaceState({}, '', clean); } catch { /* ignore */ }

    // Another surface's form on the same page: leave it to that instance.
    if (pending !== this.surface()) return;

    this.state.set('sent');
    this.analytics.reportLead(this.surface());
    this.obs.action('lead_submit_delivered', { surface: this.surface() });
  }

  protected started(): void {
    this.analytics.reportLeadStart(this.surface());
  }

  protected onSubmit(event: Event): void {
    // A ticked trap is a bot. Drop it silently — nothing sent, nothing counted.
    if (this.trapEl()?.nativeElement.checked) {
      event.preventDefault();
      this.obs.action('lead_submit_blocked', { surface: this.surface(), reason: 'honeypot' });
      return;
    }

    const name = this.nameEl()?.nativeElement.value.trim() ?? '';
    const email = this.emailEl()?.nativeElement.value.trim() ?? '';
    const company = this.companyEl()?.nativeElement.value.trim() ?? '';

    const missing: string[] = [];
    if (!name) missing.push('name');
    if (!EMAIL.test(email)) missing.push('email');
    if (!company) missing.push('company');

    if (missing.length) {
      event.preventDefault();
      this.invalid.set(missing);
      this.error.set(
        missing.includes('email') && missing.length > 1
          ? 'We need your name, company and a valid work email to reply.'
        : missing.length > 1 ? 'Add your name and company so we know who we are talking to.'
        : missing[0] === 'email' ? 'That email does not look right — we reply to this address.'
        : missing[0] === 'company' ? 'Add your company so we know who we are talking to.'
        : 'Add your name so we know who we are talking to.');
      this.analytics.reportLeadError(this.surface(), missing);
      this.obs.action('lead_submit_blocked', {
        surface: this.surface(), reason: 'validation', fields: missing.join('|'),
      });
      return;
    }

    this.invalid.set([]);
    this.error.set(null);

    // The three fields that can only be known now. Without JS these stay at
    // their defaults and the enquiry still sends — it simply lands on
    // Web3Forms' own thank-you page instead of coming back here.
    const win = this.doc.defaultView;
    const subject = this.subjectEl()?.nativeElement;
    if (subject) subject.value = `Enquiry from ${company} — ${this.surface()}`;
    // So that replying in the inbox answers the person who wrote, not the form.
    const replyto = this.replytoEl()?.nativeElement;
    if (replyto) replyto.value = email;
    const redirect = this.redirectEl()?.nativeElement;
    if (redirect && win) {
      redirect.value = `${win.location.origin}${win.location.pathname}?enquiry=sent`;
    }

    // What makes the return believable, and what distinguishes two forms on
    // one page. Written before the navigation, which happens next.
    if (this.isBrowser) {
      try { win?.sessionStorage.setItem(PENDING_KEY, this.surface()); } catch { /* private mode */ }
    }

    this.obs.action('lead_submit_sent', { surface: this.surface() });
    // Not prevented: the browser now posts to Web3Forms and navigates.
  }
}
