import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { SitePage } from './site-page';
import { UiSection } from '../../ui/section';

@Component({
  selector: 'page-docs-payment-simulators',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiSection],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-payment-simulators' },
  // <image-slot> is a styling-only element the design system owns; see
  // _components.css. Drop this schema once it becomes part of ui-figure.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<main class="doc-body">
        <div class="breadcrumb">
            <a href="/">Home</a><span class="breadcrumb-sep">/</span>
            <a href="/docs">Documentation</a><span class="breadcrumb-sep">/</span>
            <span>Payment Simulators</span>
        </div>

        <h1 class="page-title">Payment Simulators</h1>
        <p class="page-description">Nine simulators covering every party in a payment network — from the card and the terminal to the switch, the HSM and the issuer. Each card links to that simulator's full documentation.</p>

        <ui-section anchor="simulators">
            <div class="hub-grid">
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">Host Simulator <span class="badge badge-teal">Available</span></div>
                        <p class="hub-desc">Acquirer &amp; issuer host responses for POS, ATM and client apps — Server, Client or Proxy over TCP/IP, REST, RS232 and dial-up.</p>
                        <a class="hub-link" href="/docs/host-simulator">Open documentation →</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">HSM Simulator <span class="badge badge-teal">Available</span></div>
                        <p class="hub-desc">Thales payShield 10K emulation — host commands, LMK storage, key management, PIN and MAC operations.</p>
                        <a class="hub-link" href="/docs/hsm-simulator">Open documentation →</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">HSM Command Console <span class="badge badge-purple">Beta</span></div>
                        <p class="hub-desc">Host-command client for Thales, Futurex, Luna, Utimaco and nCipher — interactive console, scenarios and load tests.</p>
                        <a class="hub-link" href="/docs/hsm-command-console">Open documentation →</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">POS Simulator <span class="badge badge-teal">Available</span></div>
                        <p class="hub-desc">A configurable point-of-sale terminal — hardware profile, EMV &amp; contactless kernels — driving ISO 8583 to a host.</p>
                        <a class="hub-link" href="/docs/pos-simulator">Open documentation →</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">APDU Simulator <span class="badge badge-teal">Available</span></div>
                        <p class="hub-desc">EMV smart-card sessions — APDU command/response, TLV parsing, flow analysis and certification test plans.</p>
                        <a class="hub-link" href="/docs/apdu-simulator">Open documentation →</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">Switch Simulator <span class="badge badge-yellow">In development</span></div>
                        <p class="hub-desc">BIN routing, protocol translation and stand-in processing between acquiring and issuing endpoints.</p>
                        <a class="hub-link" href="/docs/payment-switch">Open documentation →</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">Issuer System <span class="badge badge-yellow">In development</span></div>
                        <p class="hub-desc">Issuer-side authorization — PIN and ARQC verification, limits and velocity, 0210 decisioning.</p>
                        <a class="hub-link" href="/docs/issuer-simulator">Open documentation →</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">ATM Simulator <span class="badge badge-yellow">In development</span></div>
                        <p class="hub-desc">Cash withdrawal, balance and PIN-change flows with NDC/DDC device state modelling.</p>
                        <a class="hub-link" href="/docs/atm-simulator">Open documentation →</a>
                    </div>
                </div>
                <div class="hub-card">
                    <div class="hub-body">
                        <div class="hub-title">ECR Simulator <span class="badge badge-yellow">In development</span></div>
                        <p class="hub-desc">Electronic cash register driving sale, void and refund requests to a payment terminal over RS232 or TCP.</p>
                        <a class="hub-link" href="/docs/ecr-simulator">Open documentation →</a>
                    </div>
                </div>
            </div>
            <div class="info-card note"><div class="info-card-title">Status</div><p><span class="badge badge-teal">Available</span> ships in the current release · <span class="badge badge-purple">Beta</span> is usable and evolving · <span class="badge badge-yellow">In development</span> is scaffolded in the app — follow the <a href="https://github.com/users/hpkaushik121/projects/1">roadmap</a>.</p></div>
        </ui-section>
</main>

<aside class="pro-nudge"><span class="pn-tag">✦ Pro</span><p>Testing with a team, or certifying with a scheme? Pro raises the CPS ceiling, unlocks the full algorithm set and deep simulator tweaks, plus hosted endpoints and priority support.</p><a href="/pro">Register for Pro →</a></aside>`,
})
export class DocsPaymentSimulatorsPage {}
