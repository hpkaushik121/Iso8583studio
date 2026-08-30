import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { PAYMENTS } from '../../content/payments-config';
import { PaymentsService, formatPaise, messageFor } from '../../core/payments';
import { AnalyticsService } from '../../core/analytics';

/**
 * The Pro registration form.
 *
 * It was static markup carried over from the imported page, with a `novalidate`
 * attribute and an inline script that the import dropped — so nothing validated
 * and nothing submitted. This is the form as a component: every field is a
 * control with rules, and the errors are shown where the mistake is rather than
 * as one message at the bottom.
 *
 * Errors appear once a field has been left, not while it is being typed in.
 * Marking an email invalid at the first character is noise; marking it invalid
 * after the user has moved on is information.
 */

/** Two labels either side of one @, no whitespace, and a dotted TLD. */
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

@Component({
  selector: 'app-pro-form',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form class="pro-form" [formGroup]="form" (ngSubmit)="submit()" novalidate>
      <div class="pf-grid">
        <label class="pf-field">
          <span>Full name</span>
          <input type="text" formControlName="name" autocomplete="name"
                 placeholder="Sourabh Kaushik" [attr.aria-invalid]="invalid('name') || null"
                 [attr.aria-describedby]="invalid('name') ? 'err-name' : null">
          @if (invalid('name')) {
            <em class="pf-err" id="err-name">{{ nameError() }}</em>
          }
        </label>

        <label class="pf-field">
          <span>Work email</span>
          <input type="email" formControlName="email" autocomplete="email"
                 placeholder="you&#64;company.com" [attr.aria-invalid]="invalid('email') || null"
                 [attr.aria-describedby]="invalid('email') ? 'err-email' : null">
          @if (invalid('email')) {
            <em class="pf-err" id="err-email">{{ emailError() }}</em>
          }
        </label>

        <!-- Not decoration: this is prefilled into Razorpay Checkout. Without
             it Checkout opens on its own contact form and asks for a mobile
             number before it will show a payment method. -->
        <label class="pf-field">
          <span>Mobile</span>
          <input type="tel" formControlName="contact" autocomplete="tel"
                 placeholder="+91 98765 43210" inputmode="tel"
                 [attr.aria-invalid]="invalid('contact') || null"
                 [attr.aria-describedby]="invalid('contact') ? 'err-contact' : null">
          @if (invalid('contact')) {
            <em class="pf-err" id="err-contact">{{ contactError() }}</em>
          }
        </label>

        <label class="pf-field">
          <span>Company</span>
          <input type="text" formControlName="company" autocomplete="organization"
                 placeholder="Acquirer, processor or fintech"
                 [attr.aria-invalid]="invalid('company') || null"
                 [attr.aria-describedby]="invalid('company') ? 'err-company' : null">
          @if (invalid('company')) {
            <em class="pf-err" id="err-company">Tell us which organisation this is for.</em>
          }
        </label>

        <label class="pf-field">
          <span>Role</span>
          <input type="text" formControlName="role" autocomplete="organization-title"
                 placeholder="Payments engineer">
        </label>

        <label class="pf-field pf-span">
          <span>What are you testing or certifying?</span>
          <textarea formControlName="usecase" rows="3"
                    placeholder="e.g. RuPay issuer certification, ISO 8583 switch integration, EMV L3"
                    [attr.aria-invalid]="invalid('usecase') || null"></textarea>
          <em class="pf-count" [class.pf-count-over]="usecaseLeft() < 0">
            {{ usecaseLeft() }} characters left
          </em>
        </label>

        <div class="pf-field">
          <span class="pf-label">
            <label for="proAmount">Amount to pay</label>
            <span class="pf-tip-wrap">
              <button type="button" class="pf-tip" aria-describedby="proAmountTip">
                <span aria-hidden="true">i</span><span class="sr-only">Why the amount matters</span>
              </button>
              <span class="pf-tip-bubble" role="tooltip" id="proAmountTip">Pay what Pro is worth to
                you. Early access is provisioned from a queue — <b>the higher the amount, the higher
                your priority in it</b>, so a larger contribution gets your workspace, certification
                packs and support channel opened sooner.</span>
            </span>
          </span>
          <div class="pf-amount">
            <span class="pf-cur">₹</span>
            <input type="number" id="proAmount" formControlName="amount" step="1"
                   min="2" max="100000" inputmode="numeric" placeholder="Enter amount"
                   [attr.aria-invalid]="invalid('amount') || null"
                   [attr.aria-describedby]="invalid('amount') ? 'err-amount' : 'hint-amount'">
          </div>
          @if (invalid('amount')) {
            <em class="pf-err" id="err-amount">{{ amountError() }}</em>
          } @else if (breakdown(); as b) {
            <p class="pf-total" id="hint-amount">
              <span>{{ b.subtotal }}</span>
              <span class="pf-total-op">+</span>
              <span>{{ b.tax }} GST</span>
              <span class="pf-total-op">=</span>
              <b>{{ b.total }}</b>
              <span class="pf-total-note">confirmed at checkout</span>
            </p>
          } @else {
            <p class="pf-hint" id="hint-amount">
              <b>Higher amount → higher priority</b> in the early-access queue.
              GST is added at checkout.
            </p>
          }
        </div>

        <label class="pf-field">
          <span>Billing country</span>
          <input type="text" formControlName="country" autocomplete="country-name"
                 [attr.aria-invalid]="invalid('country') || null">
          @if (invalid('country')) {
            <em class="pf-err">Billing country is required.</em>
          }
        </label>
      </div>

      @if (formError()) {
        <p class="pf-error" role="alert">{{ formError() }}</p>
      }

      <div class="pf-actions">
        <button class="btn btn-blue" type="submit" [disabled]="busy()">
          <span>{{ busy() ? 'Starting payment…' : 'Continue to payment' }}</span>
        </button>
      </div>
    </form>
  `,
})
export class ProForm {
  /** Rupees, matching the tenant's configured minimum and maximum. */
  private static readonly MIN = 2;
  private static readonly MAX = 100000;
  protected static readonly USECASE_MAX = 500;

  protected readonly form = new FormGroup({
    name: new FormControl('', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]),
    email: new FormControl('', [Validators.required, Validators.pattern(EMAIL), Validators.maxLength(254)]),
    contact: new FormControl('', [Validators.required, diallable]),
    company: new FormControl('', [Validators.required, Validators.maxLength(100)]),
    role: new FormControl('', [Validators.maxLength(100)]),
    usecase: new FormControl('', [Validators.maxLength(ProForm.USECASE_MAX)]),
    amount: new FormControl<number | null>(null, [
      Validators.required,
      Validators.min(ProForm.MIN),
      Validators.max(ProForm.MAX),
      wholeNumber,
      unitMultiple,
    ]),
    country: new FormControl('India', [Validators.required]),
  });

  private readonly payments = inject(PaymentsService);
  private readonly analytics = inject(AnalyticsService);

  protected readonly busy = signal(false);
  protected readonly formError = signal<string | null>(null);

  /** Re-read on every change so the template's error state stays current. */
  private readonly value = signal(this.form.getRawValue());
  private readonly status = signal(this.form.status);

  constructor() {
    this.form.valueChanges.subscribe(() => {
      this.value.set(this.form.getRawValue());
      this.status.set(this.form.status);
      // First keystroke anywhere = the form was started. trackOnce dedupes.
      this.analytics.reportFormStart();
      // What people think Pro is worth — reported once per view, valid only.
      const amount = this.form.get('amount');
      if (amount?.valid && amount.value) {
        this.analytics.reportAmountEntered(Number(amount.value));
      }
    });
  }

  protected readonly usecaseLeft = computed(() =>
    ProForm.USECASE_MAX - (this.value().usecase?.length ?? 0));

  /**
   * What the amount actually costs, shown while it is being typed.
   *
   * Nothing exists to ask at this point — a quote is only created on submit —
   * so this is computed here, in whole paise and rounded the way the service
   * documents, and labelled as confirmed at checkout because the service's
   * figure is the one that binds. startCheckout compares the two.
   */
  protected readonly breakdown = computed(() => {
    const rupees = Number(this.value().amount);
    if (!Number.isFinite(rupees) || rupees <= 0) return null;
    const { subtotal, tax, total } = taxOn(rupees);
    return { subtotal: formatPaise(subtotal), tax: formatPaise(tax), total: formatPaise(total), totalPaise: total };
  });

  /** Invalid, and the user has finished with the field or tried to submit. */
  protected invalid(name: string): boolean {
    this.value();
    const c = this.form.get(name);
    return !!c && c.invalid && (c.touched || c.dirty);
  }

  protected nameError(): string {
    const e = this.form.get('name')?.errors ?? {};
    if (e['required']) return 'Your name is required.';
    if (e['minlength']) return 'That looks too short to be a name.';
    return 'That name is too long.';
  }

  protected emailError(): string {
    const e = this.form.get('email')?.errors ?? {};
    if (e['required']) return 'A work email is required — this is where credentials are sent.';
    return 'That does not look like an email address.';
  }

  protected contactError(): string {
    const e = this.form.get('contact')?.errors ?? {};
    if (e['required']) return 'A mobile number is required — checkout asks for one otherwise.';
    return 'Enter a mobile number with its country code, or a 10-digit Indian number.';
  }

  protected amountError(): string {
    const e = this.form.get('amount')?.errors ?? {};
    if (e['required']) return 'Enter the amount you want to pay.';
    if (e['notWhole']) return 'Enter a whole number of rupees.';
    if (e['notMultiple']) return `Enter a multiple of ₹${PAYMENTS.unitRupees}.`;
    if (e['min']) return `The minimum is ₹${ProForm.MIN}.`;
    return `The maximum is ₹${ProForm.MAX.toLocaleString('en-IN')} — write to us for anything larger.`;
  }

  protected submit(): void {
    this.formError.set(null);
    // Touch everything, so a submit with empty fields explains itself rather
    // than doing nothing visible.
    this.form.markAllAsTouched();
    this.value.set(this.form.getRawValue());

    if (this.form.invalid) {
      this.analytics.reportFormSubmit(false);
      // Names only, never values — the form holds PII.
      this.analytics.reportFormError(
        Object.keys(this.form.controls).filter((k) => this.form.get(k)?.invalid));
      this.formError.set('Check the highlighted fields and try again.');
      return;
    }

    this.analytics.reportFormSubmit(true, Number(this.form.getRawValue().amount));
    void this.startCheckout();
  }

  /**
   * Browser-only checkout: name a price point, never an amount.
   *
   * With no price point published the form has nothing it is allowed to sell,
   * so it says so rather than making a call that would answer 403. The amount
   * field cannot drive this — see PaymentsService for why a page may not set a
   * price — and becomes a plan choice when the SKUs exist.
   */
  private async startCheckout(): Promise<void> {
    if (!this.payments.configured) {
      console.warn(`Pro checkout is off — ${this.payments.unconfiguredReason}`);
      this.formError.set(
        'Payment is not connected yet. Write to admin@iso8583.studio and we will set you up directly.');
      return;
    }

    const { email, contact, name } = this.form.getRawValue();
    this.busy.set(true);
    try {
      const shown = this.breakdown()?.totalPaise;
      const { checkoutUrl, amountPaise } = await this.payments.createCheckout({
        pricePoint: PAYMENTS.pricePoints[0],
        // A page may not name a price, so it names how many units it wants
        // and the catalog prices them. The service computes and freezes the
        // total; the amount never travels in anything the customer can edit.
        quantity: this.quantityForAmount(),
        email: email!,
        // Validated above, so this normalises. Sent as E.164 because Checkout
        // prefills it, and an unprefilled number costs the customer a screen.
        contact: normaliseContact(contact ?? '')!,
        name: (name ?? '').trim() || undefined,
        // No accounts here, so the customer's own email is the stable key.
        ref: email!,
        notes: this.notesFromForm(),
      });
      // A mismatch means the catalog's tax no longer matches PAYMENTS_TAX_BPS.
      // The customer is charged the service's figure either way — the hosted
      // page shows it — but the estimate they were given was wrong, and that
      // should be found in a log rather than in a complaint.
      if (shown !== undefined && shown !== amountPaise) {
        console.warn(`Pro checkout: showed ₹${shown / 100} but the quote is ₹${amountPaise / 100}. `
          + 'PAYMENTS_TAX_BPS is out of step with the price point.');
      }
      // begin_checkout carries the amount the service froze — the only
      // authoritative figure — and the redirect waits for the beacon (max
      // 400ms). checkout_id stitches this to the purchase on return.
      const checkoutId = crypto.randomUUID();
      this.payments.rememberCheckoutId(checkoutId);
      this.analytics.reportBeginCheckout(amountPaise, checkoutId, () => {
        // The token is already stored; leaving the site is the last thing we do.
        location.assign(checkoutUrl);
      });
    } catch (err) {
      this.busy.set(false);
      const e = err as { code?: string; message?: string; requestId?: string | null };
      this.analytics.reportCheckoutError(e.code ?? 'network_or_cors');
      if (e.code) {
        console.error(`payments ${e.code} (request ${e.requestId ?? 'unknown'}): ${e.message}`);
        this.formError.set(messageFor(e as never));
      } else {
        // fetch threw rather than answering, so there is no code to branch on.
        // In a browser that is usually a blocked preflight, not a bad network:
        // an origin outside the tenant's cors_allowed_origins gets a bare 204
        // with no allow headers and the call never leaves. The two are
        // indistinguishable from here, so say so rather than guess.
        console.warn(
          `Pro checkout: the request to ${PAYMENTS.baseUrl} did not complete. If `
          + `${location.origin} is not in the tenant's cors_allowed_origins, the browser blocks `
          + 'it at preflight and this looks identical to a network failure. Only the deployed '
          + 'origin is allowlisted, so checkout cannot be exercised from a dev server.', err);
        this.formError.set('Payment could not be started. Check your connection and try again.');
      }
    }
  }

  /**
   * The chosen amount expressed in catalog units.
   *
   * The validator keeps the amount a whole multiple of the unit price, so this
   * divides exactly. The tenant's browser_max_quantity has to be at least the
   * largest quantity this can produce, or the call is refused.
   */
  private quantityForAmount(): number {
    return Math.round(Number(this.form.getRawValue().amount) / PAYMENTS.unitRupees);
  }

  /** What the team needs to fulfil by hand, carried on the payment. */
  private notesFromForm(): Record<string, string> {
    const v = this.form.getRawValue();
    return {
      company: v.company ?? '',
      role: v.role ?? '',
      usecase: (v.usecase ?? '').slice(0, 200),
      country: v.country ?? '',
    };
  }
}

