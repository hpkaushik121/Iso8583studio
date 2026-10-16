import {
  ChangeDetectionStrategy, Component, Directive, ElementRef, NgZone, OnDestroy, PLATFORM_ID,
  afterNextRender, computed, inject, input, signal, viewChild,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

/**
 * The decorative motion on the Pro page: the throttled-versus-multicore packet
 * lanes, the algorithm chips, the early-access queue bar and the cursor-tracked
 * border light.
 *
 * Every piece is decoration. Each renders a settled state on the server, starts
 * in the browser after the first render, loops only while it is on screen, and
 * holds a single still frame under prefers-reduced-motion. None of them reads
 * or writes anything the registration form depends on.
 *
 * Styles: styles/bundles/pro.css.
 */

/** Calls `onChange` as the element enters and leaves the viewport. */
export function watchVisible(
  el: HTMLElement, margin: string, onChange: (visible: boolean) => void,
): () => void {
  const view = el.ownerDocument.defaultView;
  if (!view || !('IntersectionObserver' in view)) {
    onChange(true);
    return () => {};
  }
  const io = new IntersectionObserver(([entry]) => onChange(entry.isIntersecting), { rootMargin: margin });
  io.observe(el);
  return () => io.disconnect();
}

export function prefersReducedMotion(el: HTMLElement): boolean {
  return !!el.ownerDocument.defaultView?.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

/**
 * Pointer tracking for the cursor lights. Writes the pointer position to
 * custom properties on the host (--mx, --my, and --sp as 0 or 1) and nothing
 * else; what is lit is up to the stylesheet — the border light of a .pro-spot
 * card, or the bright dots under the pointer in the hero.
 */
@Directive({ selector: '[proSpotlight]' })
export class ProSpotlight implements OnDestroy {
  private readonly el = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  private teardown: (() => void) | null = null;

  constructor() {
    if (!isPlatformBrowser(inject(PLATFORM_ID))) return;
    const zone = inject(NgZone);
    afterNextRender(() => zone.runOutsideAngular(() => this.start()));
  }

  private start(): void {
    const el = this.el;
    const move = (e: PointerEvent) => {
      const r = el.getBoundingClientRect();
      el.style.setProperty('--mx', `${e.clientX - r.left}px`);
      el.style.setProperty('--my', `${e.clientY - r.top}px`);
      el.style.setProperty('--sp', '1');
    };
    const leave = () => el.style.setProperty('--sp', '0');
    el.addEventListener('pointermove', move, { passive: true });
    el.addEventListener('pointerleave', leave);
    this.teardown = () => {
      el.removeEventListener('pointermove', move);
      el.removeEventListener('pointerleave', leave);
    };
  }

  ngOnDestroy(): void {
    this.teardown?.();
  }
}

interface Packet { x: number; y: number; v: number; hue?: boolean; out?: boolean; }

/**
 * One packet lane. `free` passes a single packet through a throttle gate at a
 * time and queues the rest behind it; `pro` streams across four tracks.
 */
@Component({
  selector: 'app-pro-lane',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'pro-lane',
    '[class.pro-lane--pro]': "kind() === 'pro'",
    '[style.--lane-h]': 'height()',
    'aria-hidden': 'true',
  },
  template: `<canvas #cv></canvas>`,
})
export class ProLane implements OnDestroy {
  readonly kind = input.required<'free' | 'pro'>();
  /** Lane height in px. */
  readonly height = input(64);

  private readonly canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('cv');
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  private teardown: (() => void)[] = [];
  private raf = 0;

  constructor() {
    if (!isPlatformBrowser(inject(PLATFORM_ID))) return;
    const zone = inject(NgZone);
    afterNextRender(() => zone.runOutsideAngular(() => this.start()));
  }

  private start(): void {
    const view = this.host.ownerDocument.defaultView;
    const c = this.canvas().nativeElement;
    const ctx = c.getContext('2d');
    if (!view || !ctx) return;

    const reduced = prefersReducedMotion(this.host);
    const dpr = Math.min(2, view.devicePixelRatio || 1);
    const pro = this.kind() === 'pro';
    const h = this.height();
    const tracks = pro ? 4 : 1;
    const ys = Array.from({ length: tracks }, (_, i) => Math.round((h * (i + 1)) / (tracks + 1)) + .5);

    let w = 0;
    let packets: Packet[] = [];
    let queue = 0, spawnT = 0, gateT = 0, flash = 0, last = 0;
    let visible = false;

    const gateX = () => Math.round(w * .5);

    const step = (dt: number) => {
      spawnT += dt;
      const every = pro ? .075 : .3;
      while (spawnT > every) {
        spawnT -= every;
        if (pro) {
          packets.push({ x: -20, y: ys[(Math.random() * tracks) | 0], v: 300 + Math.random() * 160, hue: Math.random() < .3 });
        } else if (queue + packets.filter((p) => !p.out).length < 10) {
          packets.push({ x: -20, y: ys[0], v: 170, out: false });
        }
      }
      if (!pro) {
        gateT += dt;
        flash = Math.max(0, flash - dt * 3);
        if (gateT > .85 && queue) {
          gateT = 0; queue--; flash = 1;
          packets.push({ x: gateX() + 6, y: ys[0], v: 230, out: true });
        }
        packets = packets.filter((p) => {
          if (!p.out && p.x >= gateX() - 14 - queue * 14) { queue++; return false; }
          return true;
        });
      }
      packets.forEach((p) => { p.x += p.v * dt; });
      packets = packets.filter((p) => p.x < w + 30);
    };

    const pill = (x: number, y: number, len: number, colour: string) => {
      ctx.fillStyle = colour;
      ctx.beginPath();
      if (typeof ctx.roundRect === 'function') ctx.roundRect(x - len, y - 2, len, 4, 2);
      else ctx.rect(x - len, y - 2, len, 4);
      ctx.fill();
    };

    const draw = () => {
      ctx.clearRect(0, 0, w, h);
      ctx.strokeStyle = 'rgba(233,236,245,.07)';
      ctx.lineWidth = 1;
      ys.forEach((y) => { ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(w, y); ctx.stroke(); });
      packets.forEach((p) => {
        const tail = pro ? 46 : 26;
        const g = ctx.createLinearGradient(p.x - tail, 0, p.x, 0);
        const rgb = pro ? (p.hue ? '100,216,203' : '106,183,255') : '139,147,167';
        g.addColorStop(0, `rgba(${rgb},0)`);
        g.addColorStop(1, `rgba(${rgb},.55)`);
        ctx.strokeStyle = g;
        ctx.lineWidth = 2;
        ctx.beginPath(); ctx.moveTo(p.x - tail, p.y); ctx.lineTo(p.x - 10, p.y); ctx.stroke();
        pill(p.x, p.y, 10, `rgba(${rgb},1)`);
      });
      if (!pro) {
        for (let i = 0; i < queue; i++) pill(gateX() - 4 - i * 14, ys[0], 10, `rgba(139,147,167,${.75 - i * .06})`);
        ctx.fillStyle = `rgba(233,236,245,${.28 + flash * .5})`;
        ctx.fillRect(gateX(), ys[0] - 16, 2, 32);
      }
    };

    const size = () => {
      w = this.host.clientWidth;
      c.width = Math.max(1, Math.round(w * dpr));
      c.height = Math.max(1, Math.round(h * dpr));
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    };

    const loop = (t: number) => {
      const dt = Math.min(.05, (t - (last || t)) / 1000);
      last = t;
      step(dt);
      draw();
      this.raf = visible ? view.requestAnimationFrame(loop) : 0;
    };

    size();
    // Three seconds of simulated traffic, so the first frame — the only frame
    // under reduced motion — already shows the lane in its steady state.
    for (let i = 0; i < 180; i++) step(1 / 60);
    draw();
    this.host.classList.add('is-live');

    if ('ResizeObserver' in view) {
      const ro = new ResizeObserver(() => { size(); draw(); });
      ro.observe(this.host);
      this.teardown.push(() => ro.disconnect());
    }

    if (!reduced) {
      this.teardown.push(watchVisible(this.host, '120px', (now) => {
        visible = now;
        if (visible && !this.raf) { last = 0; this.raf = view.requestAnimationFrame(loop); }
      }));
    }
    this.teardown.push(() => { visible = false; view.cancelAnimationFrame(this.raf); this.raf = 0; });
  }

  ngOnDestroy(): void {
    this.teardown.forEach((fn) => fn());
    this.teardown = [];
  }
}

/** The algorithms Pro adds on top of the free build's 3DES and AES. */
const ALGORITHMS = ['RSA', 'ECC', 'SHA-3', 'FF1', 'FF3-1', 'Poly1305', 'ChaCha20', 'DUKPT AES'];

/** Algorithm chips with a highlight that travels along them. */
@Component({
  selector: 'app-pro-algos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'pro-algos' },
  template: `@for (algo of algorithms; track algo; let i = $index) {<span class="pro-algo" [class.is-on]="i === active()">{{ algo }}</span>}`,
})
export class ProAlgos implements OnDestroy {
  protected readonly algorithms = ALGORITHMS;
  /** -1 until the highlight starts, which is also the prerendered state. */
  protected readonly active = signal(-1);

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  private timer: ReturnType<typeof setInterval> | null = null;
  private unwatch: (() => void) | null = null;

  constructor() {
    if (!isPlatformBrowser(inject(PLATFORM_ID))) return;
    afterNextRender(() => {
      if (prefersReducedMotion(this.host)) return;
      this.unwatch = watchVisible(this.host, '0px', (visible) => {
        if (visible && !this.timer) {
          this.timer = setInterval(() => this.active.update((n) => (n + 1) % ALGORITHMS.length), 900);
        } else if (!visible) {
          this.stop();
        }
      });
    });
  }

  private stop(): void {
    if (this.timer) clearInterval(this.timer);
    this.timer = null;
  }

  ngOnDestroy(): void {
    this.unwatch?.();
    this.stop();
  }
}

const QUEUE_BARS = 36;

/**
 * Where an amount would sit in the early-access queue, on a log scale between
 * the form's minimum and maximum. A drawing of the rule "higher amount, higher
 * priority" — it invents no counts and is hidden from assistive tech, which
 * gets the same rule as text from the field's own hint.
 *
 * `amount` is read-only here: the form passes its control's value in and this
 * component never writes back.
 */
@Component({
  selector: 'app-pro-queue',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'pro-queue', 'aria-hidden': 'true' },
  template: `
    <div class="pro-queue-bars">
      @for (bar of bars; track bar) {
        <span [class.is-me]="bar === index()" [class.is-ahead]="bar < index()"
              [style.--dl]="index() < 0 ? 0 : distance(bar) * 9"></span>
      }
    </div>
    <div class="pro-queue-legend">
      <span>Earlier access</span>
      <span class="pro-queue-place">{{ index() < 0 ? 'Enter an amount to see your place' : 'Your place in the early-access queue' }}</span>
      <span>Later</span>
    </div>
  `,
})
export class ProQueue {
  readonly amount = input<number | string | null>(null);
  readonly min = input(2);
  readonly max = input(100000);

  protected readonly bars = Array.from({ length: QUEUE_BARS }, (_, i) => i);

  /** The highlighted bar, or -1 while there is no usable amount. */
  protected readonly index = computed(() => {
    const raw = this.amount();
    const amount = raw === null || raw === '' ? NaN : Number(raw);
    const min = this.min(), max = this.max();
    if (!Number.isFinite(amount) || amount < min) return -1;
    const position = Math.min(1, Math.log(amount / min) / Math.log(max / min));
    return Math.round((1 - position) * (QUEUE_BARS - 1));
  });

  protected distance(bar: number): number {
    return Math.abs(bar - this.index());
  }
}
