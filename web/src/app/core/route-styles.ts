import { DOCUMENT, isPlatformServer } from '@angular/common';
import { PLATFORM_ID, inject } from '@angular/core';
import { ResolveFn } from '@angular/router';

/**
 * The per-family stylesheets built from src/styles/bundles/. Each is emitted
 * as /<name>.css and is not in the initial bundle — a page loads only the
 * families it uses.
 */
export type StyleBundle =
  | 'home' | 'solutions' | 'guide' | 'tools' | 'sims' | 'preview'
  | 'longform' | 'docs' | 'blog' | 'pro';

/** Longest a navigation waits for a stylesheet before showing the page anyway. */
const LOAD_TIMEOUT_MS = 4000;

function ensure(doc: Document, name: StyleBundle, server: boolean): Promise<void> {
  const id = `rs-${name}`;
  if (doc.getElementById(id)) return Promise.resolve();

  const link = doc.createElement('link');
  link.id = id;
  link.rel = 'stylesheet';
  link.href = `/${name}.css`;
  doc.head.appendChild(link);

  // The prerender only has to leave the tag in the document.
  if (server) return Promise.resolve();

  return new Promise<void>((resolve) => {
    const done = () => { clearTimeout(timer); resolve(); };
    const timer = setTimeout(done, LOAD_TIMEOUT_MS);
    link.addEventListener('load', done, { once: true });
    link.addEventListener('error', done, { once: true });
  });
}

/**
 * Route resolver that makes sure a page's stylesheets are in the document
 * before the page renders.
 *
 * During prerender it writes the <link> into the head, so the static file
 * asks for its styles with the HTML. In the browser it reuses that tag, and on
 * a client-side navigation to a family not yet loaded it adds the tag and
 * waits for it, so the new page never paints unstyled.
 *
 * Bundles stay loaded once added, so their rules must be namespaced to their
 * own pages.
 */
export function styleBundles(...names: StyleBundle[]): ResolveFn<true> {
  return () => {
    const doc = inject(DOCUMENT);
    const server = isPlatformServer(inject(PLATFORM_ID));
    return Promise.all(names.map((name) => ensure(doc, name, server))).then(() => true as const);
  };
}
