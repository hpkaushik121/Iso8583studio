import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EXTERNAL, LEGAL, RESOURCES, SIMULATORS, SOLUTIONS, TOOLS } from '../core/site-nav';
import { ICON_PATHS } from './icons';

@Component({
  selector: 'app-footer',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <footer>
      <div class="wrap">
        <div class="f-grid">
          <div class="f-brand">
            <a class="brand" routerLink="/"><img src="/images/app.png" alt="" width="27" height="27">ISO8583Studio</a>
            <p>Professional ISO 8583 payment transaction processing, simulation and testing.
               Built with Kotlin Multiplatform &amp; Compose Desktop.</p>
            <a class="btn btn--primary" [href]="external.releases">Download</a>
            <div class="f-social">
              @for (s of social; track s.title) {
                <a [href]="s.href" [title]="s.title" rel="noopener">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                    <path [attr.d]="s.path"/>
                  </svg>
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
          <span>© {{ year }} AiCortex · ISO8583Studio</span>
          <span class="mono">Built with ❤ for the payments community</span>
        </div>
      </div>
    </footer>
  `,
})
export class Footer {
  /** Derived, so the footer can never drift out of date the way the two
   *  hard-coded years ("2024" in the blog, "2026" in site.js) did. */
  protected readonly year = new Date().getFullYear();

  protected readonly external = EXTERNAL;

  protected readonly social = [
    { title: 'Roadmap', href: EXTERNAL.roadmap, path: ICON_PATHS.roadmap },
    { title: 'LinkedIn', href: EXTERNAL.linkedin, path: ICON_PATHS.linkedin },
    { title: 'GitHub', href: EXTERNAL.github, path: ICON_PATHS.github },
    { title: 'Medium', href: EXTERNAL.medium, path: ICON_PATHS.medium },
  ];

  protected readonly columns = [
    { title: 'Simulators', items: SIMULATORS },
    { title: 'Tools', items: TOOLS },
    { title: 'Solutions', items: SOLUTIONS },
    { title: 'Resources', items: RESOURCES },
    { title: 'Legal', items: LEGAL },
  ];
}
