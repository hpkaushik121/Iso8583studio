import { Injectable, afterNextRender, inject, signal } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { Router } from '@angular/router';
import { PaymentsService } from '../../core/payments';
import { AnalyticsService } from '../../core/analytics';

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
  | 'failed';

@Injectable({ providedIn: 'root' })
export class CheckoutOutcome {
  private readonly payments = inject(PaymentsService);
  private readonly analytics = inject(AnalyticsService);
  private readonly doc = inject(DOCUMENT);
  private readonly router = inject(Router);

  readonly state = signal<OutcomeState>('idle');
  /** The service's order id, taken off the return URL. */
  readonly reference = signal<string | null>(null);
  /** The quote total, when this tab is the one that started the checkout. */
  readonly amountPaise = signal<number | null>(null);
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

    // Both, and a flag we recognise. The service appends `ref` itself, so a URL
    // carrying one is a genuine return from checkout; `?payment=done` typed or
    // pasted on its own is not, and must not put the page into a result screen
    // that claims something about a payment nobody made.
    if ((flag !== 'done' && flag !== 'failed') || !ref) return;

    this.reference.set(ref);
    // Read before clearToken() wipes it — order matters here.
    this.amountPaise.set(this.payments.takeAmount());
    const checkoutId = this.payments.takeCheckoutId();
    this.analytics.reportPaymentResult(flag, ref);

    if (flag === 'failed') {
      this.payments.clearToken();
      this.analytics.reportPaymentFailed(ref);
      this.state.set('failed');
      return;
    }

    // `done` is already an outcome, not a hint: the hosted checkout calls
    // verify — signature, order match, then a re-fetch from Razorpay — and
    // only redirects here once that answered paid. So the screen states it,
    // and does not depend on this tab having kept a token.
    const token = this.payments.takeToken();
    if (!token) {
      // A different tab (UPI return) or cleared storage: the redirect already
      // said paid. Count it — value_known:'no' keeps the blind spot visible.
      this.analytics.reportPurchase(ref, this.amountPaise(), checkoutId);
      this.state.set('paid');
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
      this.analytics.reportPurchase(ref, this.amountPaise(), checkoutId);
      this.state.set('paid');
    } catch {
      // The poll could not run. The redirect stands on its own.
      this.analytics.reportPurchase(ref, this.amountPaise(), checkoutId);
      this.state.set('paid');
    }
  }
}
