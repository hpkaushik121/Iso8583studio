import { Crumb } from '../../../ui';
import { GuideBlock, GuideCard, GuideFeature, GuideRailItem } from '../guide';

/**
 * The data a simulator guide page (HSM Simulator, Host Simulator, HSM Command
 * Console) is rendered from, and the helpers its data file is written with.
 * `Id` is the page's glass-screen id type (HsmScreenId, HostScreenId, …).
 *
 * The three pages share this shape but each has its own template, so a page
 * only ships the glass screens it draws.
 */

/** A glass screen in a section body, with the text after the dash in its caption. */
export interface SimScreenRef<Id extends string> {
  kind: 'screen';
  id: Id;
  caption?: string;
}

/** One of the placeholder cards on the Host Simulator page. */
export interface SimPlaceholder {
  /** The token as typed: '[SV]'. */
  tok: string;
  /** Ink of the token: b blue · t teal · o amber. */
  tone: 'b' | 't' | 'o';
  name: string;
  desc: string;
  /** [what you type, what it becomes] */
  ex: [string, string][];
  note: string;
}

export interface SimPlaceholders {
  kind: 'phcards';
  items: SimPlaceholder[];
}

export type SimCustom<Id extends string> = SimScreenRef<Id> | SimPlaceholders;
export type SimBlock<Id extends string> = GuideBlock<SimCustom<Id>>;

/** Shorthand for a glass-screen block. */
export const screen = <Id extends string>(id: Id, caption?: string): SimBlock<Id> =>
  ({ t: 'custom', data: { kind: 'screen', id, caption } });

/** "01 · Configure" — the line above a section heading. */
export const stage = (stages: readonly (readonly string[])[], index: number): string =>
  `${stages[index][0]} · ${stages[index][1]}`;

export interface SimSection<Id extends string> {
  /** The section's id — what the rail links to and section_view reports. */
  id: string;
  title: string;
  /** Shorter label for the rail. */
  rail?: string;
  /** Rail icon. */
  icon: string;
  /** Mono line above the heading. */
  eyebrow?: string;
  /** Rich lede. */
  intro?: string;
  blocks: SimBlock<Id>[];
}

export interface SimGuideData<Id extends string> {
  /** The app's name for the simulator, used in captions: 'hsm-simulator'. */
  slug: string;
  /** Mono line above the h1. */
  meta: string;
  title: string;
  lede: string;
  /** Hero video: clip names under /media/video and the poster under /media/poster. */
  clips: string[];
  poster: string;
  /** Label of the hero's second button, which jumps to the cards section. */
  browse: string;
  intro: {
    eyebrow: string;
    /** Rich paragraphs. */
    paras: string[];
    screen: SimScreenRef<Id>;
    features: GuideFeature[];
  };
  /** The grid of jump cards (section id `commands`). */
  cards: { heading: string; rail: string; lede: string; more: string; items: GuideCard[] };
  sections: SimSection<Id>[];
  cta: { heading: string; text: string; secondaryLabel?: string; secondaryLink?: string };
}

/** Home → Documentation → this page. */
export const simCrumbs = (title: string): Crumb[] => [
  { label: 'Home', link: '/' },
  { label: 'Documentation', link: '/docs' },
  { label: title },
];

/** Overview, the cards section, every content section, then the download panel. */
export const simRail = <Id extends string>(guide: SimGuideData<Id>): GuideRailItem[] => [
  { id: 'overview', label: 'Overview', icon: 'book-open' },
  { id: 'commands', label: guide.cards.rail, icon: 'squares-four' },
  ...guide.sections.map((s) => ({ id: s.id, label: s.rail || s.title, icon: s.icon })),
  { id: 'download', label: 'Try it', icon: 'download-simple' },
];

/** "hsm-simulator · Logs — an NC diagnostic and …" */
export const simCaption = (slug: string, sub: string, caption?: string): string =>
  `${slug} · ${sub}${caption ? ' — ' + caption : ''}`;

/** [icon, name, desc] rows, as the data files write them, to GuideFeature objects. */
export const simFeatures = (rows: readonly (readonly [string, string, string])[]): GuideFeature[] =>
  rows.map(([icon, name, desc]) => ({ icon, name, desc }));
