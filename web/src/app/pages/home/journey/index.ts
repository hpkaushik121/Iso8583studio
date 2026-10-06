/**
 * Typed entry to the three.js payment-journey engine. Everything behind it is
 * a mechanical port of the design system's templates/_journey/*.js and is
 * loaded only through the dynamic import in ../journey.ts, so three.js is a
 * lazy chunk that never runs during prerender.
 */
import { mountStory } from './story-engine';

export interface JourneyHandle {
  readonly ready: boolean;
  readonly paused: boolean;
  destroy(): void;
}

/** Mounts the 3D scenes on the journey section. Browser only. */
export function mountJourney(root: HTMLElement): Promise<JourneyHandle> {
  return mountStory(root) as Promise<JourneyHandle>;
}
