import {
  ChangeDetectionStrategy, Component, ElementRef, OnDestroy, PLATFORM_ID, afterNextRender,
  computed, effect, inject, input, signal, viewChild,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { PAYMENTS } from '../../content/payments-config';
import { AnalyticsService } from '../../core/analytics';
import { PaymentsService, formatMinor, messageFor } from '../../core/payments';
import { UiIcon } from '../../ui';
import { CheckoutOutcome } from './checkout-outcome';
import { ProSpotlight } from './pro-motion';

/** Two labels either side of one @, no whitespace, and a dotted TLD. */
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

/** What the card shows in place of its email row. */
type Phase = 'form' | 'processing' | 'confirming' | 'paid' | 'failed';

/**
 * One payment card of the fan behind the Pro card, as the design's CardStack
 * lays them out: pairs either side of the centre, each pair turned further and
 * pushed further out, and the odd card straight up behind the rest.
 * `dx`/`dy` in px, `rot` in degrees, `o` the opacity once open, `i` the order
 * the fan opens in.
 */
interface FanCard { i: number; step: number; dx: number; dy: number; rot: number; o: number; amount: string; label: string; }

const FACES: [string, string][] = [
  ['$17,463.00', 'Savings'], ['€8,650.00', 'Settlement'], ['$48,650.00', 'Operations'],
  ['£20,650.00', 'Reserve'], ['$385,430.00', 'Treasury'],
];

/** CardStack defaults from the design: 5 cards, 8° first turn, +6° per step, 20° cap, 132px step. */
function fan(count = 5, angle = 8, spreadStep = 6, maxAngle = 20, turn = 90, offset = 132): FanCard[] {
  const cards: FanCard[] = [];
  const pairs = Math.floor(count / 2);
  const face = (i: number) => FACES[i % FACES.length];
  for (let i = 0; i < pairs * 2; i++) {
    const side = i % 2 === 0 ? -1 : 1;
    const step = Math.floor(i / 2) + 1;
    cards.push({
      i, step, dx: side * offset * step, dy: (step - 1) * 16,
      rot: turn + side * Math.min(angle + (step - 1) * spreadStep, maxAngle),
      o: Math.max(0.34, 0.8 - (step - 1) * 0.2), amount: face(i)[0], label: face(i)[1],
    });
  }
  if (count % 2) {
    const i = count - 1;
    cards.push({ i, step: pairs + 1, dx: 0, dy: -pairs * 10, rot: turn, o: Math.max(0.34, 0.8 - pairs * 0.2), amount: face(i)[0], label: face(i)[1] });
  }
  // Outermost first, so the nearest cards paint on top.
  return cards.sort((a, b) => b.step - a.step);
}

/**
 * The Pro card from the design, on its fanned stack of payment cards — used
 * by the home page's pricing section and the Pro page's registration section.
 *
 * It runs the real payment, the way the design draws it: a work email,
 * "Continue to payment", then the card itself shows the payment being started
 * and hands the customer to the hosted checkout. On the way back (/pro with
 * ?payment=…) the same card shows the outcome — confirming, reserved or
 * declined — from CheckoutOutcome, which reads the payment ledger.
 *
 * The page never names a price or a currency: it names the price point and the
 * payment service prices it. The card's $9.00 is the design's copy; what is
 * shown while authorising, and on the receipt, is the service's own figure. A
 * checkout that comes back in any currency but US dollars is not started.
 *
 * "Continue to payment" is an anchor to /pro#register inside a .pro-nudge
 * wrapper — analytics reports it as pro_click, and without script it still
 * lands on this card on /pro.
 *
 * The fan opens when the stack scrolls into view. The prerendered page shows
 * it open; in the browser a stack still below the viewport is closed first
 * and opens on arrival, and under reduced motion it simply stays open.
 *
 * Styles: styles/bundles/_pro-reserve.css, imported by home.css and pro.css.
 */
@Component({
  selector: 'app-pro-reserve',
  imports: [UiIcon, ProSpotlight],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'rsv-host' },
  template: `
    <div #stack class="rsv" [class.is-closed]="closed()">
      @for (card of cards; track card.i) {
        <div class="rsv-fan" aria-hidden="true" [style.--dx]="card.dx" [style.--dy]="card.dy"
             [style.--rot]="card.rot" [style.--o]="card.o" [style.--fd]="card.i * 70">
          <div class="rsv-fan-face">
            <div class="rsv-fan-top"><span class="rsv-fan-disc"></span><ui-icon name="wifi-high" [size]="22" /></div>
            <div>
              <div class="rsv-fan-amount">{{ card.amount }}</div>
              <div class="rsv-fan-row"><span>{{ card.label }}</span><span class="rsv-fan-pill">Active</span></div>
            </div>
          </div>
        </div>
      }
      <div class="rsv-front" [class.ds-item]="stagger()" [style.--d]="stagger() ? 500 : null">
        <div class="rsv-spot" proSpotlight>
          <div class="rsv-card">
            <span class="rsv-kicker">✦ ISO8583Studio Pro</span>
            <p class="rsv-lede">Reserve a seat before launch. Pre-registration is paid and locks the founder rate for your first year.</p>
            <div class="rsv-price">
              <strong>$13.99</strong>
              <span>per month, billed at launch</span>
            </div>
            <div class="rsv-due"><span>Due today to reserve</span><span class="rsv-due-v">$9.00</span></div>

            @switch (phase()) {
              @case ('processing') {
                <div class="rsv-box rsv-box--wait rsv-row" role="status" aria-live="polite">
                  <span class="rsv-box-title"><span class="rsv-spin" aria-hidden="true"></span>Processing payment…</span>
                  <span class="rsv-bar"><span class="rsv-bar-run"></span></span>
                  <span class="rsv-box-foot"><span>Authorizing {{ charge() }}</span><span>Don't close this page</span></span>
                </div>
              }
              @case ('confirming') {
                <div class="rsv-box rsv-box--wait rsv-row" role="status" aria-live="polite">
                  <span class="rsv-box-title"><span class="rsv-spin" aria-hidden="true"></span>Confirming payment…</span>
                  <span class="rsv-bar"><span class="rsv-bar-run"></span></span>
                  <span class="rsv-box-foot"><span>Checking with the payment ledger</span><span>Don't close this page</span></span>
                </div>
              }
              @case ('paid') {
                <div class="rsv-box rsv-box--ok rsv-pop" role="status">
                  <span class="rsv-box-title"><ui-icon name="check-circle" [size]="20" />Seat reserved</span>
                  <span class="rsv-box-text">{{ paidAmount() }} paid. Your receipt and invite are on the way to your inbox.</span>
                  <span class="rsv-box-foot rsv-box-foot--rule"><span>Order {{ outcome.reference() }}</span><span>{{ outcome.ledgerConfirmed() ? 'Confirmed' : 'Confirmation pending' }}</span></span>
                </div>
              }
              @case ('failed') {
                <div class="rsv-box rsv-box--bad rsv-row" role="alert">
                  <span class="rsv-box-title"><ui-icon name="x-circle" [size]="20" />Payment declined</span>
                  <span class="rsv-box-text">The {{ paidAmount() }} charge did not go through. No money was taken.</span>
                  <span class="rsv-box-foot rsv-box-foot--rule rsv-box-foot--act">
                    <span class="rsv-box-code">Order {{ outcome.reference() }}</span>
                    <button class="btn btn--outline btn--sm" type="button" (click)="tryAgain()">
                      <ui-icon name="arrow-clockwise" [size]="14" />Try again
                    </button>
                  </span>
                </div>
              }
              @default {
                <div class="rsv-form pro-nudge">
                  <label class="rsv-input" [class.is-invalid]="emailInvalid()">
                    <ui-icon name="envelope-simple" [size]="16" />
                    <input #email type="email" aria-label="Work email" placeholder="you&#64;company.com"
                           autocomplete="email" [attr.aria-invalid]="emailInvalid() || null"
                           (input)="started()" (keydown.enter)="$event.preventDefault(); pay(email.value)">
                  </label>
                  <a class="btn btn--primary" href="/pro#register"
                     (click)="$event.preventDefault(); pay(email.value)">
                    Continue to payment<ui-icon name="arrow-right" [size]="16" />
                  </a>
                </div>
                @if (emailInvalid()) { <p class="rsv-err" role="alert">Enter your work email — the receipt and invite are sent there.</p> }
                @if (error()) { <p class="rsv-err" role="alert">{{ error() }}</p> }
              }
            }

            <p class="rsv-fine">{{ fine() }}</p>
            <div class="rsv-includes">
              <p>Pro includes:</p>
              <div class="rsv-includes-list">
                @for (f of features; track f) { <span><ui-icon name="check" [size]="15" />{{ f }}</span> }
              </div>
            </div>
          </div>
          <span class="rsv-spot-edge" aria-hidden="true"></span>
          <span class="rsv-spot-wash" aria-hidden="true"></span>
        </div>
      </div>
    </div>
  `,
})
export class ProReserve implements OnDestroy {
  /** Blur the card up from an ancestor uiReveal, as the home section does. */
  readonly stagger = input(false);
  /** The line under the button; the two pages word it differently. */
  readonly fine = input('Refundable until launch');

