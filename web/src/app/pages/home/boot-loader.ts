import {
  ChangeDetectionStrategy, Component, DestroyRef, DOCUMENT, PLATFORM_ID, computed, inject, signal,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { UiLogoMark } from '../../ui/logo-mark';

/** A load shorter than this never shows the loader, so a warm cache does not flash it. */
const SHOW_AFTER_MS = 150;
/** Once shown, the loader stays at least this long so the mark finishes drawing itself. */
const MIN_SHOWN_MS = 1100;
/** Nothing waits longer than this: a stalled image must not hold the page hostage. */
const MAX_WAIT_MS = 5000;
/** Set on <html> by the inline script in index.html before first paint; it pauses the hero's entrance. */
const BOOT_CLASS = 'is-booting';

type Step = 'fonts' | 'map' | 'paint';
const STEP_LABEL: Record<Step, string> = {
  fonts: 'Shaping the type',
  map: 'Lighting the payment rails',
  paint: 'Opening the workbench',
};

/**
 * Full-screen loader for the home page. It covers the hero while the things the hero's
 * entrance depends on arrive — the IBM Plex faces, the dotted world map behind the hero, and
 * the first painted frame — and only appears at all when that takes longer than a blink. The
 * hero's entrance animations are paused by `html.is-booting` (set before first paint by
 * index.html) and released the moment the loader lifts, so the choreography is never half
 * played behind the curtain. Reduced motion gets a still mark and no transitions.
 *
 * `?boot=1` forces the loader for a few seconds, for looking at it.
 */
@Component({
  selector: 'home-boot-loader',
  imports: [UiLogoMark],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (visible()) {
      <div class="boot" [class.boot--out]="leaving()" role="progressbar" aria-label="Loading ISO8583Studio"
           [attr.aria-valuenow]="percent()" aria-valuemin="0" aria-valuemax="100">
        <div class="boot-wash" aria-hidden="true"></div>
        <div class="boot-dots" aria-hidden="true"></div>
        <div class="boot-core">
          <div class="boot-rail" aria-hidden="true">
            <svg viewBox="0 0 200 200">
              <circle class="boot-orbit" cx="100" cy="100" r="86" />
              <circle class="boot-orbit boot-orbit--inner" cx="100" cy="100" r="62" />
              @for (d of rail; track d.i) {
                <circle class="boot-node" [attr.cx]="d.x" [attr.cy]="d.y" r="2.6" [style.--i]="d.i" />
              }
              <path class="boot-sweep" d="M100 14 A86 86 0 0 1 186 100 L100 100 Z" />
            </svg>
            <div class="boot-mark"><ui-logo-mark mode="brand" [size]="56" /></div>
          </div>
          <p class="boot-name">ISO8583Studio</p>
          <p class="boot-status" aria-live="polite">{{ status() }}</p>
          <div class="boot-bar" aria-hidden="true"><span class="boot-fill" [style.width.%]="percent()"></span></div>
        </div>
      </div>
    }
  `,
  styles: `
    .boot {
      position: fixed; inset: 0; z-index: calc(var(--z-toast) + 10);
      display: grid; place-items: center; overflow: hidden;
      background: var(--canvas); color: var(--text);
      transition: opacity .42s var(--ease), visibility 0s linear .42s;
    }
    .boot--out { opacity: 0; visibility: hidden; pointer-events: none; }
    .boot-wash {
      position: absolute; inset: -20%;
      background:
        radial-gradient(42% 36% at 30% 28%, rgba(30, 136, 229, .22), transparent 70%),
        radial-gradient(38% 32% at 72% 74%, rgba(38, 166, 154, .16), transparent 70%);
      animation: boot-wash 9s ease-in-out infinite alternate;
    }
    .boot-dots {
      position: absolute; inset: 0;
      background-image: radial-gradient(rgba(151, 168, 214, .22) 1px, transparent 1.3px);
      background-size: 18px 18px;
      mask-image: radial-gradient(ellipse at 50% 50%, #000 10%, transparent 68%);
      -webkit-mask-image: radial-gradient(ellipse at 50% 50%, #000 10%, transparent 68%);
    }
    .boot-core { position: relative; display: grid; justify-items: center; gap: 10px; padding: 24px; text-align: center; }
    .boot-rail { position: relative; width: 200px; height: 200px; margin-bottom: 6px; }
    .boot-rail svg { position: absolute; inset: 0; width: 100%; height: 100%; }
    .boot-orbit { fill: none; stroke: rgba(151, 168, 214, .16); stroke-width: 1; }
    .boot-orbit--inner { stroke-dasharray: 2 6; stroke-linecap: round; animation: boot-turn 24s linear infinite reverse; transform-origin: 100px 100px; }
    .boot-node {
      fill: var(--blue-hi); opacity: .25;
      animation: boot-node 2.4s ease-in-out infinite; animation-delay: calc(var(--i) * -.2s);
    }
    .boot-sweep {
      fill: url(#none); fill: rgba(106, 183, 255, .09);
      transform-origin: 100px 100px; animation: boot-turn 2.4s linear infinite;
      mix-blend-mode: screen;
    }
    .boot-mark {
      position: absolute; inset: 0; display: grid; place-items: center;
      filter: drop-shadow(0 0 22px rgba(30, 136, 229, .55));
      animation: boot-breathe 2.4s ease-in-out infinite;
    }
    .boot-name { margin: 0; font-size: var(--fs-md); font-weight: 600; letter-spacing: var(--ls-snug); }
    .boot-status {
      margin: 0; min-height: 1.4em; font-family: var(--mono); font-size: var(--fs-xs); letter-spacing: .08em;
      text-transform: uppercase; color: var(--muted);
      animation: boot-status .5s var(--ease-out) both;
    }
    .boot-status::after { content: ''; display: inline-block; width: 6px; height: 11px; margin-left: 6px; vertical-align: -1px; background: var(--blue-hi); animation: boot-caret 1s steps(1) infinite; }
    .boot-bar { width: min(260px, 60vw); height: 2px; margin-top: 8px; border-radius: 2px; background: rgba(151, 168, 214, .14); overflow: hidden; }
    .boot-fill {
      display: block; height: 100%; border-radius: 2px;
      background: linear-gradient(90deg, var(--blue), var(--teal-hi)); box-shadow: 0 0 10px rgba(30, 136, 229, .6);
      transition: width .35s var(--ease-out);
    }
    @keyframes boot-wash { from { transform: translate3d(-2%, -1%, 0) scale(1); } to { transform: translate3d(2%, 2%, 0) scale(1.06); } }
    @keyframes boot-turn { to { transform: rotate(360deg); } }
    @keyframes boot-node { 0%, 100% { opacity: .22; r: 2.6; } 50% { opacity: 1; r: 3.6; } }
    @keyframes boot-breathe { 0%, 100% { transform: scale(1); } 50% { transform: scale(1.04); } }
    @keyframes boot-status { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: none; } }
    @keyframes boot-caret { 0%, 50% { opacity: 1; } 51%, 100% { opacity: 0; } }
    @media (prefers-reduced-motion: reduce) {
      .boot { transition: none; }
      .boot-wash, .boot-orbit--inner, .boot-node, .boot-sweep, .boot-mark, .boot-status, .boot-status::after, .boot-fill { animation: none; transition: none; }
      .boot-sweep { display: none; }
      .boot-node { opacity: .7; }
    }
  `,
})
export class HomeBootLoader {
  private readonly doc = inject(DOCUMENT);
  protected readonly visible = signal(false);
  protected readonly leaving = signal(false);
  protected readonly done = signal<Set<Step>>(new Set());
  /** Twelve stops on the outer orbit, the nodes a payment would hop between. */
  protected readonly rail = Array.from({ length: 12 }, (_, i) => {
    const a = (i / 12) * Math.PI * 2 - Math.PI / 2;
    return { i, x: +(100 + 86 * Math.cos(a)).toFixed(2), y: +(100 + 86 * Math.sin(a)).toFixed(2) };
  });
  protected readonly percent = computed(() => Math.round((this.done().size / 3) * 100));
  protected readonly status = computed(() => {
    const pending = (['fonts', 'map', 'paint'] as Step[]).find((s) => !this.done().has(s));
    return pending ? STEP_LABEL[pending] : 'Ready';
  });

  constructor() {
    if (!isPlatformBrowser(inject(PLATFORM_ID))) return;
    const destroyRef = inject(DestroyRef);
    const html = this.doc.documentElement;
    const forced = /[?&]boot=1/.test(location.search);
    const still = matchMedia('(prefers-reduced-motion: reduce)').matches;
    const start = performance.now();
    let shownAt = 0;
    let finished = false;
    const timers: ReturnType<typeof setTimeout>[] = [];

    const release = () => {
      if (finished) return;
      finished = true;
      html.classList.remove(BOOT_CLASS);
      if (!this.visible()) return;
      this.leaving.set(true);
      timers.push(setTimeout(() => this.visible.set(false), still ? 0 : 450));
    };
    const maybeFinish = () => {
      if (finished || this.done().size < 3) return;
      const elapsed = performance.now() - start;
      const hold = shownAt ? Math.max(0, (forced ? 2600 : MIN_SHOWN_MS) - (performance.now() - shownAt)) : 0;
      if (elapsed < SHOW_AFTER_MS && !forced) { release(); return; }
      timers.push(setTimeout(release, hold));
    };
    const mark = (step: Step) => {
      if (finished) return;
      this.done.update((d) => new Set(d).add(step));
      maybeFinish();
    };

    // The things the hero's first frames depend on.
    const fonts = (this.doc as Document).fonts?.ready ?? Promise.resolve();
    fonts.then(() => mark('fonts'), () => mark('fonts'));

    const map = new Image();
    map.src = devicePixelRatio > 1.5 ? '/media/hero/world-dots-2880.webp' : '/media/hero/world-dots-1440.webp';
    const mapDone = () => mark('map');
    if (map.decode) map.decode().then(mapDone, mapDone); else { map.onload = mapDone; map.onerror = mapDone; }

    requestAnimationFrame(() => requestAnimationFrame(() => mark('paint')));

    // Only a load that actually takes time gets the curtain.
    timers.push(setTimeout(() => {
      if (finished) return;
      shownAt = performance.now();
      this.visible.set(true);
    }, forced ? 0 : SHOW_AFTER_MS));
    timers.push(setTimeout(release, MAX_WAIT_MS));

    destroyRef.onDestroy(() => { timers.forEach(clearTimeout); html.classList.remove(BOOT_CLASS); });
  }
}
