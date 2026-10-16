import {
  ChangeDetectionStrategy, Component, DOCUMENT, ElementRef, OnDestroy, PLATFORM_ID,
  afterNextRender, inject, input, signal, viewChildren,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

/** Below this width the 480p encode is sharp enough and a third of the bytes. */
const SMALL_SCREEN = 860;

/**
 * The looping product video behind a guide hero.
 *
 * The prerendered page carries only the poster image. The video files are
 * attached in the browser, and only when the hero is actually on screen, the
 * reader has not asked for reduced motion, and the connection is not in
 * data-saver mode — so a visitor who never sees the hero, or cannot use the
 * motion, never downloads it.
 *
 * `clips` are names under /media/video (each exists as <name>-720.mp4 and
 * <name>-480.mp4). One clip loops; several play in sequence with a crossfade
 * and then start over. `poster` is a name under /media/poster.
 *
 * Styles: styles/bundles/_hero-video.css. The host fills its positioned
 * parent, so place it as the first child of the hero section.
 */
@Component({
  selector: 'app-hero-video',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'hero-video',
    '[class.hero-video--right]': "layout() === 'right'",
    '[style.--hv-focus]': 'focus()',
    'aria-hidden': 'true',
  },
  template: `
    <img class="hero-video-poster" alt="" decoding="async" fetchpriority="high"
         width="1280" height="720"
         [src]="'/media/poster/' + poster() + '-1280.webp'"
         [attr.srcset]="'/media/poster/' + poster() + '-640.webp 640w, /media/poster/' + poster() + '-1280.webp 1280w'"
         sizes="100vw">
    @for (clip of clips(); track clip; let i = $index) {
      <video #clip class="hero-video-clip" [class.on]="playing() === i" muted playsinline
             preload="none" disablepictureinpicture tabindex="-1" [loop]="clips().length === 1"></video>
    }
    <div class="hero-video-scrim"></div>
  `,
})
export class HeroVideo implements OnDestroy {
  readonly clips = input.required<string[]>();
  readonly poster = input.required<string>();
  /** 'cover' fills the hero; 'right' pins the clip to the right and fades its left edge. */
  readonly layout = input<'cover' | 'right'>('cover');
  /** object-position of the clip, e.g. '78% 50%'. */
  readonly focus = input<string | null>(null);

  /** Index of the clip on screen; -1 while only the poster shows. */
  protected readonly playing = signal(-1);

  private readonly videos = viewChildren<ElementRef<HTMLVideoElement>>('clip');
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly doc = inject(DOCUMENT);
  private teardown: (() => void)[] = [];
  private attached = false;
  private visible = false;

  constructor() {
    if (!isPlatformBrowser(inject(PLATFORM_ID))) return;
    afterNextRender(() => this.start());
  }

  private start(): void {
    const view = this.doc.defaultView;
    if (!view || !('IntersectionObserver' in view)) return;
    if (view.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    const connection = (view.navigator as Navigator & { connection?: { saveData?: boolean } }).connection;
    if (connection?.saveData) return;

    const io = new IntersectionObserver(([entry]) => {
      this.visible = entry.isIntersecting;
      if (this.visible) this.resume(view);
      else this.pauseAll();
    }, { threshold: 0.05 });
    io.observe(this.host.nativeElement);
    this.teardown.push(() => io.disconnect());

    const onVisibility = () => {
      if (this.doc.hidden) this.pauseAll();
      else if (this.visible) this.resume(view);
    };
    this.doc.addEventListener('visibilitychange', onVisibility);
    this.teardown.push(() => this.doc.removeEventListener('visibilitychange', onVisibility));
  }

  private elements(): HTMLVideoElement[] {
    return this.videos().map((ref) => ref.nativeElement);
  }

  /** First sight of the hero: give each clip its source, then play. */
  private resume(view: Window): void {
    const videos = this.elements();
    if (!videos.length) return;

    if (!this.attached) {
      this.attached = true;
      const size = view.innerWidth <= SMALL_SCREEN ? 480 : 720;
      videos.forEach((video, i) => {
        video.muted = true;
        video.src = `/media/video/${this.clips()[i]}-${size}.mp4`;
        // Only the first clip is needed straight away; the rest buffer while it plays.
        video.preload = i === 0 ? 'auto' : 'metadata';
        const onPlaying = () => { if (this.playing() < 0 && i === 0) this.playing.set(0); };
        video.addEventListener('playing', onPlaying, { once: true });
        if (videos.length > 1) {
          const onEnded = () => this.advance(i);
          video.addEventListener('ended', onEnded);
          this.teardown.push(() => video.removeEventListener('ended', onEnded));
        }
      });
    }
    this.play(videos[Math.max(this.playing(), 0)]);
  }

  private advance(from: number): void {
    const videos = this.elements();
    if (!this.visible) return;
    const next = (from + 1) % videos.length;
    videos[next].currentTime = 0;
    this.play(videos[next]);
    this.playing.set(next);
    // Let the crossfade finish before the outgoing clip rewinds.
    setTimeout(() => { videos[from].pause(); videos[from].currentTime = 0; }, 600);
  }

  private play(video: HTMLVideoElement | undefined): void {
    video?.play()?.catch(() => { /* autoplay refused: the poster stays */ });
  }

  private pauseAll(): void {
    // Optional call: the prerender destroys this component too, and its DOM
    // has no media elements, so a <video> there has no pause().
    this.elements().forEach((video) => video.pause?.());
  }

  ngOnDestroy(): void {
    this.teardown.forEach((fn) => fn());
    this.pauseAll();
  }
}
