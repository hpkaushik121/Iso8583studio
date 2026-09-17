import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EXTERNAL, LEGAL, RESOURCES, SIMULATORS, SOLUTIONS, TOOLS } from '../core/site-nav';

@Component({
  selector: 'app-mobile-menu',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="m-menu" id="m-menu" [class.open]="open()">
      @for (section of sections; track section.title) {
        <p class="grp">{{ section.title }}</p>
        @for (item of section.items; track item.link) {
          <a [routerLink]="item.link" (click)="navigate.emit()">{{ item.label }}</a>
        }
      }
      <a [href]="external.releases">Download Studio</a>
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
}
