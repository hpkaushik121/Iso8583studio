import {
  ChangeDetectionStrategy, Component, DOCUMENT, effect, inject, input, output,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { EXTERNAL, LEGAL, RESOURCES, SIMULATORS, SOLUTIONS, TOOLS } from '../core/site-nav';
import { UiLogoMark } from '../ui/logo-mark';
import { UiIcon } from '../ui/icon';

/**
 * The full-screen navigation drawer used once the header collapses.
 *
 * The group labels and their links are flat siblings inside .m-menu, not
 * nested lists: analytics finds a link's nav_group by walking back through its
 * previous siblings to the nearest .grp, so wrapping each group in its own
 * element would blank that parameter. The close control carries .ham for the
 * same reason — it reports as the menu toggle.
 */
@Component({
  selector: 'app-mobile-menu',
  imports: [RouterLink, UiLogoMark, UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="m-menu" id="m-menu" [class.open]="open()" [attr.aria-hidden]="!open()">
      <div class="m-top">
        <a class="brand" routerLink="/" (click)="navigate.emit()"><ui-logo-mark [size]="24" mode="still" />ISO8583Studio</a>
        <button class="ham icon-btn" type="button" aria-label="Close menu" aria-controls="m-menu"
                aria-expanded="true" (click)="navigate.emit()">
          <ui-icon name="x" [size]="18" />
        </button>
      </div>
      @for (section of sections; track section.title) {
        <p class="grp">{{ section.title }}</p>
        @for (item of section.items; track item.link) {
          <a class="m-link" [routerLink]="item.link" (click)="navigate.emit()">
            @if (item.icon) { <ui-icon [name]="item.icon" [size]="16" /> }{{ item.label }}
          </a>
        }
      }
      <a class="btn btn--primary btn--block m-download" [href]="external.releases">
        Download Studio<ui-icon name="arrow-up-right" [size]="16" />
      </a>
    </div>
  `,
})
export class MobileMenu {
  readonly open = input(false);
  readonly navigate = output<void>();

  protected readonly external = EXTERNAL;
  protected readonly sections = [
    { title: 'Simulators', items: SIMULATORS },
    { title: 'Tools', items: TOOLS },
    { title: 'Solutions', items: SOLUTIONS },
    { title: 'Resources', items: RESOURCES },
    { title: 'Legal', items: LEGAL },
  ];

  constructor() {
    const doc = inject(DOCUMENT);
    // The drawer covers the viewport, so the page behind it must not scroll.
    effect(() => { doc.body.style.overflow = this.open() ? 'hidden' : ''; });
  }
}
