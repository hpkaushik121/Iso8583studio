import { ResolveFn } from '@angular/router';
import type { PostContent } from './blog-meta';

/**
 * Loads a post's content module before the route activates.
 *
 * This has to be a resolver rather than a load inside the component: the
 * prerenderer serialises the DOM as soon as the route renders, so content
 * fetched afterwards would be missing from the emitted static file — exactly
 * the failure mode that makes SPAs invisible to crawlers.
 */
export const postResolver: ResolveFn<PostContent> = async (route) => {
  const load = route.data['load'] as (() => Promise<PostContent>) | undefined;
  if (!load) return { html: '', toc: [] };
  const { html, toc } = await load();
  return { html, toc };
};
