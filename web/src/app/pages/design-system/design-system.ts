import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import {
  UiBadge, UiBreadcrumb, UiButton, UiCallout, UiCard, UiDialog, UiFigure,
  UiIconTile, UiKicker, UiPageHeader, UiSection, UiSpecRow, UiTable, UiTag,
} from '../../ui';

interface Swatch { name: string; token: string; }

/**
 * Living styleguide: every primitive in every variant, on one page. Not
 * indexed and not in the sitemap — it exists so the system stays visible as
 * pages get added.
 */
@Component({
  selector: 'app-design-system',
  imports: [
    UiBadge, UiBreadcrumb, UiButton, UiCallout, UiCard, UiDialog, UiFigure,
    UiIconTile, UiKicker, UiPageHeader, UiSection, UiSpecRow, UiTable, UiTag,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`
    .ds-swatches { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: var(--sp-3); }
    .ds-swatch { border: 1px solid var(--line); border-radius: var(--r-md); overflow: hidden; }
    .ds-chip { height: 56px; }
    .ds-label { padding: var(--sp-2) var(--sp-3); font-family: var(--mono); font-size: var(--fs-xs); color: var(--muted); }
    .ds-row { display: flex; gap: var(--sp-3); flex-wrap: wrap; align-items: center; margin-bottom: var(--sp-4); }
    .ds-scale > * { margin-bottom: var(--sp-2); }
  `],
  template: `
    <div class="doc-body">
      <ui-breadcrumb [items]="[{ label: 'Home', link: '/' }, { label: 'Design system' }]" />
      <ui-page-header
        kicker="Internal reference"
        heading="Design system"
        description="Every primitive the site is built from, in every variant it supports. If a page needs something that is not here, it belongs here first." />

      <ui-section heading="Colour" anchor="colour">
        <div class="ds-swatches">
          @for (s of swatches; track s.token) {
            <div class="ds-swatch">
              <div class="ds-chip" [style.background]="'var(' + s.token + ')'"></div>
              <div class="ds-label">{{ s.token }}</div>
            </div>
          }
        </div>
      </ui-section>

      <ui-section heading="Type scale" anchor="type">
        <div class="ds-scale">
          <p style="font-size:var(--fs-display)" class="page-title">Display</p>
          <p style="font-size:var(--fs-h2);font-weight:700">Heading 2</p>
          <p style="font-size:var(--fs-xl);font-weight:600">Heading 3</p>
          <p style="font-size:var(--fs-lg)">Large body — page descriptions</p>
          <p style="font-size:var(--fs-base)">Base body text</p>
          <p style="font-size:var(--fs-sm);color:var(--muted)">Small — captions and meta</p>
          <p class="mono" style="font-size:var(--fs-xs);color:var(--faint)">Mono — kickers, counts, specs</p>
        </div>
      </ui-section>

      <ui-section heading="Buttons" anchor="buttons">
        <div class="ds-row">
          <ui-button>Primary</ui-button>
          <ui-button variant="ghost">Ghost</ui-button>
          <ui-button size="lg">Primary large</ui-button>
          <ui-button variant="ghost" size="lg">Ghost large</ui-button>
          <ui-button [disabled]="true">Disabled</ui-button>
        </div>
        <div class="ds-row">
          <ui-button routerLink="/docs">Internal link</ui-button>
          <ui-button href="https://github.com/hpkaushik121/Iso8583studio" [external]="true">External link</ui-button>
        </div>
      </ui-section>

      <ui-section heading="Badges and tags" anchor="badges">
        <div class="ds-row">
          @for (tone of badgeTones; track tone) { <ui-badge [tone]="tone">{{ tone }}</ui-badge> }
        </div>
        <div class="ds-row">
          <ui-tag>blue</ui-tag><ui-tag tone="teal">teal</ui-tag><ui-tag tone="muted">muted</ui-tag>
        </div>
        <div class="ds-row">
          <ui-icon-tile glyph="⇄" /><ui-icon-tile glyph="⚿" /><ui-icon-tile glyph="▣" [large]="true" />
        </div>
      </ui-section>

      <ui-section heading="Cards" anchor="cards">
        <div class="grid-3">
          <ui-card>
            <span class="ui-card-eyebrow">Plain</span>
            <span class="ui-card-title">Static container</span>
            <span class="ui-card-desc">No target, so it renders as a div.</span>
          </ui-card>
          <ui-card variant="tile" routerLink="/docs">
            <span class="ui-card-eyebrow">Tile</span>
            <span class="ui-card-title">Interactive tile</span>
            <span class="ui-card-desc">Lifts on hover because it links somewhere.</span>
          </ui-card>
          <ui-card variant="post" routerLink="/blogs">
            <span class="ui-card-eyebrow">Post</span>
            <span class="ui-card-title">Blog card</span>
            <span class="ui-card-desc">Gains a gradient rule on hover.</span>
            <span class="ui-card-meta"><span>2026-01-01</span><span>7 min read</span></span>
          </ui-card>
        </div>
      </ui-section>

      <ui-section heading="Callouts" anchor="callouts">
        @for (tone of calloutTones; track tone) {
          <ui-callout [tone]="tone" [heading]="tone + ' callout'">
            <p>Body copy inside a {{ tone }} callout.</p>
          </ui-callout>
        }
      </ui-section>

      <ui-section heading="Table" anchor="table">
        <ui-table>
          <table>
            <thead><tr><th>Field</th><th>Type</th><th>Meaning</th></tr></thead>
            <tbody>
              <tr><td>MTI</td><td>n4</td><td>Message type indicator</td></tr>
              <tr><td>DE 3</td><td>n6</td><td>Processing code</td></tr>
              <tr><td>DE 39</td><td>an2</td><td>Response code</td></tr>
            </tbody>
          </table>
        </ui-table>
      </ui-section>

      <ui-section heading="Specs" anchor="specs">
        <ui-spec-row key="Protocol" value="TCP/IP, TLS 1.2+" />
        <ui-spec-row key="Framing" value="2-byte length header" />
        <ui-spec-row key="Character set" value="ASCII / EBCDIC" />
      </ui-section>

      <ui-section heading="Kicker and figure" anchor="misc">
        <ui-kicker>Section label</ui-kicker>
        <ui-figure src="/images/app.png" alt="ISO8583Studio" caption="A figure with its caption" />
      </ui-section>

      <ui-section heading="Dialog" anchor="dialog">
        <ui-button (click)="dialogOpen.set(true)">Open dialog</ui-button>
        @if (dialogOpen()) {
          <ui-dialog label="Example dialog" (close)="dialogOpen.set(false)">
            <h3>Example dialog</h3>
            <p>Focus is trapped inside, Escape closes it, and focus returns to the trigger.</p>
            <div class="pm-actions">
              <ui-button (click)="dialogOpen.set(false)">Confirm</ui-button>
              <ui-button variant="ghost" (click)="dialogOpen.set(false)">Cancel</ui-button>
            </div>
          </ui-dialog>
        }
      </ui-section>

      <ui-section heading="Prose" anchor="prose">
        <div class="prose">
          <h2>Rendered Markdown</h2>
          <p>Article bodies are styled by this layer rather than by components, because Markdown
             renders to bare tags. <a href="/docs">Links</a> and <code>inline code</code> live here.</p>
          <ul><li>List item with <strong>bold</strong></li><li>Another item</li></ul>
          <blockquote><p>A block quotation.</p></blockquote>
          <pre><code>0200 B23A80012...</code></pre>
        </div>
      </ui-section>
    </div>
  `,
})
export class DesignSystem {
  protected readonly dialogOpen = signal(false);
  protected readonly badgeTones = ['blue', 'teal', 'green', 'yellow', 'purple', 'red'] as const;
  protected readonly calloutTones = ['note', 'tip', 'warn', 'danger'] as const;

  protected readonly swatches: Swatch[] = [
    'bg-deep', 'bg', 'surface', 'card', 'card-hi', 'card-deep',
    'text', 'muted', 'faint',
    'blue', 'blue-hi', 'blue-lo', 'blue-deep',
    'teal', 'teal-hi', 'teal-lo',
    'green', 'green-hi', 'amber', 'amber-hi', 'yellow', 'red', 'red-hi', 'purple',
  ].map((name) => ({ name, token: `--${name}` }));
}
