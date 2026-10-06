import type { PageBehaviour } from './types';

/**
 * The two hover/touch engagement events the home page reports.
 *
 *   sim_board_engage  — first mouseenter or touchstart on each simulator mark
 *                       in the strip under the hero (#simGrid), with that
 *                       simulator's full name.
 *   flow_rail_engage  — first pointerenter or touchstart on any stage's scene
 *                       in the payment journey (#flowRail).
 *
 * AnalyticsService sends each of them once per page view (once per simulator
 * for the first), so the handlers here simply report every time.
 *
 * The strip is a marquee, so its marks are in the DOM twice. Only the first
 * copy carries .simtile and .st-name; a mark in the second copy reports the
 * name of the one it repeats.
 */
export const homeEngagement: PageBehaviour = (root, analytics) => {
  const cleanups: (() => void)[] = [];
  const on = (el: Element, type: string, fn: () => void, opts?: AddEventListenerOptions) => {
    el.addEventListener(type, fn, opts);
    cleanups.push(() => el.removeEventListener(type, fn, opts));
  };

  const grid = root.querySelector('#simGrid');
  if (grid) {
    const names = [...grid.querySelectorAll('.simtile .st-name')]
      .map((el) => el.textContent?.replace(/\s+/g, ' ').trim() ?? '');
    [...grid.querySelectorAll('.hp-mark')].forEach((mark, i) => {
      const name = names[i % names.length];
      if (!name) return;
      const report = () => analytics.reportBoardEngage(name);
      on(mark, 'mouseenter', report);
      on(mark, 'touchstart', report, { passive: true });
    });
  }

  const rail = root.querySelector('#flowRail');
  if (rail) {
    const report = () => analytics.reportRailEngage();
    for (const scene of rail.querySelectorAll('.story-stage .scene-art')) {
      on(scene, 'pointerenter', report);
      on(scene, 'touchstart', report, { passive: true });
    }
  }

  return () => cleanups.forEach((fn) => fn());
};
