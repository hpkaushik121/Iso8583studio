import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EXTERNAL } from '../core/site-nav';
import { RELEASE } from '../core/release';
import { UiIcon } from './icon';

/**
 * Provenance strip: which release is current and where the source is. The
 * two facts a buyer, a reviewer or a bank's vendor-risk team looks for first,
 * so they sit under the hero CTAs of the Pro and HSM pages rather than only
 * on the versions page and in the footer.
 *
 * Two anchors on one hairline row, in the mono the hero meta lines use. Each
 * is a real link (the versions page, the repository), so the click listener
 * reports it. The facts come from core/release.ts and core/site-nav.ts; the
 * strip never carries its own copy of a version.
 *
 * Reveal belongs to the parent: give the host .ds-fade and a --d like any
 * other hero line, or .ds-hold inside a uiReveal block.
 *
 *   <ui-provenance class="ds-fade" [style.--d]="700" />
 */
@Component({
  selector: 'ui-provenance',
  imports: [RouterLink, UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'provenance-host', '[class.provenance-host--center]': "align() === 'center'" },
  template: `
    <div class="provenance" role="group" aria-label="Release and source">
      <a class="provenance-item" routerLink="/docs/versions">
        <ui-icon name="tag" [size]="14" />
        <span class="provenance-k">Release</span>
        <span class="provenance-v">v{{ release.version }}<span class="provenance-dim"> · {{ release.dateLabel }}</span></span>
      </a>
      <span class="provenance-rule" aria-hidden="true"></span>
      <a class="provenance-item" [href]="repo" target="_blank" rel="noopener">
        <ui-icon name="github-logo" [size]="14" />
        <span class="provenance-k">Source</span>
        <span class="provenance-v">GitHub<span class="provenance-dim"> · {{ release.license }}</span></span>
      </a>
    </div>
  `,
})
export class UiProvenance {
  /** 'center' under centred hero copy; 'start' (default) under left-aligned copy. */
  readonly align = input<'start' | 'center'>('start');

  protected readonly release = RELEASE;
  protected readonly repo = EXTERNAL.repo;
}
