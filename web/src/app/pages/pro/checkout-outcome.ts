import { Injectable, afterNextRender, inject, signal } from '@angular/core';
import { DOCUMENT, Location } from '@angular/common';
import { Router } from '@angular/router';
import { PaymentsService } from '../../core/payments';
import { AnalyticsService } from '../../core/analytics';
import { PaymentNotice } from '../../core/payment-notice';

/**
 * What happened to the payment, read once when the customer comes back.
 *
 * This lives outside the result card because the page decides what to render
 * from it: coming back from checkout replaces the pitch and the registration
 * form with the result, rather than adding the result above them.
 *
 * Every state below is a state the payments API can actually produce. In
 * particular `confirming` and `unconfirmed` are not decoration: the service
 * documents that a customer shown failure for a payment still in flight makes
 * a second payment, so an answer that is merely *not yet known* has to look
 * different from one that is known to be bad.
 */
export type OutcomeState =
  /** Not a return from checkout — an ordinary visit to the page. */
  | 'idle'
  /** Polling `/c/{token}/status`; the ledger has not settled yet. */
  | 'confirming'
  | 'paid'
  | 'failed'
  /** The customer closed the hosted checkout without paying. */
  | 'cancelled';

@Injectable({ providedIn: 'root' })
export class CheckoutOutcome {
  private readonly payments = inject(PaymentsService);
  private readonly analytics = inject(AnalyticsService);
  private readonly notice = inject(PaymentNotice);
  private readonly doc = inject(DOCUMENT);
  private readonly router = inject(Router);
  private readonly location = inject(Location);

  readonly state = signal<OutcomeState>('idle');
  /**
   * What the card prints as the order reference.
   *
   * The checkout id, which is per-order and is also the transaction_id both
   * GA4 and Ads now key on — so a customer quoting it can be matched to the
   * analytics record. `ref` is the fallback: it is the service's customer key,
   * the same value for every purchase by one person, and it used to be their
   * email address, which the card then printed back at them.
   */
  readonly reference = signal<string | null>(null);
  /** The quote total, when this tab is the one that started the checkout. */
  readonly amountPaise = signal<number | null>(null);
  /** The quote's currency; amountPaise is in its minor units. */
  readonly currency = signal('INR');
  /**
   * Whether `/c/{token}/status` was actually read, rather than the outcome
   * being taken from the redirect. Both are shown as paid; only this one may
   * claim the ledger says so.
   */
  readonly ledgerConfirmed = signal(false);

  constructor() {
    // Deferred to after the first render rather than run in the constructor,
    // because this route is prerendered and hydrated: the server had no URL to
    // read, so a state set synchronously here would make the client's first
    // render disagree with the markup it is hydrating.
    afterNextRender(() => void this.resolve());
  }

  /** Whether the page is showing a payment result rather than the pitch. */
  active(): boolean {
    return this.state() !== 'idle';
  }

  /**
   * Puts the page back to the form after a failed payment.
   *
   * Routed rather than scrolled by hand. The app enables both anchorScrolling
   * and scrollPositionRestoration, so the router owns the scroll position: a
   * manual scrollIntoView here got part way to the form and was then slammed
   * back to the top by restoration reacting to the history change. Navigating
   * to the fragment makes the two the same action instead of two competing
   * ones.
   *
   * Clearing the query matters on its own — it stops a reload replaying a
   * result the customer has already read and acted on.
   */
  dismiss(): void {
    this.state.set('idle');
    void this.router.navigate([], { queryParams: {}, fragment: 'register', replaceUrl: true });
  }

