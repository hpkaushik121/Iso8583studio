import { ResolveFn } from '@angular/router';

/** Loads an imported page's content module before the route activates, so the
 *  prerenderer serialises a complete document. */
export const pageResolver: ResolveFn<string> = async (route) => {
  const load = route.data['load'] as (() => Promise<{ html: string }>) | undefined;
  if (!load) return '';
  return (await load()).html;
};
