import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SitePage } from './site-page';
import { EXTERNAL } from '../../core/site-nav';
import { Crumb, UiIcon } from '../../ui';
import { GuideBlocks, GuideCta, GuideHero, GuideLayout, GuideSection, GuideTips } from '../shared/guide';
import { railOf } from '../data/docs-reference.data';
import { MAC_TOOLS_GUIDE } from '../data/mac-tools.data';

/**
 * MAC Tools — /tools/mac-tools.
 *
 * A screenshot-based tool reference on the guide shell: centred hero, section
 * rail, one guide section per live section id, the closing download panel.
 * The content is in pages/data/mac-tools.data.ts; the one page-specific block
 * is the framed screenshot (styles/bundles/docs.css, .dx-fig).
 */
@Component({
  selector: 'page-docs-mac-tools',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, UiIcon, GuideHero, GuideLayout, GuideSection, GuideBlocks, GuideTips, GuideCta],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-mac-tools' },
  template: `
    <div class="dx-page">
      <app-guide-hero [heading]="g.title" [crumbs]="crumbs" [meta]="g.meta" [lede]="g.lede" [dots]="true">
        <a class="btn btn--primary btn--glow" [href]="releases">Download Studio<ui-icon name="arrow-up-right" [size]="16" /></a>
        <a class="btn btn--secondary" [routerLink]="[]" [fragment]="g.browse">Browse the tools<ui-icon name="arrow-down" [size]="16" /></a>
      </app-guide-hero>

      <app-guide-layout [rail]="rail" badge="Tool reference">
        @for (section of g.sections; track section.id) {
          <app-guide-section [anchor]="section.id" [heading]="section.title" [eyebrow]="section.eyebrow ?? null"
                             [intro]="section.intro ?? null">
            @if (section.blocks; as blocks) {
              <app-guide-blocks [blocks]="blocks">
                <ng-template let-fig>
                  <div class="dx-fig" [class.dx-fig--split]="fig.side">
                    <figure class="dx-shot" [style.--dx-w.px]="fig.shot.max">
                      <span class="dx-shot-frame">
                        <img [src]="fig.shot.src" [alt]="fig.shot.alt" [attr.width]="fig.shot.w"
                             [attr.height]="fig.shot.h" loading="lazy" decoding="async">
                      </span>
                      <figcaption class="gd-caption">{{ g.slug }} · {{ fig.shot.caption }}</figcaption>
                    </figure>
                    @if (fig.side) { <app-guide-blocks class="dx-fig-side" [blocks]="fig.side" /> }
                  </div>
                </ng-template>
              </app-guide-blocks>
            }
            @if (section.tips; as tips) { <app-guide-tips [items]="tips" /> }
          </app-guide-section>
        }

        <app-guide-cta [heading]="g.cta.heading" [text]="g.cta.text" />
      </app-guide-layout>
    </div>
  `,
})
export class DocsMacToolsPage {
  protected readonly g = MAC_TOOLS_GUIDE;
  protected readonly releases = EXTERNAL.releases;
  protected readonly rail = railOf(MAC_TOOLS_GUIDE);
  protected readonly crumbs: Crumb[] = [
    { label: 'Home', link: '/' },
    { label: 'Documentation', link: '/docs' },
    { label: MAC_TOOLS_GUIDE.crumb },
  ];
}
