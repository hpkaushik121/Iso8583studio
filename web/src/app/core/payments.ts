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
    /** Our own id for the customer; the natural key the service upserts on. */
    ref: string;
    notes?: Record<string, string>;
  }): Promise<{ checkoutUrl: string; token: string; amountPaise: number }> {
    const origin = this.doc.defaultView?.location.origin ?? '';

    const res = await fetch(`${PAYMENTS.baseUrl}/v1/checkouts`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${PAYMENTS.publicKey}`,
        'Content-Type': 'application/json',
        // Required on every money-moving POST. A retry must reuse the same key
        // or one intent becomes two checkouts.
        'Idempotency-Key': crypto.randomUUID(),
      },
      body: JSON.stringify({
        line_items: [{ price_point: input.pricePoint, quantity: input.quantity ?? 1 }],
        customer: { ref: input.ref, email: input.email },
        success_url: `${origin}/pro?payment=done`,
        failure_url: `${origin}/pro?payment=failed`,
        cancel_url: `${origin}/pro`,
        ...(input.notes ? { notes: input.notes } : {}),
      }),
    });

    if (!res.ok) throw await this.toError(res);

    const body = await res.json();
    this.rememberToken(body.checkout_token);
    return {
      checkoutUrl: body.checkout_url,
      token: body.checkout_token,
      amountPaise: body.amount_paise,
    };
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
  async waitForOutcome(token: string, onWait?: () => void): Promise<StatusResult> {
    for (;;) {
      const out = await this.status(token);
      if (out.status !== 'processing' && out.status !== 'pending') return out;
      onWait?.();
      await new Promise((r) => setTimeout(r, out.poll_after_ms ?? 1500));
    }
  }

  /** Survives the redirect in the same tab. */
  rememberToken(token: string): void {
    try { this.doc.defaultView?.sessionStorage.setItem(TOKEN_KEY, token); } catch { /* private mode */ }
  }

  takeToken(): string | null {
    try {
      const w = this.doc.defaultView;
      const t = w?.sessionStorage.getItem(TOKEN_KEY) ?? null;
      return t;
    } catch { return null; }
  }

  clearToken(): void {
    try { this.doc.defaultView?.sessionStorage.removeItem(TOKEN_KEY); } catch { /* ignore */ }
  }

  /** Branch on `code`, never on `message`; log `request_id`. */
  private async toError(res: Response): Promise<PaymentsError> {
    const requestId = res.headers.get('X-Request-Id');
    let code = `http_${res.status}`;
    let message = 'The payment service could not be reached.';
    try {
      const body = await res.json();
      if (body?.error?.code) code = body.error.code;
      if (body?.error?.message) message = body.error.message;
    } catch { /* not JSON */ }
    return { code, message, requestId };
  }
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
      return 'Checkout is not enabled for this address. Write to admin@iso8583.studio.';
    case 'amount_not_permitted':
      return 'This form cannot set its own price. Write to admin@iso8583.studio.';
    case 'http_429':
      return 'Too many attempts just now. Wait a moment and try again.';
    case 'provider_unavailable':
      return 'The payment provider is unavailable right now. Please try again shortly.';
    default:
      return 'Payment could not be started. Write to admin@iso8583.studio and we will set you up.';
  }
}
