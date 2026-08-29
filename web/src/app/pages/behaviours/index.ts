import type { PageBehaviour } from './types';
import { homeDemos } from './home-demos';

/** Keyed by the page class the importer assigns (see tools/import-pages.mjs). */
export const PAGE_BEHAVIOURS: Record<string, PageBehaviour> = {
  'page-home': homeDemos,
};

export type { PageBehaviour };
