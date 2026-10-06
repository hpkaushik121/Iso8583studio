/**
 * Glass panel data model — the specs the animated tool panels render from,
 * and the helpers the tool-guide data files build them with.
 *
 * The illustrative hex in a panel is generated, not typed out: hx() is a
 * seeded generator, so the same label always yields the same digits and the
 * server and the browser render identical text. Nothing here may call
 * Math.random() or read the clock.
 */

export type GlassFieldKind = 'hex' | 'text' | 'area' | 'sel' | 'check';

/** One form field of a panel. */
export interface GlassField {
  /** Label. */
  l: string;
  /** The value it fills with. */
  v: string;
  kind: GlassFieldKind;
  /** Character count shown in the field and the log (hex/text/area). */
  n?: number;
  /** Columns it spans on the 6-column form grid. */
  w: number;
  /** EMV tag (or similar) shown beside the label. */
  tag?: string;
  /** Marks the field "optional". */
  opt?: boolean;
}

/** One tool screen: its form, the button it runs and the result lines it logs. */
export interface GlassSpec {
  title: string;
  sub: string;
  /** Path label at the top right ("emv-tools"); falls back to the panel's `app` input. */
  app?: string;
  /** Pill at the top right ("EMV"); falls back to the panel's `badge` input. */
  badge?: string;
  tabs?: string[] | null;
  /** Index of the active tab; earlier tabs are ticked. */
  tab?: number;
  /** Phosphor icon on the run button (default 'play'). */
  icon?: string;
  hint?: string;
  fields: GlassField[];
  /** ISO 8583 field numbers to light in the bitmap grids (Bitmap Calculator only). */
  grid?: number[];
  banner?: { tone: 'warn'; text: string };
  button: string;
  /** Second, ghost button. */
  alt?: string;
  /** [label, value] lines written to the log when the button runs. */
  result: [string, string][];
}

/** A tool tile in the hub. `stage` indexes the hub's stages. */
export interface HubTool {
  id: string;
  stage: number;
  icon: string;
  name: string;
  desc: string;
}

/** [code, name, sub] — e.g. ['01', 'Offline authentication', 'SDA · DDA']. */
export type HubStage = [string, string, string];

/**
 * Deterministic pseudo-hex: FNV-1a over the seed, then a linear congruential
 * generator, six hex digits at a time. Same seed and length, same string —
 * on the server and in the browser.
 */
export function hx(seed: string, n: number): string {
  let h = 2166136261;
  for (const c of seed) {
    h ^= c.charCodeAt(0);
    h = Math.imul(h, 16777619) >>> 0;
  }
  let s = '';
  while (s.length < n) {
    h = (Math.imul(h, 1103515245) + 12345) >>> 0;
    s += (h >>> 8).toString(16).toUpperCase().padStart(6, '0');
  }
  return s.slice(0, n);
}

/** Groups a hex string in fours: 'A1B2C3D4' → 'A1B2 C3D4'. */
export const grp = (s: string): string => s.replace(/(.{4})/g, '$1 ').trim();

type FieldOptions = Partial<GlassField>;

/**
 * Hex field of `n` characters. The value is generated from the label and
 * grouped in fours; a value passed in `o.v` is shown exactly as written.
 */
export const H = (l: string, n: number, o: FieldOptions = {}): GlassField => ({
  l, n, kind: 'hex',
  v: grp(hx(l + '/' + n, n)),
  w: n >= 32 ? 6 : n >= 8 ? 3 : 2,
  ...o,
});

/** Text field. */
export const T = (l: string, v: string, o: FieldOptions = {}): GlassField =>
  ({ l, v, kind: 'text', n: v.replace(/\s/g, '').length, w: 3, ...o });

/** Multi-line text area. */
export const A = (l: string, v: string, o: FieldOptions = {}): GlassField =>
  ({ l, v, kind: 'area', n: v.replace(/\s/g, '').length, w: 6, ...o });

/** Drop-down. */
export const S = (l: string, v: string, o: FieldOptions = {}): GlassField =>
  ({ l, v, kind: 'sel', w: 3, ...o });

/** Checkbox. */
export const C = (l: string, v: string, o: FieldOptions = {}): GlassField =>
  ({ l, v, kind: 'check', w: 2, ...o });
