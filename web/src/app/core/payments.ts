import { Injectable, inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { PAYMENTS } from '../content/payments-config';

/**
 * Shape C of the payments integration guide — browser-only.
 *
 * This site is prerendered static files on Pages with nowhere to run
 * request-time code, so shapes A and B are not available to it: both need a
 * backend to create the quote and to receive the signed callback.
 *
 * Two consequences of that are not incidental, and are the reason this file is
 * small:
 *
 *  - A page may never name an amount. It names a `price_point` we have been
 *    given and the service prices it. Sending `amount_paise` from a browser
 *    credential is 403 `amount_not_permitted`, by design.
 *
 *  - There is no signed callback, so nothing here may grant an entitlement.
 *    The guide is explicit: browser-only is fine for a payment you fulfil by
 *    hand or by email, and not fine for unlocking access automatically. Pro
 *    workspaces are provisioned by a human off the back of this.
 *
 * On return we poll `/c/{token}/status` and never `/c/{token}/verify` — the
 * hosted checkout has already verified, and calling verify with no Razorpay
 * parameters answers 400 `invalid_razorpay_payment_id`.
 */

const TOKEN_KEY = 'iso8583studio.checkout_token';
/**
 * The quote's own total, kept so the result screen can lead with the amount.
 *
 * It is stored beside the token rather than recomputed, because the figure the
 * service froze is the one that was charged — the form's estimate is not
 * authoritative and, after the redirect, is gone anyway.
 */
const AMOUNT_KEY = 'iso8583studio.checkout_amount';
/**
 * An analytics-only id minted at begin_checkout and read back on return, so
 * the funnel can stitch begin_checkout to purchase. It is never the token —
 * the token is the authorisation and must not reach any analytics payload.
 */
const CHECKOUT_ID_KEY = 'iso8583studio.checkout_id';
/** The quote's currency, kept beside its amount: minor units mean nothing without it. */
const CURRENCY_KEY = 'iso8583studio.checkout_currency';
/**
 * The work email, held across the redirect for one purpose: the Google Ads
 * Enhanced Conversions match.
 *
 * localStorage for the same reason as the token — a UPI or netbanking app
 * returns the customer in a different tab. gtag normalises and SHA-256-hashes
 * this before it leaves the browser; it is never a GA4 parameter and never
 * enters a URL. Read-and-cleared on return, and dropped with the token when a
 * checkout is abandoned, so it is not left sitting there.
 */
const EC_EMAIL_KEY = 'iso8583studio.ec_email';
/**
 * Fallback customer key for contexts without `crypto.subtle` (insecure
 * origins). Per-browser rather than per-person, which is the cost of not
 * having a hash — see customerRef().
 */
const CUSTOMER_REF_KEY = 'iso8583studio.customer_ref';

export type CheckoutStatus = 'paid' | 'processing' | 'pending' | 'failed';

export interface StatusResult {
  status: CheckoutStatus;
  /** Present on the terminal states. */
  redirect?: string;
  /** Present while the answer is not yet known. Honour it. */
  poll_after_ms?: number;
}

export interface PaymentsError {
  code: string;
  message: string;
  /** Every response carries one; it is how the payments team finds the call. */
  requestId: string | null;
  /** From `Retry-After`, where the service asks to be backed off from. */
  retryAfterMs?: number;
}

@Injectable({ providedIn: 'root' })
export class PaymentsService {
  private readonly doc = inject(DOCUMENT);

  /**
   * Checkout needs both halves: a credential to call with, and a SKU to name.
   * A page may not name a price, so with no price point there is nothing it is
   * allowed to sell and no call worth making.
   */
  readonly configured = !!PAYMENTS.publicKey && PAYMENTS.pricePoints.length > 0;

  /** Which half is missing, for the console — the customer sees one message. */
  readonly unconfiguredReason: string | null = !PAYMENTS.publicKey
    ? 'PAYMENTS_PUB_KEY is not set: the build had no browser credential to bake in.'
    : PAYMENTS.pricePoints.length === 0
      ? 'PAYMENTS_PRICE_POINTS is empty: publish a unit SKU in the tenant\'s '
        + 'browser_checkout_price_points and set it, with PAYMENTS_UNIT_RUPEES.'
      : null;

  /**
   * Creates a checkout and hands back where to send the customer.
   *
   * The token is persisted before the caller redirects, because after the
   * redirect it is the only handle on the payment — `checkout_url` is returned
   * once and never again, and the service stores only its hash.
   */
  async createCheckout(input: {
    pricePoint: string;
    quantity?: number;
    email: string;
    /**
     * The mobile number, already normalised to `+91XXXXXXXXXX`.
     *
     * The guide marks this required in practice, and the reason is concrete:
     * name, email and contact are passed straight through to Razorpay Checkout
     * as `prefill`, and anything not sent is something Checkout stops and asks
     * the customer for. A missing number costs them a screen before they can
     * pay, and records whatever they type rather than who we priced for.
     */
    contact?: string;
    name?: string;
    /** ISO 3166-1 alpha-2. Decides the tax treatment; omitted, the currency stands in. */
    country?: string;
    /**
     * Our own id for the customer; the natural key the service upserts on.
     *
     * Must not be the email. The service echoes `ref` back on the return URL,
     * so whatever goes in here lands in `page_location`, in browser history
     * and in logs — see customerRef().
     */
    ref: string;
    /**
     * Analytics-only correlation id, echoed back on the return URL as `cid`.
     *
     * Non-PII, and not the token. It is round-tripped through the URL as well
     * as localStorage because the localStorage copy is defeated by exactly the
     * cases that matter: a provider webview with its own storage jar, private
     * mode, and a return in a different tab.
     */
    checkoutId?: string;
    notes?: Record<string, string>;
  }): Promise<{ checkoutUrl: string; token: string; amountPaise: number; currency: string }> {
    const origin = this.doc.defaultView?.location.origin ?? '';
    const cid = input.checkoutId ? `&cid=${encodeURIComponent(input.checkoutId)}` : '';

    const body = JSON.stringify({
      line_items: [{ price_point: input.pricePoint, quantity: input.quantity ?? 1 }],
      // `ref` carries the whole object: without it the customer is dropped
      // silently — no error — and the payment cannot be attributed later.
      customer: {
        ref: input.ref,
        email: input.email,
        ...(input.contact ? { contact: input.contact } : {}),
        ...(input.name ? { name: input.name } : {}),
        ...(input.country ? { country: input.country } : {}),
      },
      // `cid` is ours; the service appends its own `ref` alongside it.
      success_url: `${origin}/pro?payment=done${cid}`,
      failure_url: `${origin}/pro?payment=failed${cid}`,
      // Flagged, so the card can say the checkout was closed rather than
      // showing the page as if nothing had happened.
      cancel_url: `${origin}/pro?payment=cancelled${cid}`,
      ...(input.notes ? { notes: input.notes } : {}),
    });

    // Minted once and reused across retries. A fresh key on a retry is how one
    // intent becomes two checkouts, so the in-flight answer below must not
    // generate a new one.
    const idempotencyKey = crypto.randomUUID();

    for (let attempt = 0; ; attempt++) {
      const res = await fetch(`${PAYMENTS.baseUrl}/v1/checkouts`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${PAYMENTS.publicKey}`,
          'Content-Type': 'application/json',
          'Idempotency-Key': idempotencyKey,
        },
        body,
      });

      if (res.ok) {
        const created = await res.json();
        this.rememberToken(created.checkout_token);
        // The page cannot name a currency; the catalog prices the SKU in the
        // tenant's. Whatever comes back is what will be charged.
        const currency = String(created.currency ?? 'INR').toUpperCase();
        this.rememberAmount(created.amount_paise, currency);
        return {
          checkoutUrl: created.checkout_url,
          token: created.checkout_token,
          amountPaise: created.amount_paise,
          currency,
        };
      }

      const err = await this.toError(res);
      // The first request is still running. Backing off on the same key gets
      // that request's own answer replayed; giving up here would leave a
      // checkout the customer never sees.
      if (err.code === 'idempotency_in_flight' && attempt < 3) {
        await new Promise((r) => setTimeout(r, err.retryAfterMs ?? 1000));
        continue;
      }
      throw err;
    }
  }

  /**
   * The authoritative outcome. Needs no credential — holding the token is the
   * authorisation — and never calls the provider, so refreshing the page does
   * not amplify into provider traffic.
   */
  async status(token: string): Promise<StatusResult> {
    const res = await fetch(`${PAYMENTS.baseUrl}/c/${encodeURIComponent(token)}/status`);
    // Expired, cancelled, consumed, or never existed — all answer alike on
    // purpose, so the response cannot be used to probe for live tokens.
    if (res.status === 410) return { status: 'failed' };
    if (!res.ok) throw await this.toError(res);
    return res.json();
  }

  /**
   * Polls until the answer is terminal.
   *
   * `processing` and `pending` mean the answer is not known yet, most often
   * because the webhook is still in flight. They must never be shown to a
   * customer as failure: someone told "failed" for a payment that is actually
   * in progress pays a second time.
   */
  async waitForOutcome(
    token: string,
    opts: { onWait?: () => void; timeoutMs?: number } = {},
  ): Promise<StatusResult> {
    // A webhook that never lands would otherwise poll for as long as the tab is
    // open. Giving up returns the last non-terminal answer rather than
    // inventing a terminal one: the caller must keep saying "not known yet",
    // because a customer told "failed" here pays a second time.
    const deadline = Date.now() + (opts.timeoutMs ?? 90_000);
    let last: StatusResult = { status: 'processing' };
    while (Date.now() < deadline) {
      last = await this.status(token);
      if (last.status !== 'processing' && last.status !== 'pending') return last;
      opts.onWait?.();
      await new Promise((r) => setTimeout(r, last.poll_after_ms ?? 1500));
    }
    return last;
  }

  /**
   * Survives the round trip, including one that comes back in another tab.
   *
   * `sessionStorage` is per-tab, and a UPI or netbanking app returning through
   * its own browser view lands the customer in a new one — where the token is
   * gone and the payment cannot be confirmed. The guide names `localStorage`
   * as the fix for exactly that; the alternative it offers, carrying the token
   * in `success_url`, is not taken because the token *is* the authorisation
   * and a URL leaks through history, referrers and logs.
   */
  rememberToken(token: string): void {
    this.write(TOKEN_KEY, token);
  }

  takeToken(): string | null {
    return this.read(TOKEN_KEY);
  }

  clearToken(): void {
    this.remove(TOKEN_KEY);
    this.remove(AMOUNT_KEY);
    this.remove(CURRENCY_KEY);
    this.remove(EC_EMAIL_KEY);
  }

  /**
   * A non-PII customer key for `customer.ref`.
   *
   * The service echoes `ref` back on the return URL, so it must be something
   * that can safely appear in `page_location`, in history and in logs. It used
   * to be the email itself, which put the customer's address into GA4 and
   * Google Ads as `transaction_id` — a policy breach on both.
   *
   * SHA-256 of the normalised address, truncated: stable for one person across
   * devices and retries, so the service still upserts a single customer, and
   * not reversible. `crypto.subtle` needs a secure context, so an insecure one
   * falls back to a durable per-browser id — weaker (a second device looks like
   * a second customer) but never PII, and it keeps dev servers working.
   */
  async customerRef(email: string): Promise<string> {
    const normalised = email.trim().toLowerCase();
    try {
      const bytes = new TextEncoder().encode(normalised);
      const digest = await crypto.subtle.digest('SHA-256', bytes);
      const hex = Array.from(new Uint8Array(digest))
        .map((b) => b.toString(16).padStart(2, '0'))
        .join('');
      return `web_${hex.slice(0, 32)}`;
    } catch {
      const existing = this.read(CUSTOMER_REF_KEY);
      if (existing) return existing;
      const minted = `web_${crypto.randomUUID().replace(/-/g, '')}`;
      this.write(CUSTOMER_REF_KEY, minted);
      return minted;
    }
  }

  /** Held only for the Enhanced Conversions match; see EC_EMAIL_KEY. */
  rememberEcEmail(email: string): void {
    this.write(EC_EMAIL_KEY, email.trim().toLowerCase());
  }

  /** Read-and-clear: the address is needed once, on return, and then gone. */
  takeEcEmail(): string | null {
    const email = this.read(EC_EMAIL_KEY);
    this.remove(EC_EMAIL_KEY);
    return email;
  }

  rememberCheckoutId(id: string): void {
    this.write(CHECKOUT_ID_KEY, id);
  }

  /** Read-and-clear: one purchase report per checkout. */
  takeCheckoutId(): string | null {
    const id = this.read(CHECKOUT_ID_KEY);
    this.remove(CHECKOUT_ID_KEY);
    return id;
  }

  rememberAmount(minor: number, currency = 'INR'): void {
    if (!Number.isFinite(minor)) return;
    this.write(AMOUNT_KEY, String(minor));
    this.write(CURRENCY_KEY, currency);
  }

  /** The quote's currency, as the service returned it. */
  takeCurrency(): string {
    return this.read(CURRENCY_KEY) ?? 'INR';
  }

  /** The quote total, as the service froze it. */
  takeAmount(): number | null {
    const raw = this.read(AMOUNT_KEY);
    const n = raw === null ? NaN : Number(raw);
    return Number.isFinite(n) ? n : null;
  }

  private write(key: string, value: string): void {
    try { this.doc.defaultView?.localStorage.setItem(key, value); } catch { /* private mode */ }
  }

  /**
   * Reads the durable copy, falling back to the per-tab one.
   *
   * The fallback is for checkouts already in flight when this shipped: those
   * tokens were written to `sessionStorage` by the previous build, and a
   * customer mid-payment must not come back to an unconfirmable result.
   */
  private read(key: string): string | null {
    try {
      const w = this.doc.defaultView;
      return w?.localStorage.getItem(key) ?? w?.sessionStorage.getItem(key) ?? null;
    } catch { return null; }
  }

  private remove(key: string): void {
    try {
      const w = this.doc.defaultView;
      w?.localStorage.removeItem(key);
      w?.sessionStorage.removeItem(key);
    } catch { /* ignore */ }
  }

  /** Branch on `code`, never on `message`; log `request_id`. */
  private async toError(res: Response): Promise<PaymentsError> {
    const requestId = res.headers.get('X-Request-Id');
    // Seconds in the header; a date form is possible but the service sends
    // seconds, and a value we cannot parse simply leaves the caller's default.
    const retryAfter = Number(res.headers.get('Retry-After'));
    let code = `http_${res.status}`;
    let message = 'The payment service could not be reached.';
    try {
      const body = await res.json();
      if (body?.error?.code) code = body.error.code;
      if (body?.error?.message) message = body.error.message;
    } catch { /* not JSON */ }
    return {
      code,
      message,
      requestId,
      ...(Number.isFinite(retryAfter) && retryAfter > 0 ? { retryAfterMs: retryAfter * 1000 } : {}),
    };
  }
}

/**
 * Minor units of `currency` in major units. The exponent comes from the
 * currency itself — the guide is explicit that dividing by 100 regardless is
 * wrong (yen have none).
 */
export function minorToMajor(minor: number, currency: string): number {
  const digits = new Intl.NumberFormat('en-US', { style: 'currency', currency })
    .resolvedOptions().maximumFractionDigits ?? 2;
  return minor / 10 ** digits;
}

/**
 * Minor units of `currency` as a display amount: 900 USD is "$9.00", 86730
 * INR is "₹867.30". Rupees use Indian digit grouping. `cents: false` drops the
 * fraction, for list prices.
 */
export function formatMinor(minor: number, currency: string, cents = true): string {
  return new Intl.NumberFormat(currency === 'INR' ? 'en-IN' : 'en-US', {
    style: 'currency', currency,
    ...(cents ? {} : { minimumFractionDigits: 0, maximumFractionDigits: 0 }),
  }).format(minorToMajor(minor, currency));
}

/** What to tell the customer. Anything unlisted gets the generic line. */
export function messageFor(err: PaymentsError): string {
  switch (err.code) {
    case 'unknown_price_point':
    case 'browser_checkout_disabled':
      return 'That plan is not available for self-serve checkout yet. Write to admin@iso8583.studio.';
    // The likeliest failure once a unit SKU exists: browser_max_quantity
    // defaults to 10, and at ₹1 a unit even a small amount exceeds it.
    case 'invalid_quantity':
      return 'That amount is above the current self-serve limit. Write to admin@iso8583.studio.';
    case 'too_many_line_items':
      return 'That order has too many items for self-serve checkout.';
    case 'origin_not_allowed':
    case 'redirect_origin_not_allowed':
      return 'Checkout is not enabled for this address. Write to admin@iso8583.studio.';
    case 'amount_not_permitted':
      return 'This form cannot set its own price. Write to admin@iso8583.studio.';
    // Every one of these is a misconfigured or wrongly-issued credential. The
    // customer can do nothing about any of them, so they get one honest line
    // and a way to reach a human; the distinction is in the console.
    case 'unauthorized':
    case 'insufficient_scope':
    case 'tenant_mismatch':
      return 'Checkout is not set up correctly on our side. Write to admin@iso8583.studio '
        + 'and we will take the payment directly.';
    // Should be unreachable: the key is minted per submit and reused only on
    // an in-flight retry, which sends the identical body.
    case 'idempotency_key_reused':
      return 'That looks like a duplicate attempt. Reload the page and try once more.';
    case 'http_429':
      return 'Too many attempts just now. Wait a moment and try again.';
    case 'provider_unavailable':
      return 'The payment provider is unavailable right now. Please try again shortly.';
    default:
      return 'Payment could not be started. Write to admin@iso8583.studio and we will set you up.';
  }
}
