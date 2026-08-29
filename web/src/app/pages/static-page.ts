import {
  ChangeDetectionStrategy, Component, ElementRef, OnDestroy, PLATFORM_ID,
  computed, effect, inject, viewChild,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { DomSanitizer } from '@angular/platform-browser';
import { AnalyticsService } from '../core/analytics';
import { PAGE_BEHAVIOURS } from './behaviours';

/**
 * Renders one of the imported pages.
 *
 * The body is bound with [innerHTML] rather than compiled as a template: these
 * are hand-authored documents, and re-parsing them as Angular templates would
 * fail on markup a browser accepts but the template compiler does not.
 * Internal links inside that HTML are handed to the router by the click
 * handler below, and any interactive behaviour the page had as an inline
 * script is looked up in PAGE_BEHAVIOURS and torn down on navigation.
 */
@Component({
  selector: 'app-static-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div #host class="static-page" [class]="pageClass()" [innerHTML]="body()"
                  (click)="onClick($event)"></div>`,
})
export class StaticPage implements OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly analytics = inject(AnalyticsService);
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
  private readonly host = viewChild<ElementRef<HTMLElement>>('host');

  private readonly data = toSignal(this.route.data, { requireSync: true });
  private teardown: (() => void)[] = [];

  /** Scopes this page's imported stylesheet; see tools/css-codemod.mjs. */
  protected readonly pageClass = computed(() => (this.data()['pageClass'] as string) ?? '');

  /** Build-time content from our own repository, never user input. Bypassing
   *  the sanitizer keeps heading ids, which in-page anchors depend on. */
  protected readonly body = computed(() =>
    this.sanitizer.bypassSecurityTrustHtml((this.data()['html'] as string) ?? ''),
  );

  constructor() {
    effect(() => {
      this.body();
      const cls = this.pageClass();
      if (!this.isBrowser) return;
      // Wait for [innerHTML] to be applied before touching the new DOM.
      queueMicrotask(() => this.attach(cls));
    });
  }

  ngOnDestroy(): void { this.detach(); }

  private attach(pageClass: string): void {
    this.detach();
    const root = this.host()?.nativeElement;
    if (!root) return;

    this.revealSections(root);
    this.observeSections(root);

    const behaviour = PAGE_BEHAVIOURS[pageClass];
    if (behaviour) this.teardown.push(behaviour(root, this.analytics));
  }

  private detach(): void {
    this.teardown.forEach((fn) => fn());
    this.teardown = [];
  }

  protected onClick(event: MouseEvent): void {
    if (event.defaultPrevented || event.button !== 0 || event.metaKey ||
        event.ctrlKey || event.shiftKey || event.altKey) return;

    const anchor = (event.target as Element | null)?.closest?.('a');
    const href = anchor?.getAttribute('href');
    if (!anchor || !href) return;
    if (anchor.hasAttribute('target') || !href.startsWith('/')) return;
    if (href.startsWith('/images/') || href.startsWith('/assets/')) return;

    event.preventDefault();
    void this.router.navigateByUrl(href);
  }

  /** Sections fade in as they scroll into view. */
  private revealSections(root: HTMLElement): void {
    const items = root.querySelectorAll<HTMLElement>('.reveal');
    if (!items.length) return;

    if (!('IntersectionObserver' in window)) {
      items.forEach((el) => el.classList.add('on'));
      return;
    }
    const io = new IntersectionObserver((entries) => {
      for (const entry of entries) {
        if (!entry.isIntersecting) continue;
        entry.target.classList.add('on');
        io.unobserve(entry.target);
      }
    }, { threshold: .14 });
    items.forEach((el) => io.observe(el));
    this.teardown.push(() => io.disconnect());
  }

  /**
   * Reports each section the first time it enters the viewport, giving a funnel
   * down the page. rootMargin rather than a ratio threshold, because doc
   * sections are routinely taller than the viewport and can never reach one.
   */
  private observeSections(root: HTMLElement): void {
    if (!('IntersectionObserver' in window)) return;
    const sections = [...root.querySelectorAll<HTMLElement>('[data-sect], section, .doc-section')];
    if (!sections.length) return;

    const nameOf = (el: HTMLElement, index: number) =>
      (el.getAttribute('data-sect') || el.id ||
        el.querySelector('.kicker')?.textContent?.trim() ||
        el.querySelector('h2')?.textContent?.trim() ||
        `section_${index + 1}`).slice(0, 100);

    const io = new IntersectionObserver((entries) => {
      for (const entry of entries) {
        if (!entry.isIntersecting) continue;
        const el = entry.target as HTMLElement;
        const index = sections.indexOf(el);
        io.unobserve(el);
        this.analytics.reportSectionView(nameOf(el, index), index + 1);
      }
    }, { threshold: 0, rootMargin: '0px 0px -20% 0px' });

    sections.forEach((el) => io.observe(el));
    this.teardown.push(() => io.disconnect());
  }
}
