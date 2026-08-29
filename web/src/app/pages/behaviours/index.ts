import type { PageBehaviour } from './types';
import { homeDemos } from './home-demos';

/** Keyed by the page class each page component sets on its own host. */
export const PAGE_BEHAVIOURS: Record<string, PageBehaviour> = {
  'page-home': homeDemos,
};

export type { PageBehaviour };