/**
 * A typed number as E.164, or null if it cannot be read as one.
 *
 * The service strips separators itself, so this is not about formatting — it
 * is about supplying the country code. A bare ten-digit Indian mobile is what
 * people actually type, and sending it unqualified prefills a number Checkout
 * cannot dial. Anything already carrying a `+` is passed through, so a
 * customer billing outside India is not forced into +91.
 */
function normaliseContact(raw: string): string | null {
  const t = raw.trim();
  if (!t) return null;
  const digits = t.replace(/[^\d]/g, '');

  let e164: string | null = null;
  if (t.startsWith('+')) e164 = /^\d{8,15}$/.test(digits) ? `+${digits}` : null;
  // 0XXXXXXXXXX — the domestic trunk prefix, which E.164 drops.
  else if (/^0\d{10}$/.test(digits)) e164 = `+91${digits.slice(1)}`;
  else if (/^91\d{10}$/.test(digits)) e164 = `+${digits}`;
  else if (/^[6-9]\d{9}$/.test(digits)) e164 = `+91${digits}`;
  if (!e164) return null;

  // The generic length range above cannot tell a short +91 number from a
  // legitimately shorter foreign one, and +91 is almost every number typed
  // here. Its rule is known — ten digits starting 6-9 — so a digit dropped
  // while typing is caught now rather than at Checkout.
  if (e164.startsWith('+91') && !/^\+91[6-9]\d{9}$/.test(e164)) return null;
  return e164;
}

