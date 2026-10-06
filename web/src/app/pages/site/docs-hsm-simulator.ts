import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EXTERNAL } from '../../core/site-nav';
import { UiIcon } from '../../ui';
import { HSM_SIMULATOR_GUIDE } from '../data/hsm-simulator.data';
import {
  GuideBlocks, GuideCards, GuideCta, GuideFeatures, GuideHero, GuideLayout, GuideRich, GuideSection,
} from '../shared/guide';
import { HeroVideo } from '../shared/hero-video';
import { HSM_SCREENS, HsmScreen, HsmScreenId } from '../shared/simglass/hsm-screens';
import { SimScreenRef, simCaption, simCrumbs, simRail } from '../shared/simglass/sim-guide';
import { SitePage } from './site-page';

/**
 * HSM Simulator guide (/simulator/hsm): the looping HSM hero video, the
 * overview with the HSM Handler screen, the command-set cards, one section per
 * configuration tab, command group and reference table, and the download panel.
 *
 * Content: pages/data/hsm-simulator.data.ts. Glass screens: pages/shared/simglass/.
 * Styles: styles/bundles/guide.css and _sims-hsm.css.
 */
@Component({
  selector: 'page-docs-hsm-simulator',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink, UiIcon, GuideHero, GuideLayout, GuideSection, GuideBlocks, GuideCards, GuideFeatures, GuideCta,
    GuideRich, HeroVideo, HsmScreen,
  ],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-hsm-simulator' },
  template: `
    <app-guide-hero align="left" [heading]="g.title" [meta]="g.meta" [lede]="g.lede" [crumbs]="crumbs">
      <app-hero-video gdHeroBg [clips]="g.clips" [poster]="g.poster" [style.--d]="500" />
      <a class="btn btn--primary btn--glow" [href]="releases">Download Studio<ui-icon name="arrow-up-right" [size]="16" /></a>
      <a class="btn btn--secondary" [routerLink]="[]" fragment="commands">{{ g.browse }}<ui-icon name="arrow-down" [size]="16" /></a>
    </app-guide-hero>

    <app-guide-layout [rail]="rail" badge="Simulator guide">
      <app-guide-section anchor="overview" heading="Overview" [eyebrow]="g.intro.eyebrow" eyebrowTone="faint" [threshold]="0.15">
        @for (para of g.intro.paras; track $index) {
          <p class="gd-lede ds-hold" [style.--d]="220 + $index * 120" [gdRich]="para"></p>
        }
        <div class="hg-figure">
          <app-hsm-screen [id]="g.intro.screen.id" />
          <p class="gd-caption">{{ caption(g.intro.screen) }}</p>
        </div>
        <app-guide-features [items]="g.intro.features" />
      </app-guide-section>

      <app-guide-section anchor="commands" [heading]="g.cards.heading" [intro]="g.cards.lede" [threshold]="0.1">
        <app-guide-cards [items]="g.cards.items" [more]="g.cards.more" />
      </app-guide-section>

      @for (section of g.sections; track section.id) {
        <app-guide-section [anchor]="section.id" [heading]="section.title" [eyebrow]="section.eyebrow ?? null"
                           [intro]="section.intro ?? null">
          <app-guide-blocks [blocks]="section.blocks">
            <ng-template let-ref>
              <div class="hg-figure">
                <app-hsm-screen [id]="ref.id" />
                <p class="gd-caption">{{ caption(ref) }}</p>
              </div>
            </ng-template>
          </app-guide-blocks>
        </app-guide-section>
      }

      <app-guide-cta [heading]="g.cta.heading" [text]="g.cta.text" />
    </app-guide-layout>
  `,
})
export class DocsHsmSimulatorPage {
  protected readonly g = HSM_SIMULATOR_GUIDE;
  protected readonly releases = EXTERNAL.releases;
  protected readonly crumbs = simCrumbs(this.g.title);
  protected readonly rail = simRail(this.g);

  protected caption(ref: SimScreenRef<HsmScreenId>): string {
    return simCaption(this.g.slug, HSM_SCREENS[ref.id].sub, ref.caption);
  }
}
