import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { CheckoutOutcome } from './checkout-outcome';
import { formatPaise } from '../../core/payments';

/**
 * What the customer sees coming back from the hosted checkout.
 *
 * Built from the "Transaction result screens" design, with three deliberate
 * departures. The first two follow from that design drawing the *simulator's*
 * result, where this is a real charge:
 *
 *  - The design marks every screen `SANDBOX · NOT A REAL PAYMENT`. Money
 *    actually moves here, so that mark is gone. What the customer needs in its
 *    place is the order reference, and that sits in the details below — it is
 *    the only identifier they are given, because a browser credential may
 *    never hold `order:read`.
 *
 *  - The design's detail rows are DE39, RRN, terminal and card. None of that
 *    exists on this surface: `/c/{token}/status` returns a status and nothing
 *    else, and no response on the browser surface carries card data by design.
 *    The rows show what is actually known rather than plausible placeholders.
 *
 * The third is a drawing convention rather than a change of meaning: the design
 * frames each state in a browser chrome to present it as a screen. Reproducing
 * that frame would put a window inside the page, under a header carrying the
 * same logo, so the content is rendered as the page itself.
 *
 * The discreet code line under the details is kept, because the reason the
 * design gives for it survives the change of subject: it is what lets someone
 * reading the screen tell a confirmed capture from a redirect we have not
 * confirmed yet.
 */
@Component({
  selector: 'app-payment-result',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'pr-wrap' },
  template: `
    <div class="pr-card" role="status" aria-live="polite">
      <div class="pr-body">
        <div class="pr-disc" [class]="'pr-disc-' + tone()">
          @switch (state()) {
            @case ('paid') {
              <span class="pr-ring" aria-hidden="true"></span>
              <svg class="pr-icon" viewBox="0 0 24 24" fill="none" stroke="var(--green-hi)"
                   stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                <path class="pr-draw" d="M4.5 12.6 L9.4 17.5 L19.5 7.2" />
              </svg>
            }
            @case ('failed') {
              <svg class="pr-icon" viewBox="0 0 24 24" fill="none" stroke="var(--red-hi)"
                   stroke-width="2.4" stroke-linecap="round">
                <path class="pr-draw" d="M6.5 6.5 L17.5 17.5" />
                <path class="pr-draw pr-draw-2" d="M17.5 6.5 L6.5 17.5" />
              </svg>
            }
            @default {
              @if (state() === 'confirming') {
                <svg class="pr-sweep" viewBox="0 0 72 72" fill="none" aria-hidden="true">
                  <circle cx="36" cy="36" r="35" stroke="var(--muted)" stroke-width="1.5"
                          stroke-linecap="round" />
                </svg>
              }
              <svg class="pr-icon" viewBox="0 0 24 24" fill="none" [attr.stroke]="waitStroke()"
                   stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="8.5" />
                <path d="M12 7.6 V12 L15.4 14" />
              </svg>
            }
          }
        </div>

        <div class="pr-head">
          <h2>{{ copy().title }}</h2>
          <p>{{ copy().body }}</p>
        </div>

        @if (amount(); as a) {
          <div class="pr-amount" [class.pr-amount-quiet]="state() !== 'paid'">
            <div class="pr-amount-value">{{ a }}</div>
            <div class="pr-amount-note">{{ copy().amountNote }}</div>
          </div>
        }

        @if (copy().advisory; as advisory) {
          <div class="pr-advisory">
            <svg viewBox="0 0 24 24" fill="none" stroke="var(--amber-hi)" stroke-width="2.2"
                 stroke-linecap="round" aria-hidden="true">
              <path d="M12 8 v5 M12 16.4 v.2" /><circle cx="12" cy="12" r="9" />
            </svg>
            <p>{{ advisory }}</p>
          </div>
        }

        @if (outcome.reference(); as ref) {
          <dl class="pr-grid">
            <dt>Reference</dt><dd class="pr-mono">{{ ref }}</dd>
            @if (state() === 'paid') {
              <dt>Confirmed</dt><dd class="pr-mono">{{ confirmedAt }}</dd>
            }
          </dl>
        }

        <div class="pr-code">
          <span class="pr-pill" [class]="'pr-pill-' + tone()">{{ copy().code }}</span>
          <span class="pr-code-text">{{ copy().codeNote }}</span>
        </div>

        <div class="pr-actions">
          @if (state() === 'failed') {
            <button type="button" class="pr-btn pr-btn-primary" (click)="tryAgain()">Try again</button>
          } @else if (state() === 'paid') {
            <a class="pr-btn pr-btn-primary" href="/">Back to the studio</a>
          }
          <a class="pr-btn pr-btn-quiet" [href]="supportHref()">Contact support</a>
        </div>
      </div>
    </div>
  `,
})
export class PaymentResult {
  protected readonly outcome = inject(CheckoutOutcome);

