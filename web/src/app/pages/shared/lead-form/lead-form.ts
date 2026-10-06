import {
  ChangeDetectionStrategy, Component, ElementRef, computed, inject, input, signal, viewChild,
} from '@angular/core';
import { AnalyticsService } from '../../../core/analytics';
import { LEADS } from '../../../content/leads-config';
import { UiIcon } from '../../../ui';

/** Good enough to catch a typo; the real check is whether the reply arrives. */
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

/**
 * The enquiry form, posted straight into Zoho CRM as a lead.
 *
 * Why a native form POST into a hidden iframe rather than fetch(): Zoho's
 * WebToLeadForm endpoint sends no CORS headers, so a fetch is blocked, and a
 * no-cors fetch returns an opaque response that cannot be told apart from a
 * failure. A real form POST is not subject to CORS at all, and targeting a
 * hidden iframe keeps the visitor on the page instead of bouncing them through
 * Zoho's own thank-you screen. The iframe's load event is the signal that a
 * response came back.
 *
 * What that buys and what it does not: we learn the round trip completed, not
 * that Zoho accepted the record — the response is cross-origin and unreadable.
 * A rejected lead therefore still shows as sent here. That is the standard
 * trade for web-to-lead, and the reason the honeypot matters: it is cheaper to
 * drop a bot before Zoho sees it than to reconcile junk afterwards.
 *
 * Fields are Zoho's standard Leads names, spaces and all. Only standard ones
 * are used, so this works against a stock Leads module with no custom fields:
 * the surface and the message are folded into Description instead. `Last Name`
 * is mandatory in Zoho, which is why there is a single Name field mapped to it.
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
              <a href="mailto:admin&#64;iso8583.studio">admin&#64;iso8583.studio</a> and we will pick it up.
            </span>
          </div>
        }
        @default {
          <!-- The POST target. Named, so the browser routes the response here
               and the page never navigates. The name is derived from the
               surface rather than randomised: hydration compares the
               prerendered attribute with the client's. -->
          <iframe class="lead-sink" [name]="sinkName()" title="Enquiry submission target"
                  aria-hidden="true" tabindex="-1" (load)="onSinkLoad()"></iframe>

          <!-- novalidate on purpose: native constraint validation cancels the
               submit event before any handler runs, so the inline messages
               below never showed and form_error was never reported. The checks
               in onSubmit replace it, and they are the ones measured. -->
          <form class="lead-fields" method="POST" [action]="action" [target]="sinkName()"
                accept-charset="UTF-8" novalidate (submit)="onSubmit($event)">
            <!-- Zoho's form identifiers. Public by construction; see leads-config. -->
            <input type="hidden" name="xnQsjsdp" [value]="id">
            <input type="hidden" name="xmIwtLD" [value]="token">
            <input type="hidden" name="actionType" [value]="actionType">
            <!-- Only when configured: Lead Source is a picklist, and a value
                 outside its options is not stored. See build-leads-config. -->
            @if (source) { <input type="hidden" name="Lead Source" [value]="source"> }
            <!-- Composed at submit time, not bound: the message is read from
                 the textarea then, and a binding would ship an empty string. -->
            <input #desc type="hidden" name="Description" value="">

            <!-- Bot bait: off-screen, not hidden, and never sent to Zoho —
                 a filled trap aborts before the POST. A real visitor cannot
                 reach it; a form-filler fills everything it finds. -->
            <div class="lead-trap" aria-hidden="true">
              <label>Leave this empty
                <!-- No name attribute: an unnamed control is not serialised,
                     so this never reaches Zoho even as an empty field. -->
                <input #trap type="text" tabindex="-1" autocomplete="off">
              </label>
            </div>

            <label class="lead-field" [class.is-invalid]="invalid().includes('name')">
              <span class="lead-label">Name</span>
              <input #name type="text" name="Last Name" autocomplete="name"
                     placeholder="Your name" (input)="started()">
            </label>

            <label class="lead-field" [class.is-invalid]="invalid().includes('email')">
              <span class="lead-label">Work email</span>
              <input #email type="email" name="Email" autocomplete="email"
                     placeholder="you&#64;company.com" (input)="started()">
            </label>

            <label class="lead-field">
              <span class="lead-label">Company <span class="lead-opt">optional</span></span>
              <input type="text" name="Company" autocomplete="organization"
                     placeholder="Acme Payments" (input)="started()">
            </label>

            <label class="lead-field">
              <span class="lead-label">Phone <span class="lead-opt">optional</span></span>
              <input type="tel" name="Phone" autocomplete="tel"
                     placeholder="+91 98765 43210" (input)="started()">
            </label>

            <label class="lead-field lead-field--wide">
              <span class="lead-label">What do you need?</span>
              <textarea #message rows="4" (input)="started()"
                        placeholder="The integration, the certification scope, or the problem you are trying to test."></textarea>
            </label>

            @if (error(); as e) { <p class="lead-err" role="alert">{{ e }}</p> }

            <div class="lead-actions">
              <button class="btn btn--primary btn--glow" type="submit" [disabled]="state() === 'sending'">
                {{ state() === 'sending' ? 'Sending…' : 'Send enquiry' }}
                <ui-icon name="arrow-right" [size]="16" />
              </button>
              <span class="lead-fineprint">Goes to our CRM and to a human. No newsletter, no sharing.</span>
            </div>
          </form>
        }
      }
    </div>
  `,
})
export class LeadForm {
  /** Which page this sits on; reported as cta_location and folded into Description. */
  readonly surface = input.required<string>();
  readonly heading = input('Talk to us');
  readonly lede = input('Tell us what you are building and we will come back with specifics, not a brochure.');
  /** Id on the wrapper, so a page rail or a CTA can link straight to it. */
  readonly anchor = input('enquiry');

  private readonly analytics = inject(AnalyticsService);
  private readonly descEl = viewChild<ElementRef<HTMLInputElement>>('desc');
  private readonly trapEl = viewChild<ElementRef<HTMLInputElement>>('trap');
  private readonly nameEl = viewChild<ElementRef<HTMLInputElement>>('name');
  private readonly emailEl = viewChild<ElementRef<HTMLInputElement>>('email');
  private readonly messageEl = viewChild<ElementRef<HTMLTextAreaElement>>('message');

  protected readonly action = LEADS.action;
  protected readonly id = LEADS.id;
  protected readonly token = LEADS.token;
  protected readonly actionType = LEADS.actionType;
  protected readonly source = LEADS.source;
  /** Stable across server and client, and unique while one surface holds one form. */
  protected readonly sinkName = computed(() => `lead-sink-${this.surface()}`);

  protected readonly state = signal<'form' | 'sending' | 'sent' | 'offline'>(
    LEADS.configured ? 'form' : 'offline');
  protected readonly invalid = signal<string[]>([]);
  protected readonly error = signal<string | null>(null);

  /** Set when we submit, so the iframe's own blank first load is not read as a reply. */
  private awaiting = false;

  protected started(): void {
    this.analytics.reportLeadStart(this.surface());
  }

  protected onSubmit(event: Event): void {
    // A filled trap is a bot. Drop it silently — no error, no analytics, and
    // above all no POST, so Zoho never sees the record.
    if (this.trapEl()?.nativeElement.value) { event.preventDefault(); return; }

    const missing: string[] = [];
    const name = this.nameEl()?.nativeElement.value.trim() ?? '';
    const email = this.emailEl()?.nativeElement.value.trim() ?? '';
    if (!name) missing.push('name');
    if (!EMAIL.test(email)) missing.push('email');

    if (missing.length) {
      event.preventDefault();
      this.invalid.set(missing);
      this.error.set(
        missing.length === 2 ? 'Add your name and a work email so we can reply.'
        : missing[0] === 'email' ? 'That email does not look right — we reply to this address.'
        : 'Add your name so we know who we are talking to.');
      this.analytics.reportLeadError(this.surface(), missing);
      return;
    }

    // Zoho gets the context in Description, so no custom field is needed.
    const body = this.messageEl()?.nativeElement.value.trim() ?? '';
    const desc = this.descEl()?.nativeElement;
    if (desc) desc.value = `Interest: ${this.surface()}\n\n${body}`;

    this.invalid.set([]);
    this.error.set(null);
    this.awaiting = true;
    this.state.set('sending');
    // Not prevented: the browser now performs the POST into the hidden iframe.
    this.analytics.reportLead(this.surface());
  }

  protected onSinkLoad(): void {
    if (!this.awaiting) return;   // the iframe's own blank first load
    this.awaiting = false;
    this.state.set('sent');
  }
}