function diallable(control: { value: unknown }) {
  const v = control.value;
  if (v === null || v === undefined || v === '') return null;
  return normaliseContact(String(v)) ? null : { notDiallable: true };
}

/** Integer paise throughout, half-up once, matching how the service rounds. */
function taxOn(rupees: number): { subtotal: number; tax: number; total: number } {
  const subtotal = Math.round(rupees * 100);
  const tax = Math.floor((subtotal * PAYMENTS.taxBps) / 10000 + 0.5);
  return { subtotal, tax, total: subtotal + tax };
}

/**
 * The amount is bought as whole catalog units, so it has to sit on a unit
 * boundary. With the usual ₹1 unit every whole rupee qualifies and this never
 * fires; it earns its place if the unit is ever coarser than that.
 */
function unitMultiple(control: { value: unknown }) {
  const v = Number(control.value);
  if (control.value === null || control.value === '' || Number.isNaN(v)) return null;
  return v % PAYMENTS.unitRupees === 0 ? null : { notMultiple: true };
}

/** `type="number"` accepts 12.5; rupees here are whole. */
function wholeNumber(control: { value: unknown }) {
  const v = control.value;
  if (v === null || v === '' || v === undefined) return null;
  return Number.isInteger(Number(v)) ? null : { notWhole: true };
}