  protected readonly state = this.outcome.state;

  /** Rendered once: it is when this page confirmed, not a ledger timestamp. */
  protected readonly confirmedAt = new Date().toLocaleString('en-IN', {
    day: '2-digit', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit', second: '2-digit',
  });

  protected readonly amount = computed(() => {
    const paise = this.outcome.amountPaise();
    return paise === null ? null : formatPaise(paise, true);
  });

  /** Which accent the screen carries. Only a known failure is allowed red. */
  protected readonly tone = computed(() => {
    switch (this.state()) {
      case 'paid': return 'ok';
      case 'failed': return 'bad';
      case 'confirming': return 'wait';
      default: return 'hold';
    }
  });

  protected readonly waitStroke = computed(() =>
    this.state() === 'confirming' ? 'var(--muted)' : 'var(--amber-hi)');

  protected readonly copy = computed(() => {
    switch (this.state()) {
      case 'paid':
        return {
          title: 'Payment received',
          body: 'Your workspace is provisioned by hand — we will email your credentials shortly.',
          amountNote: 'ISO8583Studio Pro · early access',
          advisory: null,
          code: 'STATUS paid',
          codeNote: 'CAPTURED · CONFIRMED AGAINST THE PAYMENTS LEDGER',
        };
      case 'failed':
        return {
          title: 'Payment not completed',
          body: 'Nothing was charged. You can try again, or write to us and we will '
            + 'set you up directly.',
          amountNote: 'not charged · ISO8583Studio Pro',
          advisory: null,
          code: 'STATUS failed',
          codeNote: 'NOT CAPTURED · NOTHING CHARGED',
        };
      case 'confirming':
        return {
          title: 'Confirming your payment',
          body: 'Your bank has sent you back. We are waiting for the payment service to '
            + 'confirm the capture, which usually takes a few seconds.',
          amountNote: 'ISO8583Studio Pro · early access',
          advisory: 'Do not pay again. This is a confirmation still in flight, not a failure.',
          code: 'STATUS processing',
          codeNote: 'AWAITING WEBHOOK · POLLING THE LEDGER',
        };
      case 'unconfirmed':
        return {
          title: 'Your payment is being confirmed',
          body: 'Checkout finished in another tab, so this one cannot confirm it. Your payment '
            + 'is not affected — we will email you once it settles.',
          amountNote: 'ISO8583Studio Pro · early access',
          advisory: 'Do not pay again. Quote the reference below and we can find it.',
          code: 'NO SESSION',
          codeNote: 'RETURNED ON THE SUCCESS URL · NOT CONFIRMABLE FROM THIS TAB',
        };
      default:
        return {
          title: 'Still confirming your payment',
          body: 'The confirmation is taking longer than usual, which is slowness rather than a '
            + 'problem. We will email you once it settles.',
          amountNote: 'ISO8583Studio Pro · early access',
          advisory: 'Do not pay again. Quote the reference below and we can find it.',
          code: 'STATUS processing',
          // Covers both ways this state is reached — the poll ran to its
          // deadline, or it could not complete at all.
          codeNote: 'NO ANSWER YET · NOT A FAILURE',
        };
    }
  });

  /** Carries the reference, so support does not have to ask for it. */
  protected supportHref(): string {
    const ref = this.outcome.reference();
    const subject = ref ? `Pro payment ${ref}` : 'Pro payment';
    return `mailto:admin@iso8583.studio?subject=${encodeURIComponent(subject)}`;
  }

  /** Back to the form. The service owns this, so the scroll survives the
   *  removal of this card. */
  protected tryAgain(): void {
    this.outcome.dismiss();
  }
}
