import { ResolveFn } from '@angular/router';

/**
 * Loads a post's content module before the route activates.
 *
 * This has to be a resolver rather than a load inside the component: the
 * prerenderer serialises the DOM as soon as the route renders, so content
 * fetched afterwards would be missing from the emitted static file — exactly
 * the failure mode that makes SPAs invisible to crawlers.
 */
export const postResolver: ResolveFn<string> = async (route) => {
  const load = route.data['load'] as (() => Promise<{ html: string }>) | undefined;
  if (!load) return '';
  return (await load()).html;
};
