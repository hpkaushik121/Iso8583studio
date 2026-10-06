import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EXTERNAL } from '../../core/site-nav';
import { UiIcon } from '../../ui';
import { HOST_SIMULATOR_GUIDE } from '../data/host-simulator.data';
import {
  GuideBlocks, GuideCards, GuideCta, GuideFeatures, GuideHero, GuideLayout, GuideRich, GuideSection,
} from '../shared/guide';
import { HeroVideo } from '../shared/hero-video';
import { HOST_SCREENS, HostScreen, HostScreenId } from '../shared/simglass/host-screens';
import { SimScreenRef, simCaption, simCrumbs, simRail } from '../shared/simglass/sim-guide';
import { SitePage } from './site-page';

/**
 * Host Simulator guide (/simulator/host): the looping terminal-and-host hero
 * video, the overview with the ISO8583 Transaction screen, the runtime-tab
 * cards, one section per configuration screen, tab and reference table, and
 * the download panel.
 *
 * Content: pages/data/host-simulator.data.ts. Glass screens: pages/shared/simglass/.
 * Styles: styles/bundles/guide.css and _sims-hsm.css.
 */
@Component({
  selector: 'page-docs-host-simulator',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink, UiIcon, GuideHero, GuideLayout, GuideSection, GuideBlocks, GuideCards, GuideFeatures, GuideCta,
    GuideRich, HeroVideo, HostScreen,
  ],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-host-simulator' },
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
          <app-host-screen [id]="g.intro.screen.id" />
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
              @if (ref.kind === 'phcards') {
                <div class="hostg-phs">
                  @for (card of ref.items; track card.tok) {
                    <div class="hostg-ph">
                      <span class="hostg-ph-tok" [class]="'hostg-ph-tok--' + card.tone">{{ card.tok }}</span>
                      <strong class="hostg-ph-name">{{ card.name }}</strong>
                      <span class="hostg-ph-desc">{{ card.desc }}</span>
                      <div class="hostg-ph-label">Examples</div>
                      <ul class="gd-bullets hostg-ph-ex">
                        @for (ex of card.ex; track ex[0]) { <li class="gd-rich"><code>{{ ex[0] }}</code><span> → {{ ex[1] }}</span></li> }
                      </ul>
                      <span class="hostg-ph-note">{{ card.note }}</span>
                    </div>
                  }
                </div>
              } @else {
                <div class="hg-figure">
                  <app-host-screen [id]="ref.id" />
                  <p class="gd-caption">{{ caption(ref) }}</p>
                </div>
              }
            </ng-template>
          </app-guide-blocks>
        </app-guide-section>
      }

      <app-guide-cta [heading]="g.cta.heading" [text]="g.cta.text" />
    </app-guide-layout>
  `,
})
export class DocsHostSimulatorPage {
  protected readonly g = HOST_SIMULATOR_GUIDE;
  protected readonly releases = EXTERNAL.releases;
  protected readonly crumbs = simCrumbs(this.g.title);
  protected readonly rail = simRail(this.g);

  protected caption(ref: SimScreenRef<HostScreenId>): string {
    return simCaption(this.g.slug, HOST_SCREENS[ref.id].sub, ref.caption);
  }
}