  protected readonly cards = fan();
  protected readonly features = [
    'Raised CPS ceiling', 'Full algorithm set', 'Deep simulator tweaks',
    'Hosted cloud endpoints', 'Priority support', 'Team configuration sync',
  ];

  protected readonly outcome = inject(CheckoutOutcome);
  private readonly payments = inject(PaymentsService);
  private readonly analytics = inject(AnalyticsService);

  /** This card's own step, before the page leaves for checkout. */
  private readonly local = signal<'form' | 'processing'>('form');
  protected readonly emailInvalid = signal(false);
  protected readonly error = signal<string | null>(null);
  /** The amount the service quoted, shown while authorising. */
  protected readonly charge = signal('$9.00');

  /** A return from checkout outranks the card's own step. */
  protected readonly phase = computed<Phase>(() => {
    const state = this.outcome.state();
    return state === 'idle' ? this.local() : state;
  });

  /** What was charged, from the quote this tab started; the card's own figure otherwise. */
  protected readonly paidAmount = computed(() => {
    const minor = this.outcome.amountPaise();
    return minor === null ? '$9.00' : formatMinor(minor, this.outcome.currency());
  });

  /** True while the fan is gathered into one stack, waiting to open. */
  protected readonly closed = signal(false);

  private readonly stack = viewChild.required<ElementRef<HTMLElement>>('stack');
  private io: IntersectionObserver | null = null;

