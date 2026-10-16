/**
 * The release facts a page states: the current version, when it shipped and
 * which version the installers are built from. One place, so the provenance
 * strip on the heroes, the versions page and the hero mockup cannot disagree.
 * Bump here with each release.
 */
export const RELEASE = {
  /** Current source release. */
  version: '1.0.14',
  /** ISO date of that release. */
  date: '2026-06-12',
  /** The same date as readers see it. */
  dateLabel: 'Jun 12, 2026',
  /** The version the Windows and macOS installers are built from. */
  installers: '1.0.0',
  license: 'AGPL v3',
  licenseUrl: 'https://github.com/hpkaushik121/Iso8583studio/blob/main/LICENSE',
} as const;
