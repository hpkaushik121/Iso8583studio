import {
  ChangeDetectionStrategy, Component, ElementRef, OnDestroy, PLATFORM_ID, afterNextRender,
  inject, input, signal, viewChild,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { UiDialog, UiIcon } from '../../ui';
import { ProSpotlight } from './pro-motion';
import { ProForm } from './pro-form';

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
 * The card is the design's, prices included. "Continue to payment" opens the
 * full registration form in a dialog, with the email from the card already
 * filled in; checkout needs more than an email, so the card cannot take the
 * payment itself.
 *
 * The button is an anchor to /pro#register inside a .pro-nudge wrapper: that
 * is what analytics reports as pro_click, and without script it still takes
 * the reader to the form. The dialog is a 'sheet', so the form's clicks are
 * not mistaken for the download interstitial's.
 *
 * The fan opens when the stack scrolls into view. The prerendered page shows
 * it open; in the browser a stack that is still below the viewport is closed
 * first and opens on arrival, and under reduced motion it simply stays open.
 *
 * Styles: styles/bundles/_pro-reserve.css, imported by home.css and pro.css.
 */
@Component({
  selector: 'app-pro-reserve',
  imports: [UiDialog, UiIcon, ProSpotlight, ProForm],
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
            <div class="rsv-form pro-nudge">
              <label class="rsv-input">
                <ui-icon name="envelope-simple" [size]="16" />
                <input #email type="email" aria-label="Work email" placeholder="you&#64;company.com"
                       autocomplete="email" (keydown.enter)="$event.preventDefault(); openForm(email.value)">
              </label>
              <a class="btn btn--primary" href="/pro#register"
                 (click)="$event.preventDefault(); openForm(email.value)">
                Continue to payment<ui-icon name="arrow-right" [size]="16" />
              </a>
            </div>
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

    @if (formOpen()) {
      <div class="rsv-dialog">
        <ui-dialog kind="sheet" label="Register for Pro" (close)="formOpen.set(false)">
          <h3>Register for Pro</h3>
          <p>A few details for checkout. We provision your workspace and send credentials by email.</p>
          <app-pro-form [email]="prefill()" />
        </ui-dialog>
      </div>
    }
  `,
})
export class ProReserve implements OnDestroy {
  /** Blur the card up from an ancestor uiReveal, as the home section does. */
  readonly stagger = input(false);
  /** The line under the button; the two pages word it differently. */
  readonly fine = input('Refundable until launch');

  protected readonly cards = fan();
  protected readonly formOpen = signal(false);
  protected readonly prefill = signal('');
  protected readonly features = [
    'Raised CPS ceiling', 'Full algorithm set', 'Deep simulator tweaks',
    'Hosted cloud endpoints', 'Priority support', 'Team configuration sync',
  ];

  /** True while the fan is gathered into one stack, waiting to open. */
  protected readonly closed = signal(false);

  private readonly stack = viewChild.required<ElementRef<HTMLElement>>('stack');
  private io: IntersectionObserver | null = null;

  constructor() {
    if (!isPlatformBrowser(inject(PLATFORM_ID))) return;
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

  protected openForm(email: string): void {
    this.prefill.set(email.trim());
    this.formOpen.set(true);
  }

  ngOnDestroy(): void { this.io?.disconnect(); }
}

