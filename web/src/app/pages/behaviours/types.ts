import type { AnalyticsService } from '../../core/analytics';

/**
 * Interactive behaviour for one imported page, applied after its HTML is in
 * the DOM. Returns a teardown that must stop every timer and observer it
 * started, because the router reuses this component across navigations.
 */
export type PageBehaviour = (root: HTMLElement, analytics: AnalyticsService) => () => void;
