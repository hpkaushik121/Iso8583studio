import { DOCUMENT, Injectable, inject } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { filter, map } from 'rxjs/operators';

export const SITE_ORIGIN = 'https://iso8583.studio';
export const SITE_NAME = 'ISO8583Studio';
export const DEFAULT_OG_IMAGE = `${SITE_ORIGIN}/images/app.png`;

/** Per-route SEO metadata, carried on the route's `data.seo`. */
export interface PageSeo {
  title: string;
  description?: string;
  keywords?: string;
  /** Canonical path, always extensionless and rooted, e.g. '/blogs/foo'. */
  path: string;
  ogType?: 'website' | 'article';
  image?: string;
  robots?: string;
  author?: string;
  /** Structured data. Injected as <script type="application/ld+json">, which
   *  a template cannot express — Angular strips <script> from templates. */
  jsonLd?: unknown | unknown[];
}

export function canonicalUrl(path: string): string {
  if (path === '/') return `${SITE_ORIGIN}/`;
  return SITE_ORIGIN + (path.startsWith('/') ? path : `/${path}`);
}

@Injectable({ providedIn: 'root' })
export class SeoService {
  private readonly doc = inject(DOCUMENT);
  private readonly title = inject(Title);
  private readonly meta = inject(Meta);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  /** Subscribes to navigation and applies each route's metadata. Runs on the
   *  server during prerender too, so the emitted static file is complete. */
  init(): void {
    this.router.events
      .pipe(
        filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        map(() => {
          let r = this.route;
          while (r.firstChild) r = r.firstChild;
          return r.snapshot.data['seo'] as PageSeo | undefined;
        }),
      )
      .subscribe((seo) => seo && this.apply(seo));
  }

  apply(seo: PageSeo): void {
    const url = canonicalUrl(seo.path);
    const image = seo.image ?? DEFAULT_OG_IMAGE;
    const description = seo.description ?? '';

    this.title.setTitle(seo.title);

    this.setName('description', description);
    this.setName('keywords', seo.keywords);
    this.setName('robots', seo.robots ?? 'index, follow');
    this.setName('author', seo.author);

    this.setProperty('og:type', seo.ogType ?? 'website');
    this.setProperty('og:url', url);
    this.setProperty('og:title', seo.title);
    this.setProperty('og:description', description);
    this.setProperty('og:image', image);
    this.setProperty('og:site_name', SITE_NAME);

    this.setName('twitter:card', 'summary_large_image');
    this.setName('twitter:title', seo.title);
    this.setName('twitter:description', description);
    this.setName('twitter:image', image);

    this.setCanonical(url);
    this.setJsonLd(seo.jsonLd);
  }

  private setName(name: string, content: string | undefined): void {
    if (content) this.meta.updateTag({ name, content });
    else this.meta.removeTag(`name='${name}'`);
  }

  private setProperty(property: string, content: string | undefined): void {
    if (content) this.meta.updateTag({ property, content });
    else this.meta.removeTag(`property='${property}'`);
  }

  private setCanonical(url: string): void {
    const head = this.doc.head;
    let link = head.querySelector<HTMLLinkElement>('link[rel="canonical"]');
    if (!link) {
      link = this.doc.createElement('link');
      link.setAttribute('rel', 'canonical');
      head.appendChild(link);
    }
    link.setAttribute('href', url);
  }

  private setJsonLd(data: unknown | unknown[] | undefined): void {
    const head = this.doc.head;
    head.querySelectorAll('script[data-seo-jsonld]').forEach((n) => n.remove());
    if (!data) return;
    for (const block of Array.isArray(data) ? data : [data]) {
      const script = this.doc.createElement('script');
      script.setAttribute('type', 'application/ld+json');
      script.setAttribute('data-seo-jsonld', '');
      // Closing-tag sequences would terminate the script element early.
      script.textContent = JSON.stringify(block).replace(/</g, '\\u003c');
      head.appendChild(script);
    }
  }
}
