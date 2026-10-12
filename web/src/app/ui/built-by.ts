import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AUTHOR, EXTERNAL } from '../core/site-nav';
import { RELEASE } from '../core/release';
import { UiIcon } from './icon';

/**
 * Provenance strip: who built the studio, which release is current and where
 * the source is. These are the three facts a buyer, a reviewer or a bank's
 * vendor-risk team looks for first, so they sit on the hero of the home, Pro
 * and HSM pages rather than only on the versions page and in the footer.
 *
 * Three anchors on one hairline row, in the mono the hero meta lines use.
 * Each is a real link (the author page, the versions page, the repository),
 * so the click listener reports it. The facts come from core/release.ts and
 * core/site-nav.ts; the strip never carries its own copy of a version or a
 * name.
 *
 * Reveal belongs to the parent: give the host .ds-fade and a --d like any
 * other hero line, or .ds-hold inside a uiReveal block.
 *
 *   <ui-built-by class="ds-fade" [style.--d]="1500" align="center" />
 */
@Component({
  selector: 'ui-built-by',
  imports: [RouterLink, UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'built-by-host', '[class.built-by-host--center]': "align() === 'center'" },
  template: `
    <div class="built-by" role="group" aria-label="Project provenance">
      <a class="built-by-item" [routerLink]="author.path">
        <ui-icon name="user-circle" [size]="14" />
        <span class="built-by-k">Built by</span>
        <span class="built-by-v">{{ author.name }}</span>
      </a>
      <span class="built-by-rule" aria-hidden="true"></span>
      <a class="built-by-item" routerLink="/docs/versions">
        <ui-icon name="tag" [size]="14" />
        <span class="built-by-k">Release</span>
        <span class="built-by-v">v{{ release.version }}<span class="built-by-dim"> · {{ release.dateLabel }}</span></span>
      </a>
      <span class="built-by-rule" aria-hidden="true"></span>
      <a class="built-by-item" [href]="repo" target="_blank" rel="noopener">
        <ui-icon name="github-logo" [size]="14" />
        <span class="built-by-k">Source</span>
        <span class="built-by-v">GitHub<span class="built-by-dim"> · {{ release.license }}</span></span>
      </a>
    </div>
  `,
})
export class UiBuiltBy {
  /** 'center' for a centred hero (home); 'start' for a left-aligned one. */
  readonly align = input<'start' | 'center'>('start');

  protected readonly author = AUTHOR;
  protected readonly release = RELEASE;
  protected readonly repo = EXTERNAL.repo;
}
