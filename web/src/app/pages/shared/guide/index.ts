/**
 * The guide shell — everything a guide page (tool guide, simulator guide,
 * re-skinned docs page) is assembled from. Styles: styles/bundles/guide.css
 * (every class is prefixed gd-), loaded by any route whose styleBundles()
 * include 'guide'.
 *
 * Page skeleton:
 *
 *   <app-guide-hero …> CTAs, [gdHeroBg], [gdHeroVisual] </app-guide-hero>
 *   <app-guide-layout [rail]="rail" badge="…">
 *     <app-guide-section anchor="overview" heading="…" intro="…"> blocks </app-guide-section>
 *     …
 *     <app-guide-cta heading="…" text="…" />
 *   </app-guide-layout>
 *
 * Each file's header comment documents its inputs and slots.
 */
export * from './guide-rich';
export * from './guide-hero';
export * from './guide-layout';
export * from './guide-section';
export * from './guide-blocks';
export * from './guide-cards';
export * from './guide-cta';
