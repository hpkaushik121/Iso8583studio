import type { PageBehaviour } from './types';
import { homeEngagement } from './home-engagement';

/** Keyed by the page class each page component sets on its own host. */
export const PAGE_BEHAVIOURS: Record<string, PageBehaviour> = {
  'page-home': homeEngagement,
};

export type { PageBehaviour };
