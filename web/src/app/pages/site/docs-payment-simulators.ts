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
      <app-guide-hero heading="ISO 8583 Simulators" [crumbs]="crumbs" meta="Simulator index · 9 simulators · free and open source" [dots]="true"
                      lede="ISO8583Studio is a free, open-source ISO 8583 simulator suite for the desktop: nine simulators covering every party in a payment network, from the card and the terminal to the switch, the HSM and the issuer. Send a 0200 to a host you control, script the 0210, translate a PIN block through a simulated payShield — with no bank connection and nothing leaving your machine. Each card links to that simulator's full documentation.">
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

        <app-guide-section anchor="compare" heading="Which simulator does what" [threshold]="0.1">
          <p class="gd-lede">Every simulator speaks ISO 8583 to the others, so you can chain them: a POS Simulator sends to a Host Simulator that calls the HSM Simulator for PIN translation. The table is what each one models, the protocol it speaks and the status of the build.</p>
          <div class="lf-table-wrap"><table class="lf-table dx-matrix">
            <thead><tr><th>Simulator</th><th>Models</th><th>Protocol / transport</th><th>Status</th></tr></thead>
            <tbody>
              @for (row of matrix; track row.link) {
                <tr>
                  <td><a class="dx-link" [routerLink]="row.link">{{ row.name }}</a></td>
                  <td>{{ row.models }}</td>
                  <td>{{ row.protocol }}</td>
                  <td><span class="badge" [class]="row.badge">{{ row.status }}</span></td>
                </tr>
              }
            </tbody>
          </table></div>
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

  /** The comparison matrix. Status mirrors the cards above; protocol is the
   *  wire the simulator speaks, not the UI it has. */
  protected readonly matrix = [
    { name: 'Host Simulator', link: '/simulator/host', models: 'Acquirer or issuer host; proxy between two hosts', protocol: 'ISO 8583 over TCP/IP, REST, RS232 or dial-up', status: 'Available', badge: 'badge--teal' },
    { name: 'HSM Simulator', link: '/simulator/hsm', models: 'Thales payShield 10K: LMK storage, keys, PIN, MAC', protocol: 'payShield host commands over TCP/IP', status: 'Available', badge: 'badge--teal' },
    { name: 'HSM Command Console', link: '/simulator/hsm-command-console', models: 'Host-side client for payShield, Futurex, Luna, Utimaco, nCipher', protocol: 'Vendor host commands over TCP/IP, optional TLS', status: 'Beta', badge: 'badge--blue' },
    { name: 'POS Simulator', link: '/simulator/pos', models: 'Android payment terminal in an emulator, 23 device models', protocol: 'ISO 8583 to a host; APK install over adb', status: 'Beta', badge: 'badge--blue' },
    { name: 'APDU Simulator', link: '/simulator/apdu', models: 'EMV card: in-process profile, PC/SC reader or STM32 emulation', protocol: 'ISO 7816 APDU, BER-TLV', status: 'Beta', badge: 'badge--blue' },
    { name: 'Switch Simulator', link: '/simulator/payment-switch', models: 'BIN routing, protocol translation, stand-in', protocol: 'ISO 8583 in and out', status: 'In development', badge: 'badge--neutral' },
    { name: 'Issuer System', link: '/simulator/issuer', models: 'Issuer authorization: PIN and ARQC checks, limits, 0210', protocol: 'ISO 8583 0200 in, 0210 out', status: 'In development', badge: 'badge--neutral' },
    { name: 'ATM Simulator', link: '/simulator/atm', models: 'Self-service cash machine: withdrawal, balance, PIN change', protocol: 'NDC / DDC device states, ISO 8583 to host', status: 'In development', badge: 'badge--neutral' },
    { name: 'ECR Simulator', link: '/simulator/ecr', models: 'Electronic cash register driving a payment terminal', protocol: 'ECR-to-terminal messages: sale, void, refund', status: 'In development', badge: 'badge--neutral' },
  ] as const;

  protected readonly crumbs: Crumb[] = [
    { label: 'Home', link: '/' },
    { label: 'Documentation', link: '/docs' },
    { label: 'Payment Simulators' },
  ];

  protected readonly rail: GuideRailItem[] = [
    { id: 'simulators', label: 'All simulators', icon: 'plugs-connected' },
    { id: 'compare', label: 'Which does what', icon: 'info' },
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