  constructor() {
    if (!isPlatformBrowser(inject(PLATFORM_ID))) return;

    // Coming back from checkout: bring the card that holds the answer into view.
    let shown = false;
    effect(() => {
      if (shown || !this.outcome.active()) return;
      shown = true;
      setTimeout(() => this.stack().nativeElement.scrollIntoView({ block: 'center' }), 120);
    });

    afterNextRender(() => {
      const el = this.stack().nativeElement;
      const view = el.ownerDocument.defaultView;
      if (!view || !('IntersectionObserver' in view)) return;
      if (view.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
      // Only a stack the reader has not reached yet is closed, so nothing on
      // screen folds up in front of them.
      if (el.getBoundingClientRect().top < view.innerHeight) return;
      this.closed.set(true);
      this.io = new IntersectionObserver(([entry]) => {
        if (!entry.isIntersecting) return;
        this.io?.disconnect();
        this.closed.set(false);
      }, { threshold: 0.2 });
      this.io.observe(el);
    });
  }

  /** First keystroke = the form was started. trackOnce dedupes. */
  protected started(): void {
    this.emailInvalid.set(false);
    this.analytics.reportFormStart();
  }

  protected tryAgain(): void {
    this.local.set('form');
    this.error.set(null);
    this.outcome.dismiss();
  }

  /**
   * Starts the real checkout for the reservation.
   *
   * Browser-only shape: the page names the price point, the service prices
   * it, and the card shows the service's figure while it hands over. Nothing
   * here sets an amount or a currency.
   */
  protected async pay(raw: string): Promise<void> {
    if (this.local() === 'processing') return;
    const email = raw.trim();
    this.error.set(null);
    if (!EMAIL.test(email)) {
      this.emailInvalid.set(true);
      this.analytics.reportFormSubmit(false);
      this.analytics.reportFormError(['email']);
      return;
    }
    this.emailInvalid.set(false);
    this.analytics.reportFormSubmit(true, 9, 'USD');

    if (!this.payments.configured) {
      console.warn(`Pro checkout is off — ${this.payments.unconfiguredReason}`);
      this.error.set('Payment is not connected yet. Write to admin@iso8583.studio and we will reserve your seat directly.');
      return;
    }

    this.local.set('processing');
    try {
      const { checkoutUrl, amountPaise, currency } = await this.payments.createCheckout({
        pricePoint: PAYMENTS.pricePoints[0],
        quantity: 1,
        email,
        // No accounts here, so the customer's own email is the stable key.
        ref: email,
        notes: { source: 'pro-reserve-card' },
      });

      // The card promises dollars. A price point still priced in another
      // currency would charge something the customer was never shown, so it
      // is not started at all.
      if (currency !== 'USD') {
        this.payments.clearToken();
        console.error(`Pro checkout: price point ${PAYMENTS.pricePoints[0]} is priced in ${currency}, `
          + 'not USD. Reprice it in the payment service; the checkout was not started.');
        this.analytics.reportCheckoutError('currency_not_usd');
        this.local.set('form');
        this.error.set('Checkout is not set up in US dollars yet, so no payment was started. Write to admin@iso8583.studio.');
        return;
      }

      this.charge.set(formatMinor(amountPaise, currency));
      // checkout_id stitches begin_checkout to the purchase on return; the
      // redirect waits for the beacon (max 400ms).
      const checkoutId = crypto.randomUUID();
      this.payments.rememberCheckoutId(checkoutId);
      this.analytics.reportBeginCheckout(amountPaise, checkoutId, () => {
        location.assign(checkoutUrl);
      }, currency);
    } catch (err) {
      this.local.set('form');
      const e = err as { code?: string; message?: string; requestId?: string | null };
      this.analytics.reportCheckoutError(e.code ?? 'network_or_cors');
      if (e.code) {
        console.error(`payments ${e.code} (request ${e.requestId ?? 'unknown'}): ${e.message}`);
        this.error.set(messageFor(e as never));
      } else {
        // A blocked preflight looks identical to a network failure: only the
        // deployed origin is allowlisted, so this is what a dev server sees.
        console.warn(`Pro checkout: the request to ${PAYMENTS.baseUrl} did not complete.`, err);
        this.error.set('Payment could not be started. Check your connection and try again.');
      }
    }
  }

  ngOnDestroy(): void { this.io?.disconnect(); }
}
