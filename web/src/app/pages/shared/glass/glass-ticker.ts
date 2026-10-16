import { DOCUMENT, Injectable, inject } from '@angular/core';

/** Something that animates in steps while it is on screen. */
export interface GlassClient {
  /** The element whose visibility decides whether the client runs. */
  el: HTMLElement;
  /** Milliseconds between steps. */
  period: number;
  /** Milliseconds before the first step after the client first comes into view (default: period). */
  delay?: number;
  /** Advance one step. */
  tick(): void;
  /** Called when the client enters or leaves the viewport; `first` is true the first time it enters. */
  visible?(on: boolean, first: boolean): void;
}

interface Entry {
  client: GlassClient;
  on: boolean;
  seen: boolean;
  due: number;
}

/**
 * GlassTicker — one IntersectionObserver and one timer for every animated
 * glass panel and hub on the page.
 *
 * A guide can hold twenty-odd panels. Each would otherwise own an observer
 * and an interval; here they register, and a single timeout is armed for
 * whichever visible panel is due next. Nothing runs while no panel is on
 * screen, while the tab is hidden, or when the reader prefers reduced motion
 * (in which case register() does nothing at all and every panel stays in the
 * settled state it was prerendered in).
 *
 * Browser only: call register() from afterNextRender, and call the function
 * it returns when the component is destroyed.
 */
@Injectable({ providedIn: 'root' })
export class GlassTicker {
  private readonly doc = inject(DOCUMENT);
  private readonly entries = new Map<Element, Entry>();
  private io: IntersectionObserver | null = null;
  private timer: ReturnType<typeof setTimeout> | null = null;
  private listening = false;

  private readonly onVisibility = () => {
    if (this.doc.hidden) { this.stop(); return; }
    const now = Date.now();
    for (const entry of this.entries.values()) if (entry.on) entry.due = now + entry.client.period;
    this.schedule();
  };

  /** True when the reader has asked for reduced motion (or there is no way to observe). */
  get still(): boolean {
    const view = this.doc.defaultView;
    return !view || !('IntersectionObserver' in view) ||
      view.matchMedia('(prefers-reduced-motion: reduce)').matches;
  }

  register(client: GlassClient): () => void {
    if (this.still) return () => { /* nothing was started */ };

    this.io ??= new IntersectionObserver((changes) => this.onIntersect(changes), { threshold: 0.15 });
    if (!this.listening) {
      this.doc.addEventListener('visibilitychange', this.onVisibility);
      this.listening = true;
    }
    this.entries.set(client.el, { client, on: false, seen: false, due: 0 });
    this.io.observe(client.el);

    return () => {
      this.io?.unobserve(client.el);
      this.entries.delete(client.el);
      if (this.entries.size) { this.schedule(); return; }
      this.stop();
      this.io?.disconnect();
      this.io = null;
      this.doc.removeEventListener('visibilitychange', this.onVisibility);
      this.listening = false;
    };
  }

  private onIntersect(changes: IntersectionObserverEntry[]): void {
    const now = Date.now();
    for (const change of changes) {
      const entry = this.entries.get(change.target);
      if (!entry || entry.on === change.isIntersecting) continue;
      entry.on = change.isIntersecting;
      const first = entry.on && !entry.seen;
      if (entry.on) {
        entry.seen = true;
        entry.due = now + (first ? entry.client.delay ?? entry.client.period : entry.client.period);
      }
      entry.client.visible?.(entry.on, first);
    }
    this.schedule();
  }

  /** Arms one timeout for the visible client that is due soonest. */
  private schedule(): void {
    this.stop();
    if (this.doc.hidden) return;
    let next = Infinity;
    for (const entry of this.entries.values()) if (entry.on && entry.due < next) next = entry.due;
    if (next === Infinity) return;
    this.timer = setTimeout(() => this.run(), Math.max(0, next - Date.now()));
  }

  private run(): void {
    this.timer = null;
    const now = Date.now();
    for (const entry of this.entries.values()) {
      if (!entry.on || entry.due > now + 8) continue;
      entry.due = now + entry.client.period;
      entry.client.tick();
    }
    this.schedule();
  }

  private stop(): void {
    if (this.timer) clearTimeout(this.timer);
    this.timer = null;
  }
}
