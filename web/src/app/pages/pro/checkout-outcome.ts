import { Injectable, afterNextRender, inject, signal } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { Router } from '@angular/router';
import { PaymentsService } from '../../core/payments';

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
  /** No `?payment=` on the URL — an ordinary visit to the page. */
  | 'idle'
  /** Polling `/c/{token}/status`; the ledger has not settled yet. */
  | 'confirming'
  | 'paid'
  | 'failed'
  /** Came back on the success URL, but this tab has no token to confirm with. */
  | 'unconfirmed'
  /** Polled to the deadline, or the poll threw. Still not a failure. */
  | 'unresolved';

@Injectable({ providedIn: 'root' })
export class CheckoutOutcome {
  private readonly payments = inject(PaymentsService);
  private readonly doc = inject(DOCUMENT);
  private readonly router = inject(Router);

  readonly state = signal<OutcomeState>('idle');
  /** The service's order id, taken off the return URL. */
  readonly reference = signal<string | null>(null);
  /** The quote total, when this tab is the one that started the checkout. */
  readonly amountPaise = signal<number | null>(null);

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
    if (!flag) return;

    // Read before anything can clear it: on the branch with no token this is
    // the only concrete thing the customer can quote at us, because a browser
    // credential may never hold `order:read`.
    this.reference.set(params.get('ref'));
    this.amountPaise.set(this.payments.takeAmount());

    const token = this.payments.takeToken();
    if (!token) {
      // A different tab, cleared storage, or a forwarded link. The redirect
      // itself is evidence — the hosted page only sends the customer to
      // success_url after its own verify succeeded — but it is not the ledger,
      // so this says "being confirmed" rather than "paid".
      this.state.set(flag === 'done' ? 'unconfirmed' : 'failed');
      return;
    }

    this.state.set('confirming');
    try {
      const out = await this.payments.waitForOutcome(token);
      if (out.status === 'paid' || out.status === 'failed') {
        this.payments.clearToken();
        this.state.set(out.status);
        return;
      }
      // Still processing at the deadline. The token stays: it is what a reload
      // would need to pick the poll back up.
      this.state.set('unresolved');
    } catch {
      this.state.set('unresolved');
    }
  }
}
