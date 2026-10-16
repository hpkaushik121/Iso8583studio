import {
  ChangeDetectionStrategy, Component, DestroyRef, ElementRef, PLATFORM_ID, afterNextRender,
  inject, viewChild,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterLink } from '@angular/router';
import { UiIcon } from '../../ui';
import { JOURNEY_STAGES } from './journey.data';
import type { JourneyHandle } from './journey/index';

/** How far below the viewport the section may be when three.js starts loading. */
const LOAD_MARGIN = '600px 0px';

/**
 * The scroll-driven payment journey: eight stages, each pairing copy with a 3D
 * scene.
 *
 * The prerendered page carries every stage's copy and a flat plate where the
 * scene goes. In the browser, once the section is within LOAD_MARGIN of the
 * viewport, the three.js engine is fetched as its own chunk and takes the
 * plates' place — unless WebGL is missing or the reader asked for reduced
 * motion, in which case the plates stay.
 *
 * Hooks analytics depends on: the section is #flowRail, and every stage is a
 * .node holding its title in an <h3> (flow_node_click). Stages are <div>s, not
 * <section>s, so the section funnel counts the journey once.
 */
@Component({
  selector: 'home-journey',
  imports: [RouterLink, UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section #root id="flowRail" class="payment-story" data-sect="transaction_path"
             aria-labelledby="journey-heading-01">
      <div class="hj-wash" aria-hidden="true"></div>
      <div class="hj-top-fade" aria-hidden="true"></div>
      <div class="story-stages">
        <svg class="journey-trail" aria-hidden="true">
          <defs>
            <linearGradient id="journey-flow" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0" class="jf-blue" stop-opacity="0" />
              <stop offset=".06" class="jf-blue" stop-opacity=".55" />
              <stop offset=".18" class="jf-blue" />
              <stop offset=".7" class="jf-deep" />
              <stop offset=".8" class="jf-teal" />
              <stop offset=".95" class="jf-teal" />
              <stop offset="1" class="jf-teal" stop-opacity="0" />
            </linearGradient>
            <filter id="journey-glow" x="-200%" y="-200%" width="500%" height="500%">
              <feGaussianBlur stdDeviation="5" />
            </filter>
          </defs>
          <path class="trail-halo" fill="none" stroke="url(#journey-flow)" stroke-width="7" opacity=".22" filter="url(#journey-glow)" />
          <path class="trail-line" fill="none" stroke="url(#journey-flow)" stroke-width="1.6" opacity=".45" />
          <path class="trail-progress" fill="none" stroke="url(#journey-flow)" stroke-width="2.5" opacity=".8" />
          <circle class="trail-packet-halo" r="12" opacity=".45" filter="url(#journey-glow)" />
          <circle class="trail-packet" r="3" />
        </svg>

        @for (row of stages; track row.n; let i = $index) {
          <div class="story-stage node" [class]="row.scene" [class.is-tall]="row.tall"
               [attr.data-stage]="i" [attr.data-align]="row.align">
            <span class="stage-num" aria-hidden="true">{{ row.n }}</span>

            <div class="stage-copy">
              @if (row.lead) { <p class="stage-eyebrow stage-lead">{{ row.lead }}</p> }
              <p class="stage-eyebrow">{{ row.n }} / <span>{{ row.label }}</span></p>
              <h3 [id]="'journey-heading-' + row.n">{{ row.title[0] }}@if (row.title[1]) {<br class="stage-br"> {{ row.title[1] }}}</h3>
              @if (row.desc) { <p class="stage-desc">{{ row.desc }}</p> }
              @if (row.link) {
                <a class="stage-link" [routerLink]="row.link">Learn more<ui-icon name="arrow-up-right" [size]="16" /></a>
              }
            </div>

            <div class="scene-art">
              <div class="scene-view" [class.has-messages]="!!row.messages" role="img" [attr.aria-label]="row.alt">
                <div class="model-fallback">
                  <strong>{{ row.fallback[0] }}</strong>
                  <span>{{ row.fallback[1] }}</span>
                  <small>{{ row.fallback[2] }}</small>
                </div>
                <canvas></canvas>
                @if (row.handshake) {
                  <div class="handshake"><span>Insert test card</span><b>✓</b></div>
                }
              </div>

              @if (row.messages; as messages) {
                <svg class="message-threads" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">
                  <path d="M43 42 Q53 31 70 23 M44 43 Q58 40 70 39 M44 44 Q59 48 70 54" fill="none" stroke-width=".2" opacity=".65" />
                </svg>
                <div class="message-stack">
                  @for (m of messages; track m.name) {
                    <div class="hj-glass hj-message">
                      <span class="hj-glyph" aria-hidden="true">{{ m.glyph }}</span>
                      <span><b>{{ m.name }}</b>@if (m.note) {<small>{{ m.note }}</small>}</span>
                    </div>
                  }
                </div>
              }

              @if (row.response; as r) {
                <div class="hj-glass response-panel">
                  <div>
                    <b class="hj-mti">{{ r.mti }}</b>
                    <strong>{{ r.label }} <em>{{ r.code }}</em></strong>
                  </div>
                  <b class="hj-tick" aria-hidden="true">✓</b>
                  <small>{{ r.note }}</small>
                </div>
              }

              @if (row.labels; as labels) {
                <div class="stage-labels">
                  @for (l of labels; track l.text) {
                    <div class="hj-glass hj-label" [class.verified]="l.place === 'verified'" [attr.data-place]="l.place">
                      @if (l.place === 'verified') {
                        <b class="hj-tick" aria-hidden="true">✓</b>
                      } @else {
                        <ui-icon [name]="l.icon" />
                      }
                      <span>{{ l.text }}@if (l.note) {<small>{{ l.note }}</small>}</span>
                    </div>
                  }
                </div>
              }

              @if (row.end; as end) {
                <div class="stage-end">
                  <p>{{ end.line }}</p>
                  <a class="btn btn--outline" [routerLink]="end.link">{{ end.cta }}<ui-icon name="arrow-up-right" [size]="16" /></a>
                </div>
              }

              @if (row.note) { <small class="stage-note">{{ row.note }}</small> }

              @if (row.panel; as panel) {
                <div class="hj-glass request-panel">
                  <div class="hj-panel-head">{{ panel.title }}<span aria-hidden="true">›</span></div>
                  <dl>
                    @for (kv of panel.rows; track kv[0]) { <dt>{{ kv[0] }}</dt><dd>{{ kv[1] }}</dd> }
                  </dl>
                </div>
              }
            </div>
          </div>
        }
      </div>
    </section>
  `,
})
export class HomeJourney {
  protected readonly stages = JOURNEY_STAGES;

  private readonly root = viewChild.required<ElementRef<HTMLElement>>('root');

  constructor() {
    if (!isPlatformBrowser(inject(PLATFORM_ID))) return;
    const destroyRef = inject(DestroyRef);

    afterNextRender(() => {
      const section = this.root().nativeElement;
      if (matchMedia('(prefers-reduced-motion: reduce)').matches) return;
      if (!('IntersectionObserver' in window) || !hasWebGL()) return;

      let handle: JourneyHandle | null = null;
      let dead = false;

      const io = new IntersectionObserver((entries) => {
        if (!entries.some((e) => e.isIntersecting)) return;
        io.disconnect();
        import('./journey/index')
          // Scene construction is a ~1.5 s main-thread task on a 4x-throttled
          // phone. Running it inside the scroll handler's frame meant any tap
          // landing in that window inherited the delay; an idle slot lets the
          // interaction that woke the observer finish first.
          .then((m) => new Promise<JourneyHandle>((resolve, reject) => {
            const go = () => { try { resolve(m.mountJourney(section)); } catch (e) { reject(e); } };
            if ('requestIdleCallback' in window) requestIdleCallback(go, { timeout: 1500 });
            else setTimeout(go, 0);
          }))
          .then((h) => { if (dead) h.destroy(); else handle = h; })
          .catch((err) => {
            // The plates are still in place, so a failed load costs nothing visible.
            console.warn('Payment journey: 3D scenes failed to load', err);
            section.classList.remove('journey-live');
          });
      }, { rootMargin: LOAD_MARGIN });

      // On a tall window the section is already within the margin at load, and
      // observing straight away would pull three.js in for a visitor who never
      // leaves the hero. So the watch starts with the first sign the reader is
      // moving down the page — or at once if the page opened already scrolled.
      const WAKE = ['scroll', 'wheel', 'touchstart', 'keydown'] as const;
      const arm = () => {
        WAKE.forEach((type) => window.removeEventListener(type, arm));
        if (!dead) io.observe(section);
      };
      if (window.scrollY > 0) arm();
      else WAKE.forEach((type) => window.addEventListener(type, arm, { passive: true }));

      destroyRef.onDestroy(() => {
        dead = true;
        WAKE.forEach((type) => window.removeEventListener(type, arm));
        io.disconnect();
        handle?.destroy();
        handle = null;
      });
    });
  }
}

/** Probes for a WebGL context without keeping it. */
function hasWebGL(): boolean {
  try {
    const canvas = document.createElement('canvas');
    const gl = (canvas.getContext('webgl2') ?? canvas.getContext('webgl')) as WebGLRenderingContext | null;
    if (!gl) return false;
    gl.getExtension('WEBGL_lose_context')?.loseContext();
    return true;
  } catch {
    return false;
  }
}
