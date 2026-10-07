import { DOCUMENT, Injectable, inject } from '@angular/core';
import { LEADS } from '../content/leads-config';
import { Observability } from './observability';

/** Minor units are what the service froze; the notice should read in rupees. */
function major(minor: number | null, currency: string): string {
  if (minor === null) return 'unknown';
  return `${currency} ${(minor / 100).toFixed(2)}`;
}

/**
 * Emails the two moments of a Pro checkout, each to its own Web3Forms form.
 *
 * Why these are not the native form POST the enquiry uses: both happen while
 * the visitor is doing something else. The first fires in the moment before
 * the browser leaves for the payment provider, and the second on the result
 * page they are reading. Navigating them to Web3Forms would break the
 * checkout outright.
 *
 * So each is sent in the background, and the two moments need different
 * mechanisms:
 *
 *   started   sendBeacon, because a fetch issued immediately before
 *             location.assign() is cancelled by the navigation. The browser
 *             owns the request once it is queued and sends it regardless.
 *             Nothing can be read back, which is the trade for surviving.
 *
 *   completed fetch, because the page is staying put. Multipart keeps it a
 *             CORS-simple request, the same reason the enquiry form avoids a
 *             JSON body: Web3Forms answers a preflight with 403.
 *
 * Neither is allowed to affect the checkout. A notice that threw, or that
 * blocked the redirect waiting for a response, would cost a sale to save an
 * email — so both are fire-and-forget and every failure is swallowed into
 * New Relic rather than raised.
 *
 * These carry the customer's email, which is the point: they are an order
 * notification going to the inbox that handles the order. That is a different
 * thing from the analytics layer, which is deliberately kept clear of it.
 */
@Injectable({ providedIn: 'root' })
export class PaymentNotice {
  private readonly doc = inject(DOCUMENT);
  private readonly obs = inject(Observability);

  /**
   * Someone pressed pay. Sent before the redirect, so it arrives whether or
   * not they go on to complete — an abandoned checkout is worth knowing about
   * too, and is the only trace of one that exists outside analytics.
   */
  started(input: {
    email: string; amountMinor: number | null; currency: string; checkoutId: string;
  }): void {
    if (!LEADS.startedKey) return;
    const body = this.compose(LEADS.startedKey, {
      subject: `Pro checkout started — ${input.email}`,
      email: input.email,
      amount: major(input.amountMinor, input.currency),
      checkout_id: input.checkoutId,
      note: 'Checkout was opened. This is not a payment — the result notice confirms that.',
    });

    try {
      const sent = this.doc.defaultView?.navigator.sendBeacon(LEADS.endpoint, body);
      this.obs.action('payment_notice_started', { queued: sent === true });
    } catch {
      this.obs.error('payment started notice could not be queued', {});
    }
  }

  /**
   * The payment came back good. `confirmed` separates a result the ledger
   * agreed with from one the redirect asserted on its own, because the two
   * deserve different attention in the inbox.
   */
  completed(input: {
    email: string | null; reference: string; amountMinor: number | null;
    currency: string; checkoutId: string | null; confirmed: boolean;
  }): void {
    if (!LEADS.paidKey) return;
    const body = this.compose(LEADS.paidKey, {
      subject: `Pro payment received — ${major(input.amountMinor, input.currency)}`,
      ...(input.email ? { email: input.email } : {}),
      amount: major(input.amountMinor, input.currency),
      reference: input.reference,
      ...(input.checkoutId ? { checkout_id: input.checkoutId } : {}),
      // Worth saying plainly: an unconfirmed one may still settle the other way.
      status: input.confirmed ? 'confirmed by the ledger' : 'reported by the redirect, not yet confirmed',
    });

    void fetch(LEADS.endpoint, { method: 'POST', headers: { Accept: 'application/json' }, body })
      .then(async (res) => {
        const json = await res.json().catch(() => ({}) as { success?: boolean });
        this.obs.action('payment_notice_paid', { delivered: json.success === true });
      })
      .catch(() => this.obs.error('payment completed notice failed to send', {}));
  }

  private compose(key: string, fields: Record<string, string>): FormData {
    const form = new FormData();
    form.append('access_key', key);
    form.append('from_name', 'ISO8583Studio payments');
    for (const [name, value] of Object.entries(fields)) form.append(name, value);
    return form;
  }
}
