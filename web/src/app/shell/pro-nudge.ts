import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-pro-nudge',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <aside class="pro-nudge">
      <span class="pn-tag">✦ Pro</span>
      <p>{{ label() }} Pro raises the CPS ceiling, unlocks the full algorithm set and deep
         simulator tweaks, plus hosted endpoints and priority support.</p>
      <a routerLink="/pro">Register for Pro →</a>
    </aside>
  `,
})
export class ProNudge {
  readonly label = input('Testing with a team, or certifying with a scheme?');
}
