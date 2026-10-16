/**
 * Shared shapes for the screenshot-based tool references (DUKPT, MAC and Card
 * Validation). Those three pages have no glass panels: they show the app's own
 * screenshots, so their one custom block is a framed figure.
 *
 * Imported by pages/data/{dukpt,mac,card-validation}-tools.data.ts and the
 * three pages that render them.
 */
import type { GuideBlock, GuideRailItem } from '../shared/guide';

/** A documentation screenshot: the WebP under /media/docs and its real pixel size. */
export interface RefShot {
  src: string;
  alt: string;
  /** Pixel size of the WebP file. */
  w: number;
  h: number;
  /** Widest the figure is drawn, in CSS px (the window's size on screen). */
  max: number;
  /** Text after the page slug in the caption. */
  caption: string;
}

/** A figure block. `side` puts a narrow screenshot beside the blocks that describe it. */
export interface RefFigure {
  shot: RefShot;
  side?: GuideBlock[];
}

export type RefBlock = GuideBlock<RefFigure>;

export interface RefSection {
  /** The section's id — what the rail links to and section_view reports. */
  id: string;
  /** The h2. */
  title: string;
  /** Shorter label for the rail. */
  rail: string;
  /** Rail icon. */
  icon: string;
  /** Mono line above the heading. */
  eyebrow?: string;
  /** Rich lede. */
  intro?: string;
  blocks?: RefBlock[];
  /** Rich. Drawn as the numbered tips card instead of blocks. */
  tips?: string[];
}

export interface RefGuide {
  /** The app's name for the group, shown in captions: 'dukpt-tools'. */
  slug: string;
  /** Last breadcrumb item. */
  crumb: string;
  /** Mono line above the h1. */
  meta: string;
  title: string;
  lede: string;
  /** Id of the section the hero's second CTA jumps to. */
  browse: string;
  sections: RefSection[];
  cta: { heading: string; text: string };
}

const SHOTS = '/media/docs/payment-utilities/';

/** A figure block for `<name>.webp` in the payment-utilities folder. */
export const shot = (
  name: string, w: number, h: number, max: number, caption: string, alt: string, side?: GuideBlock[],
): RefBlock => ({ t: 'custom', data: { shot: { src: `${SHOTS}${name}.webp`, alt, w, h, max, caption }, side } });

/** The rail of a reference page: its sections, then the closing download panel. */
export const railOf = (guide: RefGuide): GuideRailItem[] => [
  ...guide.sections.map((s) => ({ id: s.id, label: s.rail, icon: s.icon })),
  { id: 'download', label: 'Try it', icon: 'download-simple' },
];
