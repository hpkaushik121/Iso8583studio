import {
  AfterViewInit, Directive, ElementRef, OnDestroy, PLATFORM_ID, inject,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { AnalyticsService } from '../../core/analytics';
import { PAGE_BEHAVIOURS } from '../behaviours';

/**
 * The behaviour StaticPage used to supply, as a host directive every site page
 * applies to itself.
 *
 * The pages are real components now, so their markup is compiled rather than
 * bound through [innerHTML]. What does not come free with that is the three
 * things the old renderer did to a freshly injected document: reveal-on-scroll,
 * the section funnel, and handing internal links to the router. Those are
 * behaviour rather than markup, so they live here and are attached with
 * hostDirectives — no base class, and nothing for a generated component to
 * remember to call.
 *
 * The page class is read off the host rather than passed in: it is already
 * declared there for the page's scoped stylesheet, and a second copy in an
 * input is a second thing to keep in sync.
 */
@Directive({
  selector: '[sitePage]',
  host: { '(click)': 'onClick($event)' },
})
export class SitePage implements AfterViewInit, OnDestroy {
  private readonly el = inject(ElementRef<HTMLElement>);
  private readonly router = inject(Router);
  private readonly analytics = inject(AnalyticsService);
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
  private teardown: (() => void)[] = [];

  ngAfterViewInit(): void {
    if (!this.isBrowser) return;
    const root = this.el.nativeElement as HTMLElement;
    this.revealSections(root);
    this.observeSections(root);

    const pageClass = [...root.classList].find((c) => c.startsWith('page-'));
    const behaviour = pageClass ? PAGE_BEHAVIOURS[pageClass] : undefined;
    if (behaviour) this.teardown.push(behaviour(root, this.analytics));
  }

  ngOnDestroy(): void {
    this.teardown.forEach((fn) => fn());
    this.teardown = [];
  }

  /**
   * Internal links are plain hrefs in the page markup, so a click would reload
   * the document. Handing them to the router keeps the navigation client-side
   * while the markup stays readable, and leaves anchors, new-tab clicks and
   * modified clicks to the browser.
   */
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
