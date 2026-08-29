import { ChangeDetectionStrategy, Component, computed, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

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
          } @else {
            <p class="pf-hint" id="hint-amount">
              <b>Higher amount → higher priority</b> in the early-access queue.
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
    company: new FormControl('', [Validators.required, Validators.maxLength(100)]),
    role: new FormControl('', [Validators.maxLength(100)]),
    usecase: new FormControl('', [Validators.maxLength(ProForm.USECASE_MAX)]),
    amount: new FormControl<number | null>(null, [
      Validators.required,
      Validators.min(ProForm.MIN),
      Validators.max(ProForm.MAX),
      wholeNumber,
    ]),
    country: new FormControl('India', [Validators.required]),
  });

  protected readonly busy = signal(false);
  protected readonly formError = signal<string | null>(null);

  /** Re-read on every change so the template's error state stays current. */
  private readonly value = signal(this.form.getRawValue());
  private readonly status = signal(this.form.status);

  constructor() {
    this.form.valueChanges.subscribe(() => {
      this.value.set(this.form.getRawValue());
      this.status.set(this.form.status);
    });
  }

  protected readonly usecaseLeft = computed(() =>
    ProForm.USECASE_MAX - (this.value().usecase?.length ?? 0));

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

  protected amountError(): string {
    const e = this.form.get('amount')?.errors ?? {};
    if (e['required']) return 'Enter the amount you want to pay.';
    if (e['notWhole']) return 'Enter a whole number of rupees.';
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
      this.formError.set('Check the highlighted fields and try again.');
      return;
    }

    // Payment is not wired up yet: this site has no backend, and the browser-only
    // shape of the payments service cannot accept a customer-named amount.
    // See .design-sync/NOTES.md — the form validates and collects, and the call
    // is added once the pricing model is settled.
    this.formError.set(
      'Payment is not connected yet. Write to admin@iso8583.studio and we will set you up directly.');
  }
}

/** `type="number"` accepts 12.5; rupees here are whole. */
function wholeNumber(control: { value: unknown }) {
  const v = control.value;
  if (v === null || v === '' || v === undefined) return null;
  return Number.isInteger(Number(v)) ? null : { notWhole: true };
}
