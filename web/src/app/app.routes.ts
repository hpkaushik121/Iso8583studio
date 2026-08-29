import { Routes } from '@angular/router';
import { pageRoutes } from './content/page-routes';
import { blogRoutes } from './content/blog-routes';
import { BlogIndex } from './pages/blog/blog-index';
import { PageSeo } from './core/seo';

const BLOG_INDEX_SEO: PageSeo = {
  title: 'Blog - ISO8583Studio | Payment Testing & Fintech Guides',
  description:
    'ISO8583Studio blog - Expert guides on ISO 8583 payment testing, HSM simulation, EMV tools, ' +
    'cryptography, PIN block operations, and fintech development.',
  keywords:
    'ISO 8583 blog, payment testing guides, HSM simulator tutorial, EMV tools guide, fintech blog, ' +
    'payment security, PIN block, DUKPT, MAC calculation',
  path: '/blogs',
  ogType: 'website',
  jsonLd: {
    '@context': 'https://schema.org',
    '@type': 'Blog',
    name: 'ISO8583Studio Blog',
    description:
      'Expert guides on payment testing, ISO 8583, HSM simulation, EMV tools, and fintech development.',
    url: 'https://iso8583.studio/blogs',
    publisher: { '@type': 'Organization', name: 'AiCortex Solutions', url: 'https://iso8583.studio/' },
  },
};

export const routes: Routes = [
  ...pageRoutes,
  { path: 'blogs', component: BlogIndex, data: { seo: BLOG_INDEX_SEO } },
  ...blogRoutes,
  {
    path: 'design-system',
    loadComponent: () => import('./pages/design-system/design-system').then((m) => m.DesignSystem),
    data: {
      seo: {
        title: 'Design System - ISO8583Studio',
        description: 'Every primitive in the ISO8583Studio design system, in every variant.',
        path: '/design-system',
        // Internal reference page: never indexed, never in the sitemap.
        robots: 'noindex, nofollow',
      } satisfies PageSeo,
    },
  },
  {
    // Prerendered so the deploy has a real 404.html to serve; noindex keeps it
    // out of the sitemap and out of search results.
    path: '404',
    loadComponent: () => import('./pages/not-found/not-found').then((m) => m.NotFound),
    data: {
      seo: {
        title: 'Page not found - ISO8583Studio',
        description: 'That page does not exist.',
        path: '/404',
        robots: 'noindex, nofollow',
      } satisfies PageSeo,
    },
  },
  {
    path: '**',
    loadComponent: () => import('./pages/not-found/not-found').then((m) => m.NotFound),
    data: {
      seo: {
        title: 'Page not found - ISO8583Studio',
        description: 'That page does not exist.',
        path: '/404',
        robots: 'noindex, nofollow',
      } satisfies PageSeo,
    },
  },
];
