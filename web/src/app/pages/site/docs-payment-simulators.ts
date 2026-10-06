import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SitePage } from './site-page';
import { EXTERNAL, SIMULATORS } from '../../core/site-nav';
import { Crumb, UiIcon } from '../../ui';
import { GuideCta, GuideHero, GuideLayout, GuideNote, GuideRailItem, GuideSection } from '../shared/guide';

type SimStatus = 'Available' | 'Beta' | 'In development';

interface SimCard {
  name: string;
  link: string;
  icon: string;
  status: SimStatus;
  /** Card modifier that sets the status tint. */
  tone: string;
  /** Badge modifier. */
  badge: string;
  desc: string;
}

/** What each simulator does, keyed by its route. Names, links and icons come from SIMULATORS. */
const DESCRIPTIONS: Record<string, string> = {
  '/simulator/host': 'Acquirer & issuer host responses for POS, ATM and client apps — Server, Client or Proxy over TCP/IP, REST, RS232 and dial-up.',
  '/simulator/hsm': 'Thales payShield 10K emulation — host commands, LMK storage, key management, PIN and MAC operations.',
  '/simulator/hsm-command-console': 'Host-command client for Thales, Futurex, Luna, Utimaco and nCipher — interactive console, scenarios and load tests.',
  '/simulator/pos': 'A configurable point-of-sale terminal — hardware profile, EMV & contactless kernels — driving ISO 8583 to a host.',
  '/simulator/apdu': 'EMV smart-card sessions — APDU command/response, TLV parsing, flow analysis and certification test plans.',
  '/simulator/payment-switch': 'BIN routing, protocol translation and stand-in processing between acquiring and issuing endpoints.',
  '/simulator/issuer': 'Issuer-side authorization — PIN and ARQC verification, limits and velocity, 0210 decisioning.',
  '/simulator/atm': 'Cash withdrawal, balance and PIN-change flows with NDC/DDC device state modelling.',
  '/simulator/ecr': 'Electronic cash register driving sale, void and refund requests to a payment terminal over RS232 or TCP.',
};

/** The nav's status chip ('Available' | 'Beta' | 'Dev') to the wording and tint of a card. */
const STATUS: Record<string, Pick<SimCard, 'status' | 'tone' | 'badge'>> = {
  Available: { status: 'Available', tone: 'dx-sim--live', badge: 'badge--teal' },
  Beta: { status: 'Beta', tone: 'dx-sim--beta', badge: 'badge--blue' },
  Dev: { status: 'In development', tone: 'dx-sim--dev', badge: 'badge--neutral' },
};

/**
 * Payment Simulators — /simulator. The index of the nine simulators: one hub
 * card each (a.hub-card > .hub-title > .badge, so a click reports
 * hub_card_click with the simulator's name and status), then the status key.
 * Built on the guide shell; card styles are in styles/bundles/docs.css.
 */
@Component({
  selector: 'page-docs-payment-simulators',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, UiIcon, GuideHero, GuideLayout, GuideSection, GuideNote, GuideCta],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-payment-simulators' },
  template: `
    <div class="dx-page">
      <app-guide-hero heading="Payment Simulators" [crumbs]="crumbs" meta="Simulator index · 9 simulators" [dots]="true"
                      lede="Nine simulators covering every party in a payment network — from the card and the terminal to the switch, the HSM and the issuer. Each card links to that simulator's full documentation.">
        <a class="btn btn--primary btn--glow" [href]="releases">Download Studio<ui-icon name="arrow-up-right" [size]="16" /></a>
        <a class="btn btn--secondary" [routerLink]="[]" fragment="simulators">Browse the simulators<ui-icon name="arrow-down" [size]="16" /></a>
      </app-guide-hero>

      <app-guide-layout [rail]="rail" badge="Simulator index">
        <app-guide-section anchor="simulators" heading="All simulators" [threshold]="0.1">
          <div class="dx-sims">
            @for (sim of sims; track sim.link; let i = $index) {
              <div class="dx-sim-cell ds-item" [style.--d]="300 + i * 70">
                <a class="hub-card dx-sim" [class]="sim.tone" [routerLink]="sim.link">
                  <span class="dx-sim-ico"><ui-icon [name]="sim.icon" [size]="18" /></span>
                  <span class="hub-title">{{ sim.name }} <span class="badge" [class]="sim.badge">{{ sim.status }}</span></span>
                  <span class="dx-sim-desc">{{ sim.desc }}</span>
                  <span class="dx-sim-go">Open guide<ui-icon name="arrow-right" [size]="13" /></span>
                </a>
              </div>
            }
          </div>

          <app-guide-note class="dx-status" tone="neutral" icon="info" heading="Status">
            <span class="badge badge--teal">Available</span> ships in the current release ·
            <span class="badge badge--blue">Beta</span> is usable and evolving ·
            <span class="badge badge--neutral">In development</span> is scaffolded in the app — follow the
            <a class="dx-link" [href]="roadmap">roadmap</a>.
          </app-guide-note>
        </app-guide-section>

        <app-guide-cta heading="Try it on your own transactions"
                       text="Free and open source. Download the studio and run these simulators on your desk in minutes." />
      </app-guide-layout>
    </div>
  `,
})
export class DocsPaymentSimulatorsPage {
  protected readonly releases = EXTERNAL.releases;
  protected readonly roadmap = EXTERNAL.roadmap;

  protected readonly crumbs: Crumb[] = [
    { label: 'Home', link: '/' },
    { label: 'Documentation', link: '/docs' },
    { label: 'Payment Simulators' },
  ];

  protected readonly rail: GuideRailItem[] = [
    { id: 'simulators', label: 'All simulators', icon: 'plugs-connected' },
    { id: 'download', label: 'Try it', icon: 'download-simple' },
  ];

  protected readonly sims: SimCard[] = SIMULATORS.map((s) => ({
    name: s.label,
    link: s.link,
    icon: s.icon ?? 'plugs-connected',
    desc: DESCRIPTIONS[s.link] ?? s.desc ?? '',
    ...(STATUS[s.chip ?? 'Dev'] ?? STATUS['Dev']),
  }));
}
