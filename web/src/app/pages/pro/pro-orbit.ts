import {
  ChangeDetectionStrategy, Component, ElementRef, NgZone, OnDestroy, PLATFORM_ID,
  afterNextRender, inject, viewChild, viewChildren,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { UiIcon } from '../../ui/icon';
import { prefersReducedMotion, watchVisible } from './pro-motion';

/**
 * The Pro hero's orbital system: every simulator and tool family circling a
 * PRO core on three tilted rings. A canvas draws the star dust, dial ticks,
 * comet trails and the beams from the front chips into the core; the chips
 * themselves are elements, placed each frame. The pointer tilts the scene.
 *
 * Decoration only, and hidden from assistive tech. The prerendered state is
 * the still part of the drawing — glow, floor grid, horizon and the core —
 * with the rings and chips absent until the browser has measured the box.
 * It runs only while on screen, and under prefers-reduced-motion it draws one
 * settled frame and stops.
 *
 * Where the system sits in its box comes from the stylesheet, so one element
 * serves both layouts without a width check in script: `--orbit-gx` is the
 * core's horizontal position (0–1), `--orbit-k` the ring radius as a fraction
 * of the width. Styles: styles/bundles/pro.css.
 */

type Tone = 'available' | 'beta' | 'dev' | 'tools';
interface OrbitItem { icon: string; label: string; tag: string; tone: Tone; }

const ITEMS: OrbitItem[] = [
  { icon: 'arrows-left-right', label: 'Host Simulator', tag: 'Available', tone: 'available' },
  { icon: 'key', label: 'HSM Simulator', tag: 'Available', tone: 'available' },
  { icon: 'terminal-window', label: 'HSM Console', tag: 'Beta', tone: 'beta' },
  { icon: 'credit-card', label: 'POS Simulator', tag: 'Beta', tone: 'beta' },
  { icon: 'sim-card', label: 'APDU Simulator', tag: 'Beta', tone: 'beta' },
  { icon: 'shuffle', label: 'Switch Simulator', tag: 'Dev', tone: 'dev' },
  { icon: 'bank', label: 'Issuer System', tag: 'Dev', tone: 'dev' },
  { icon: 'money', label: 'ATM Simulator', tag: 'Dev', tone: 'dev' },
  { icon: 'printer', label: 'ECR Simulator', tag: 'Dev', tone: 'dev' },
  { icon: 'cards', label: 'EMV & Card Tools', tag: 'Tools', tone: 'tools' },
  { icon: 'lock-key', label: 'Cryptographic Tools', tag: 'Tools', tone: 'tools' },
  { icon: 'key', label: 'Key Management', tag: 'Tools', tone: 'tools' },
  { icon: 'keyboard', label: 'Payment Utilities', tag: 'Tools', tone: 'tools' },
  { icon: 'swap', label: 'Data Converters', tag: 'Tools', tone: 'tools' },
];

interface Ring { k: number; tilt: number; rz: number; dy: number; T: number; dir: number; items: number[]; }

const RINGS: Ring[] = [
  { k: 1, tilt: 66, rz: -8, dy: 0, T: 34000, dir: 1, items: [0, 3, 5, 7, 9, 11] },
  { k: .7, tilt: 44, rz: 5, dy: 0, T: 24000, dir: -1, items: [1, 4, 8, 12] },
  { k: .48, tilt: 56, rz: -3, dy: -.04, T: 16000, dir: 1, items: [2, 6, 10, 13] },
];

/** Deterministic star field, so every visit draws the same sky. */
const STARS = Array.from({ length: 80 }, (_, i) => {
  const r = (n: number) => { const x = Math.sin(i * 12.9898 + n * 78.233) * 43758.5453; return x - Math.floor(x); };
  return { x: r(1), y: r(2), r: .5 + r(3) * 1.1, ph: r(4) * 6.28, sp: 900 + r(5) * 1400 };
});

/** The frame shown when nothing may move. */
const STILL_FRAME_MS = 5000;
const TAU = Math.PI * 2;

interface Placed { n: HTMLElement; i: number; x: number; y: number; d: number; nearCore: number; s: number; op: number; w: number; h: number; }

@Component({
  selector: 'app-pro-orbit',
  imports: [UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'pro-orbit', 'aria-hidden': 'true' },
  template: `
    <span class="pro-orbit-blob pro-orbit-blob--a" #blobA></span>
    <span class="pro-orbit-blob pro-orbit-blob--b" #blobB></span>
    <div class="pro-orbit-floor" #floor></div>
    <span class="pro-orbit-horizon"></span>
    <div class="pro-orbit-scene" #scene>
      <canvas #cvs></canvas>
      <div class="pro-orbit-hub">
        <span class="pro-orbit-beam"></span>
        <span class="pro-orbit-dial" #dial></span>
        <span class="pro-orbit-dial-ring"></span>
      </div>
      @for (half of ringHalves; track half) {
        <span class="pro-orbit-ring" [class.pro-orbit-ring--front]="half % 2 === 1" #ring></span>
      }
      <div class="pro-orbit-core" #core>
        <span class="pro-orbit-core-glow"></span>
        <span class="pro-orbit-halo" #halo></span>
        <span class="pro-orbit-orb">
          <span class="pro-orbit-sheen"></span>
          <span class="pro-orbit-word">PRO</span>
        </span>
      </div>
      @for (chip of chips; track $index) {
        <div [class]="'pro-orbit-chip pro-orbit-chip--' + chip.tone" #chip>
          <span class="pro-orbit-chip-icon"><ui-icon [name]="chip.icon" [size]="13" /></span>
          <span class="pro-orbit-chip-text">
            <span class="pro-orbit-chip-name">{{ chip.label }}</span>
            <span class="pro-orbit-chip-tag"><i></i>{{ chip.tag }}</span>
          </span>
        </div>
      }
    </div>
  `,
})
export class ProOrbit implements OnDestroy {
  /** Two half-ellipses per ring: the back half sits behind the core, the front half over it. */
  protected readonly ringHalves = RINGS.flatMap((_, i) => [i * 2, i * 2 + 1]);
  /** In ring order, which is the order the frame loop walks them in. */
  protected readonly chips = RINGS.flatMap((ring) => ring.items).map((index) => ITEMS[index]);

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  private readonly canvasRef = viewChild.required<ElementRef<HTMLCanvasElement>>('cvs');
  private readonly sceneRef = viewChild.required<ElementRef<HTMLElement>>('scene');
  private readonly coreRef = viewChild.required<ElementRef<HTMLElement>>('core');
  private readonly haloRef = viewChild.required<ElementRef<HTMLElement>>('halo');
  private readonly dialRef = viewChild.required<ElementRef<HTMLElement>>('dial');
  private readonly floorRef = viewChild.required<ElementRef<HTMLElement>>('floor');
  private readonly blobARef = viewChild.required<ElementRef<HTMLElement>>('blobA');
  private readonly blobBRef = viewChild.required<ElementRef<HTMLElement>>('blobB');
  private readonly ringRefs = viewChildren<ElementRef<HTMLElement>>('ring');
  private readonly chipRefs = viewChildren<ElementRef<HTMLElement>>('chip');

  private teardown: (() => void)[] = [];
  private raf = 0;

  constructor() {
    if (!isPlatformBrowser(inject(PLATFORM_ID))) return;
    const zone = inject(NgZone);
    afterNextRender(() => zone.runOutsideAngular(() => this.start()));
  }

  private start(): void {
    const el = this.host;
    const view = el.ownerDocument.defaultView;
    const c = this.canvasRef().nativeElement;
    const ctx = c.getContext('2d');
    if (!view || !ctx) return;

    const reduced = prefersReducedMotion(el);
    const scene = this.sceneRef().nativeElement;
    const core = this.coreRef().nativeElement;
    const halo = this.haloRef().nativeElement;
    const dial = this.dialRef().nativeElement;
    const floor = this.floorRef().nativeElement;
    const blobA = this.blobARef().nativeElement;
    const blobB = this.blobBRef().nativeElement;
    const rings = this.ringRefs().map((r) => r.nativeElement);
    const chips = this.chipRefs().map((r) => r.nativeElement);

    let W = 0, H = 0, gx = .5, k = .36;
    let mx = 0, my = 0, sx = 0, sy = 0;
    let elapsed = 0, last = 0, visible = false;
    // Chip boxes never change between resizes, so they are measured once per
    // resize rather than read back from layout on every frame.
    let chipSize: { w: number; h: number }[] = [];

    const measure = () => { chipSize = chips.map((n) => ({ w: n.offsetWidth, h: n.offsetHeight })); };

    const size = () => {
      W = el.clientWidth;
      H = el.clientHeight;
      const style = view.getComputedStyle(el);
      gx = parseFloat(style.getPropertyValue('--orbit-gx')) || .5;
      k = parseFloat(style.getPropertyValue('--orbit-k')) || .36;
      const dpr = Math.min(2, view.devicePixelRatio || 1);
      c.width = Math.max(1, Math.round(W * dpr));
      c.height = Math.max(1, Math.round(H * dpr));
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      measure();
    };

    const draw = (t: number) => {
      sx += (mx - sx) * .05;
      sy += (my - sy) * .05;
      const cx = W * gx, cy = H / 2;
      const base = Math.min(W * k, H * .62);
      const cs = Math.min(1, Math.max(.62, base / 340));

      scene.style.transform = `perspective(1400px) rotateX(${(-sy * 4).toFixed(2)}deg) rotateY(${(sx * 6).toFixed(2)}deg)`;
      ctx.clearRect(0, 0, W, H);

      for (const s of STARS) {
        const a = .12 + .3 * (.5 + .5 * Math.sin(t / s.sp + s.ph));
        ctx.fillStyle = `rgba(190,215,255,${a.toFixed(3)})`;
        ctx.beginPath();
        ctx.arc(s.x * W + sx * 6 * s.r, s.y * H + sy * 4 * s.r, s.r, 0, TAU);
        ctx.fill();
      }

      const front: [number, number, number][] = [];
      const placed: Placed[] = [];
      let ci = 0;

      RINGS.forEach((rg, ri) => {
        const rx = base * rg.k, tilt = (rg.tilt * Math.PI) / 180, ry = rx * Math.cos(tilt);
        const rz = (rg.rz * Math.PI) / 180, st = Math.sin(tilt), cz = Math.cos(rz), sz = Math.sin(rz);
        const oy = cy + rg.dy * base;
        const px = (x0: number, y0: number): [number, number] => [cx + x0 * cz - y0 * sz, oy + x0 * sz + y0 * cz];

        for (let half = 0; half < 2; half++) {
          const r = rings[ri * 2 + half];
          if (!r) continue;
          r.style.width = `${rx * 2}px`;
          r.style.height = `${ry * 2}px`;
          r.style.transform = `translate(${cx}px,${oy}px) translate(-50%,-50%) rotate(${rg.rz}deg)`;
        }

        if (ri === 0) {
          ctx.lineWidth = 1;
          for (let i = 0; i < 72; i++) {
            const th = (i * Math.PI) / 36, d = (Math.sin(th) * st + 1) / 2, k2 = i % 6 ? 1.1 : 1.14;
            const a = px(rx * 1.07 * Math.cos(th), ry * 1.07 * Math.sin(th));
            const b = px(rx * k2 * Math.cos(th), ry * k2 * Math.sin(th));
            ctx.strokeStyle = `rgba(150,190,240,${(.05 + d * .16).toFixed(3)})`;
            ctx.beginPath(); ctx.moveTo(a[0], a[1]); ctx.lineTo(b[0], b[1]); ctx.stroke();
          }
        }

        // The comet: thirty short arcs, fading and thinning away from the head.
        const head = rg.dir * (t / (rg.T * .42)) * TAU + ri * 2.1, N = 30, dth = .05;
        ctx.shadowColor = 'rgba(30,136,229,.9)';
        for (let n = 0; n < N; n++) {
          const a = head - rg.dir * n * dth, b = head - rg.dir * (n + 1) * dth;
          const d = (Math.sin(a) * st + 1) / 2, al = (1 - n / N) * (.2 + .8 * d);
          ctx.strokeStyle = `rgba(165,212,255,${(al * .95).toFixed(3)})`;
          ctx.lineWidth = .6 + (1 - n / N) * 1.7;
          ctx.shadowBlur = n < 3 ? 12 : 0;
          ctx.beginPath();
          ctx.ellipse(cx, oy, rx, ry, rz, Math.min(a, b), Math.max(a, b), false);
          ctx.stroke();
        }
        ctx.shadowBlur = 0;

        rg.items.forEach((_, j) => {
          const i = ci++, n = chips[i];
          if (!n) return;
          const th = rg.dir * (t / rg.T) * TAU + (j * TAU) / rg.items.length;
          const z = Math.sin(th) * st, d = (z + 1) / 2;
          const [bx, by] = px(rx * Math.cos(th), ry * Math.sin(th));
          const x = bx + z * sx * 16, y = by + z * sy * 10;
          const reveal = reduced ? 1 : Math.min(1, Math.max(0, (t - 150 - i * 90) / 700));
          const nearCore = Math.max(0, 1 - Math.hypot(x - cx, (y - cy) * 1.6) / 120);
          placed.push({
            n, i, x, y, d, nearCore, w: 0, h: 0,
            s: (.7 + d * .38) * (.7 + .3 * reveal) * cs,
            op: (.28 + d * .72) * (1 - nearCore * .75) * reveal,
          });
        });
      });

      // Front to back, fading whatever a nearer chip already covers.
      placed.sort((a, b) => b.d - a.d);
      const kept: Placed[] = [];
      for (const p of placed) {
        const box = chipSize[p.i] ?? { w: 0, h: 0 };
        const w = box.w * p.s, h = box.h * p.s;
        let overlap = 0;
        if (w > 0 && h > 0) {
          for (const q of kept) {
            const ix = Math.min(p.x + w / 2, q.x + q.w / 2) - Math.max(p.x - w / 2, q.x - q.w / 2);
            const iy = Math.min(p.y + h / 2, q.y + q.h / 2) - Math.max(p.y - h / 2, q.y - q.h / 2);
            if (ix > 0 && iy > 0) overlap = Math.max(overlap, (ix * iy) / (w * h));
          }
        }
        p.op *= 1 - Math.min(1, overlap * 1.6);
        p.w = w; p.h = h;
        if (p.op > .3) kept.push(p);
        p.n.style.transform = `translate(${p.x.toFixed(1)}px,${p.y.toFixed(1)}px) translate(-50%,-50%) scale(${p.s.toFixed(3)})`;
        p.n.style.opacity = p.op.toFixed(3);
        p.n.style.zIndex = String(2 + Math.round(p.d * 96));
        p.n.style.filter = `blur(${((1 - p.d) * 1.4).toFixed(2)}px)`;
        p.n.style.setProperty('--g', p.d.toFixed(3));
        if (p.d > .72 && p.op > .5 && p.nearCore < .2) front.push([p.x, p.y, (p.d - .72) / .28]);
      }

      ctx.lineWidth = 1;
      for (const [x, y, f] of front) {
        const g = ctx.createLinearGradient(x, y, cx, cy);
        g.addColorStop(0, `rgba(120,190,255,${(f * .55).toFixed(3)})`);
        g.addColorStop(1, `rgba(120,190,255,${(f * .06).toFixed(3)})`);
        ctx.strokeStyle = g;
        ctx.beginPath(); ctx.moveTo(x, y); ctx.lineTo(cx, cy); ctx.stroke();
        const q = (t / 1400 + x * .01) % 1, qx = x + (cx - x) * q, qy = y + (cy - y) * q;
        ctx.fillStyle = `rgba(214,236,255,${(f * (1 - q)).toFixed(3)})`;
        ctx.beginPath(); ctx.arc(qx, qy, 2, 0, TAU); ctx.fill();
      }

      // The core, hub and glows are anchored in the stylesheet at the same
      // point the rings are centred on, so only their motion is written here.
      const breath = 1 + Math.sin(t / 900) * .025;
      core.style.transform = `translate(-50%,-50%) scale(${breath.toFixed(3)})`;
      halo.style.transform = `rotate(${((t / 4200) * 360) % 360}deg)`;
      dial.style.transform = `rotate(${(-(t / 26000) * 360) % 360}deg)`;
      floor.style.backgroundPosition = `${(sx * 20).toFixed(1)}px ${((t / 36) % 56).toFixed(1)}px`;
      blobA.style.transform = `translate(${(Math.sin(t / 7000) * 60).toFixed(1)}px,${(Math.cos(t / 9000) * 40).toFixed(1)}px) translate(-50%,-50%)`;
      blobB.style.transform = `translate(${(base * .9 + Math.cos(t / 8000) * 50).toFixed(1)}px,${(-base * .3 + Math.sin(t / 6000) * 40).toFixed(1)}px) translate(-50%,-50%)`;
    };

    const frame = (now: number) => {
      if (last) elapsed += Math.min(100, now - last);
      last = now;
      draw(elapsed);
      this.raf = visible ? view.requestAnimationFrame(frame) : 0;
    };

    const setRunning = (on: boolean) => {
      visible = on;
      el.classList.toggle('is-running', on);
      if (on && !this.raf) { last = 0; this.raf = view.requestAnimationFrame(frame); }
    };

    size();
    el.classList.add('is-live');
    draw(reduced ? STILL_FRAME_MS : 0);

    if ('ResizeObserver' in view) {
      const ro = new ResizeObserver(() => {
        size();
        // While the loop is running the next frame picks the new box up.
        if (!this.raf) draw(reduced ? STILL_FRAME_MS : elapsed);
      });
      ro.observe(el);
      this.teardown.push(() => ro.disconnect());
    }

    // Chip widths settle once the web font has loaded.
    void el.ownerDocument.fonts?.ready.then(() => {
      if (!el.isConnected) return;
      measure();
      if (!this.raf) draw(reduced ? STILL_FRAME_MS : elapsed);
    });

    if (!reduced) {
      const onMove = (e: PointerEvent) => {
        if (!visible) return;
        const r = el.getBoundingClientRect();
        mx = Math.max(-1, Math.min(1, ((e.clientX - r.left) / r.width - .5) * 2));
        my = Math.max(-1, Math.min(1, ((e.clientY - r.top) / r.height - .5) * 2));
      };
      view.addEventListener('pointermove', onMove, { passive: true });
      this.teardown.push(() => view.removeEventListener('pointermove', onMove));
      this.teardown.push(watchVisible(el, '80px', setRunning));
    }

    this.teardown.push(() => {
      visible = false;
      view.cancelAnimationFrame(this.raf);
      this.raf = 0;
    });
  }

  ngOnDestroy(): void {
    this.teardown.forEach((fn) => fn());
    this.teardown = [];
  }
}
