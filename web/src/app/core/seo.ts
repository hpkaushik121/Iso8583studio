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

export const PUBLISHER = {
  '@type': 'Organization',
  '@id': `${SITE_ORIGIN}/#organization`,
  name: 'AiCortex Solutions',
  url: `${SITE_ORIGIN}/`,
  logo: { '@type': 'ImageObject', url: `${SITE_ORIGIN}/images/app.png`, width: 512, height: 512 },
  sameAs: [
    'https://github.com/hpkaushik121/Iso8583studio',
    'https://www.linkedin.com/company/iso8583-studio',
    'https://medium.com/@iso8583.studio',
  ],
};

/** Visible breadcrumb labels, keyed by path. Every simulator, tool and docs
 *  page renders `Home / Documentation / <label>`; top-level pages render
 *  `Home / <label>`. Blog posts build their own trail in build-blog-routes. */
const CRUMB_LABELS: Record<string, string> = {
  '/blogs': 'Blog', '/cloud-simulators': 'Cloud Simulators', '/contact': 'Contact',
  '/docs': 'Documentation', '/download': 'Download', '/emv-certification': 'EMV Certification',
  '/kernel': 'Kernel Development', '/middleware': 'Payment Middleware', '/privacy-policy': 'Privacy Policy',
  '/pro': 'Pro', '/terms-and-conditions': 'Terms and Conditions',
  '/docs/contributing': 'How to Contribute', '/docs/installation': 'Installation', '/docs/versions': 'Versions',
  '/simulator': 'Payment Simulators', '/simulator/apdu': 'APDU Simulator', '/simulator/atm': 'ATM Simulator',
  '/simulator/ecr': 'ECR Simulator', '/simulator/host': 'Host Simulator',
  '/simulator/hsm-command-console': 'HSM Command Console', '/simulator/hsm': 'HSM Simulator',
  '/simulator/issuer': 'Issuer System', '/simulator/payment-switch': 'Switch Simulator', '/simulator/pos': 'POS Simulator',
  '/tools/card-validation': 'Card Validation', '/tools/cipher-tools': 'Cryptographic Tools',
  '/tools/dukpt-tools': 'DUKPT Tools', '/tools/emv-tools': 'EMV Tools', '/tools/key-tools': 'Key Management Tools',
  '/tools/mac-tools': 'MAC Tools', '/tools/pin-tools': 'Payment Utilities', '/tools/utility-tools': 'Data Converters',
};

const DOC_PREFIX = /^\/(simulator|tools|docs)\//;
const SERVICE_PATHS = new Set(['/emv-certification', '/middleware', '/kernel', '/cloud-simulators']);

function breadcrumbList(path: string): unknown | undefined {
  if (path === '/' || path.startsWith('/blogs/')) return undefined;
  const label = CRUMB_LABELS[path];
  if (!label) return undefined;
  const trail: { name: string; path: string }[] = [{ name: 'Home', path: '/' }];
  if (DOC_PREFIX.test(path) || path === '/simulator') trail.push({ name: 'Documentation', path: '/docs' });
  trail.push({ name: label, path });
  return {
    '@context': 'https://schema.org',
    '@type': 'BreadcrumbList',
    itemListElement: trail.map((c, i) => ({
      '@type': 'ListItem', position: i + 1, name: c.name, item: canonicalUrl(c.path),
    })),
  };
}

/** A sensible block for routes that declare no `jsonLd` of their own: the
 *  simulator, tool and docs references are TechArticles; the four
 *  consulting pages are Services. Routes that set `jsonLd` keep it. */
function defaultJsonLd(seo: PageSeo): unknown | undefined {
  const url = canonicalUrl(seo.path);
  if (DOC_PREFIX.test(seo.path)) {
    return {
      '@context': 'https://schema.org',
      '@type': 'TechArticle',
      headline: seo.title.replace(/\s*[-|]\s*ISO8583Studio.*$/, ''),
      description: seo.description,
      url,
      mainEntityOfPage: { '@type': 'WebPage', '@id': url },
      inLanguage: 'en',
      image: seo.image ?? DEFAULT_OG_IMAGE,
      author: PUBLISHER,
      publisher: PUBLISHER,
    };
  }
  if (SERVICE_PATHS.has(seo.path)) {
    return {
      '@context': 'https://schema.org',
      '@type': 'Service',
      name: seo.title.replace(/\s*[-|—]\s*ISO8583Studio.*$/, ''),
      description: seo.description,
      url,
      provider: PUBLISHER,
      areaServed: 'Worldwide',
    };
  }
  return undefined;
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
    // Large image previews on every indexable page; routes that spell out
    // 'index, follow' themselves still get it.
    const robots = seo.robots ?? 'index, follow';
    this.setName('robots', /noindex|max-image-preview/.test(robots) ? robots : `${robots}, max-image-preview:large`);
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
    const own = seo.jsonLd === undefined ? [] : Array.isArray(seo.jsonLd) ? seo.jsonLd : [seo.jsonLd];
    const fallback = own.length ? [] : [defaultJsonLd(seo)];
    const blocks = [...own, ...fallback, breadcrumbList(seo.path)].filter((b) => b !== undefined);
    this.setJsonLd(blocks.length ? blocks : undefined);
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