  /**
   * Polls for the outcome; never verifies.
   *
   * The hosted checkout has already called verify — calling it again from here
   * with no Razorpay parameters answers 400. `/c/{token}/status` reports what
   * the ledger already says and never calls the provider, so a customer
   * refreshing this page cannot amplify into provider traffic.
   */
  private async resolve(): Promise<void> {
    const params = new URLSearchParams(this.doc.defaultView?.location.search ?? '');
    const flag = params.get('payment');
    const ref = params.get('ref');
    // Our own id, round-tripped on the return URL. Preferred over the
    // localStorage copy, which a provider webview or a new tab defeats.
    const cid = params.get('cid');

    // A closed checkout. Only believed when this browser started one — a
    // typed ?payment=cancelled has no token behind it and changes nothing.
    if (flag === 'cancelled') {
      if (!this.payments.takeToken()) return;
      // Read before clearToken() wipes all three. This branch returns early,
      // so it is the only chance to tell the notice what was abandoned.
      const amount = this.payments.takeAmount();
      const currency = this.payments.takeCurrency();
      const email = this.payments.takeEcEmail();
      this.payments.clearToken();
      const closedId = cid ?? this.payments.takeCheckoutId();
      this.reference.set(closedId ?? ref);
      this.analytics.reportPaymentResult('cancelled', cid ?? '');
      this.notice.cancelled({ email, amountMinor: amount, currency, checkoutId: closedId });
      this.state.set('cancelled');
      this.clearQuery();
      return;
    }

    // Both, and a flag we recognise. The service appends `ref` itself, so a URL
    // carrying one is a genuine return from checkout; `?payment=done` typed or
    // pasted on its own is not, and must not put the page into a result screen
    // that claims something about a payment nobody made.
    if ((flag !== 'done' && flag !== 'failed') || !ref) return;

    // Read before clearToken() wipes it — order matters here.
    this.amountPaise.set(this.payments.takeAmount());
    this.currency.set(this.payments.takeCurrency());
    // takeCheckoutId() is called either way, so the stored key never goes stale.
    const checkoutId = cid ?? this.payments.takeCheckoutId();
    this.reference.set(checkoutId ?? ref);
    this.analytics.reportPaymentResult(flag, checkoutId ?? '');

    // Read once, and before the failed branch below calls clearToken(), which
    // wipes it. takeEcEmail clears as it reads, and the Ads match, the paid
    // notice and the failed notice all want the same address.
    const ecEmail = this.payments.takeEcEmail();

    if (flag === 'failed') {
      this.payments.clearToken();
      this.analytics.reportPaymentFailed(checkoutId ?? '');
      this.notice.failed({
        email: ecEmail, reference: ref, amountMinor: this.amountPaise(),
        currency: this.currency(), checkoutId,
      });
      this.state.set('failed');
      this.clearQuery();
      return;
    }

    // Only on the way to a purchase: this is the Ads conversion's match data.
    if (ecEmail) this.analytics.setAdsUserData(ecEmail);

    // `done` is already an outcome, not a hint: the hosted checkout calls
    // verify — signature, order match, then a re-fetch from Razorpay — and
    // only redirects here once that answered paid. So the screen states it,
    // and does not depend on this tab having kept a token.
    const token = this.payments.takeToken();
    if (!token) {
      // A different tab (UPI return) or cleared storage: the redirect already
      // said paid. Count it — value_known:'no' keeps the blind spot visible.
      this.notifyPaid(ref, checkoutId, ecEmail, false);
      this.state.set('paid');
      this.clearQuery();
      return;
    }

    // With a token there is a better source than the redirect, so use it: the
    // ledger can also correct a `done` that has since settled the other way.
    this.state.set('confirming');
    try {
      const out = await this.payments.waitForOutcome(token);
      this.payments.clearToken();
      if (out.status === 'failed') {
        this.state.set('failed');
        return;
      }
      // paid, or still processing when the poll gave up. Either way the
      // redirect already said paid, and a webhook that has not landed yet is
      // not a reason to tell the customer otherwise.
      this.ledgerConfirmed.set(out.status === 'paid');
      this.notifyPaid(ref, checkoutId, ecEmail, out.status === 'paid');
      this.state.set('paid');
      this.clearQuery();
    } catch {
      // The poll could not run. The redirect stands on its own.
      this.notifyPaid(ref, checkoutId, ecEmail, false);
      this.state.set('paid');
      this.clearQuery();
    }
  }

  /**
   * Reports the purchase and, only if that was the call that reported it,
   * emails the notice. Both hang off the one ledger in reportPurchase, so a
   * reload cannot produce a second email for the same payment.
   */
  private notifyPaid(ref: string, checkoutId: string | null,
                     email: string | null, confirmed: boolean): void {
    const reported = this.analytics.reportPurchase(
      ref, this.amountPaise(), checkoutId, this.currency());
    if (!reported) return;
    this.notice.completed({
      email, reference: ref, amountMinor: this.amountPaise(),
      currency: this.currency(), checkoutId, confirmed,
    });
  }

  /**
   * Drops the return parameters once they have been read.
   *
   * `Location.replaceState`, not `router.navigate`: a navigation emits
   * NavigationEnd and so a second page_view for every completed payment.
   * Only called from terminal branches — doing it before the `confirming`
   * poll would leave a reload mid-confirm with nothing to resolve.
   *
   * The service appends its own `ref` to this URL, so clearing it is also
   * what stops that value sitting in history and in the next referrer.
   */
  private clearQuery(): void {
    this.location.replaceState('/pro');
  }
}
