import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EXTERNAL, LEGAL, RESOURCES, SIMULATORS, SOLUTIONS, TOOLS } from '../core/site-nav';
import { UiLogoMark } from '../ui/logo-mark';
import { UiIcon } from '../ui/icon';

/**
 * Site footer.
 *
 * Analytics reads a footer link's nav_group from the <b> inside its .f-col
 * (or 'Social' inside .f-social), and an icon-only link's text from its
 * title — so the column heads stay <b>, the legal links in the bottom bar get
 * a hidden one, and every social link keeps its title.
 */
@Component({
  selector: 'app-footer',
  imports: [RouterLink, UiLogoMark, UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <footer>
      <div class="f-in">
        <div class="f-grid">
          <div class="f-brand">
            <a class="brand" routerLink="/"><ui-logo-mark [size]="24" mode="still" />ISO8583Studio</a>
            <p>Professional ISO 8583 payment transaction processing, simulation and testing.
               Built with Kotlin Multiplatform &amp; Compose Desktop.</p>
            <a class="btn btn--secondary btn--sm" [href]="external.releases">
              <ui-icon name="download-simple" [size]="14" />Download
            </a>
            <div class="f-social">
              @for (s of social; track s.title) {
                <a class="icon-btn icon-btn--filled icon-btn--round" [href]="s.href" [title]="s.title" rel="noopener">
                  <ui-icon [name]="s.icon" [size]="18" />
                  <span class="visually-hidden">{{ s.title }}</span>
                </a>
              }
            </div>
          </div>

          @for (col of columns; track col.title) {
            <div class="f-col">
              <b>{{ col.title }}</b>
              @for (item of col.items; track item.link) {
                <a [routerLink]="item.link">{{ item.label }}</a>
              }
            </div>
          }
        </div>
        <div class="f-btm">
          <span>© {{ year }} AiCortex · ISO8583Studio · Built with ❤ for the payments community</span>
          <span class="f-col f-legal">
            <b class="visually-hidden">Legal</b>
            @for (item of legal; track item.link) {
              <a [routerLink]="item.link">{{ item.label }}</a>
            }
          </span>
        </div>
      </div>
    </footer>
  `,
})
export class Footer {
  /** Derived, so the footer can never drift out of date. */
  protected readonly year = new Date().getFullYear();

  protected readonly external = EXTERNAL;
  protected readonly legal = LEGAL;

  protected readonly social = [
    { title: 'Roadmap', href: EXTERNAL.roadmap, icon: 'map-trifold' },
    { title: 'LinkedIn', href: EXTERNAL.linkedin, icon: 'linkedin-logo' },
    { title: 'GitHub', href: EXTERNAL.github, icon: 'github-logo' },
    { title: 'Medium', href: EXTERNAL.medium, icon: 'medium-logo' },
  ];

  protected readonly columns = [
    { title: 'Simulators', items: SIMULATORS },
    { title: 'Tools', items: TOOLS },
    { title: 'Solutions', items: SOLUTIONS },
    { title: 'Resources', items: RESOURCES },
  ];
}
