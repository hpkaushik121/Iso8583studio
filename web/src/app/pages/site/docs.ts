import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { SitePage } from './site-page';
import { UiSection } from '../../ui/section';

@Component({
  selector: 'page-docs',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [UiSection],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs' },
  // <image-slot> is a styling-only element the design system owns; see
  // _components.css. Drop this schema once it becomes part of ui-figure.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  template: `<main class="doc-body">
        <div class="breadcrumb">
            <a href="/">Home</a><span class="breadcrumb-sep">/</span>
            <span>Documentation</span>
        </div>

        <h1 class="page-title">Documentation</h1>
        <p class="page-description">Guides and references for every simulator, tool and service in ISO8583Studio.</p>

        <ui-section anchor="simulators" heading="Simulator Guides">
            <p>One guide per simulator — configuration, runtime tabs and protocol details. Or start from the <a href="/simulator">all-simulators overview</a>.</p>
            <div class="hub-grid">
                <a class="hub-card" href="/simulator/host"><div class="hub-body"><div class="hub-title">Host Simulator <span class="badge badge-teal">Available</span></div><p class="hub-desc">Acquirer &amp; issuer host responses — Server, Client or Proxy over TCP/IP, REST, RS232 and dial-up.</p><span class="hub-link">Open guide →</span></div></a>
                <a class="hub-card" href="/simulator/hsm"><div class="hub-body"><div class="hub-title">HSM Simulator <span class="badge badge-teal">Available</span></div><p class="hub-desc">Thales payShield 10K emulation — host commands, LMK storage, keys, PIN and MAC.</p><span class="hub-link">Open guide →</span></div></a>
                <a class="hub-card" href="/simulator/hsm-command-console"><div class="hub-body"><div class="hub-title">HSM Command Console <span class="badge badge-purple">Beta</span></div><p class="hub-desc">Host-command client for Thales, Futurex, Luna, Utimaco and nCipher — console, scenarios, load tests.</p><span class="hub-link">Open guide →</span></div></a>
                <a class="hub-card" href="/simulator/pos"><div class="hub-body"><div class="hub-title">POS Simulator <span class="badge badge-teal">Available</span></div><p class="hub-desc">A configurable point-of-sale terminal — hardware, EMV &amp; contactless — driving ISO 8583 to a host.</p><span class="hub-link">Open guide →</span></div></a>
                <a class="hub-card" href="/simulator/apdu"><div class="hub-body"><div class="hub-title">APDU Simulator <span class="badge badge-teal">Available</span></div><p class="hub-desc">EMV smart-card sessions — APDU exchange, TLV parsing, flow analysis and test plans.</p><span class="hub-link">Open guide →</span></div></a>
                <a class="hub-card" href="/simulator/payment-switch"><div class="hub-body"><div class="hub-title">Switch Simulator <span class="badge badge-yellow">In development</span></div><p class="hub-desc">BIN routing, protocol translation and stand-in between acquirers and issuers.</p><span class="hub-link">Open guide →</span></div></a>
                <a class="hub-card" href="/simulator/issuer"><div class="hub-body"><div class="hub-title">Issuer System <span class="badge badge-yellow">In development</span></div><p class="hub-desc">Issuer-side authorization — PIN and ARQC verification, limits, 0210 decisioning.</p><span class="hub-link">Open guide →</span></div></a>
                <a class="hub-card" href="/simulator/atm"><div class="hub-body"><div class="hub-title">ATM Simulator <span class="badge badge-yellow">In development</span></div><p class="hub-desc">Cash withdrawal, balance and PIN-change flows with NDC/DDC device states.</p><span class="hub-link">Open guide →</span></div></a>
                <a class="hub-card" href="/simulator/ecr"><div class="hub-body"><div class="hub-title">ECR Simulator <span class="badge badge-yellow">In development</span></div><p class="hub-desc">Electronic cash register driving sale, void and refund to a payment terminal.</p><span class="hub-link">Open guide →</span></div></a>
            </div>
        </ui-section>

        <ui-section anchor="tools" heading="Tool References">
            <p>Category references for the 64 tools in the studio.</p>
            <div class="hub-grid">
                <a class="hub-card" href="/tools/emv-tools"><div class="hub-body"><div class="hub-title">EMV &amp; Card Tools <span class="badge badge-blue">12 tools</span></div><p class="hub-desc">Cryptograms (ARQC/TC), SDA/DDA, ATR parsing, tag dictionary, CAP tokens, secure messaging.</p><span class="hub-link">Open reference →</span></div></a>
                <a class="hub-card" href="/tools/cipher-tools"><div class="hub-body"><div class="hub-title">Cryptographic Tools <span class="badge badge-blue">7 tools</span></div><p class="hub-desc">AES, DES/3DES, RSA, Thales RSA, ECDSA and format-preserving encryption calculators.</p><span class="hub-link">Open reference →</span></div></a>
                <a class="hub-card" href="/tools/key-tools"><div class="hub-body"><div class="hub-title">Key Management <span class="badge badge-blue">10 tools</span></div><p class="hub-desc">DEA keys, key shares, SSL certificates, Atalla and Futurex key calculators.</p><span class="hub-link">Open reference →</span></div></a>
                <a class="hub-card" href="/tools/pin-tools"><div class="hub-body"><div class="hub-title">Payment Utilities <span class="badge badge-blue">21 tools</span></div><p class="hub-desc">PIN blocks (ISO 9564 and OEM), AES PIN blocks, TPK-to-ZPK translation and DUKPT PIN encryption.</p><span class="hub-link">Open reference →</span></div></a>
                <a class="hub-card" href="/tools/utility-tools"><div class="hub-body"><div class="hub-title">Data Converters <span class="badge badge-blue">6 tools</span></div><p class="hub-desc">Base64, Base94, BCD, character encoding, check digits and the Track 2 codec.</p><span class="hub-link">Open reference →</span></div></a>
                <a class="hub-card" href="/tools/dukpt-tools"><div class="hub-body"><div class="hub-title">DUKPT Tools</div><p class="hub-desc">Key derivation per ANSI X9.24 — IPEK, KSN walks and transaction keys.</p><span class="hub-link">Open reference →</span></div></a>
                <a class="hub-card" href="/tools/mac-tools"><div class="hub-body"><div class="hub-title">MAC Tools</div><p class="hub-desc">Message authentication — ISO 9797 algorithms, retail MAC and HMAC.</p><span class="hub-link">Open reference →</span></div></a>
                <a class="hub-card" href="/tools/card-validation"><div class="hub-body"><div class="hub-title">Card Validation</div><p class="hub-desc">PAN validation, CVV/CVC computation and card-number utilities.</p><span class="hub-link">Open reference →</span></div></a>
            </div>
        </ui-section>

        <ui-section anchor="solutions" heading="Solutions &amp; Services">
            <div class="hub-grid">
                <a class="hub-card" href="/emv-certification"><div class="hub-body"><div class="hub-title">EMV Certification</div><p class="hub-desc">L1 / L2 / L3 and scheme certification — from gap analysis to lab sign-off.</p><span class="hub-link">Learn more →</span></div></a>
                <a class="hub-card" href="/cloud-simulators"><div class="hub-body"><div class="hub-title">Cloud Simulators</div><p class="hub-desc">Hosted host &amp; HSM endpoints for CI pipelines and distributed teams.</p><span class="hub-link">Learn more →</span></div></a>
                <a class="hub-card" href="/middleware"><div class="hub-body"><div class="hub-title">Payment Middleware</div><p class="hub-desc">Switching, routing and protocol translation built on the Studio engine.</p><span class="hub-link">Learn more →</span></div></a>
                <a class="hub-card" href="/kernel"><div class="hub-body"><div class="hub-title">Kernel Development</div><p class="hub-desc">EMV L2 kernel engineering for terminals, contact to contactless.</p><span class="hub-link">Learn more →</span></div></a>
            </div>
        </ui-section>

        <ui-section anchor="resources" heading="Resources">
            <div class="hub-grid">
                <a class="hub-card" href="/docs/installation"><div class="hub-body"><div class="hub-title">Installation</div><p class="hub-desc">Prerequisites and install steps for Windows, macOS and Linux — plus building from source.</p><span class="hub-link">Install →</span></div></a>
                <a class="hub-card" href="/docs/versions"><div class="hub-body"><div class="hub-title">Versions</div><p class="hub-desc">Current release (v1.0.14), the release channel and upgrade notes.</p><span class="hub-link">See versions →</span></div></a>
                <a class="hub-card" href="/docs/contributing"><div class="hub-body"><div class="hub-title">How to Contribute</div><p class="hub-desc">Kotlin Multiplatform dev setup, project layout, code style and the PR flow.</p><span class="hub-link">Contribute →</span></div></a>
                <a class="hub-card" href="/blogs"><div class="hub-body"><div class="hub-title">Blog</div><p class="hub-desc">Deep dives on ISO 8583, EMV and payment cryptography.</p><span class="hub-link">Read →</span></div></a>
                <a class="hub-card" href="https://github.com/hpkaushik121/Iso8583studio"><div class="hub-body"><div class="hub-title">GitHub</div><p class="hub-desc">Source code, issues and discussions. Apache-licensed and open source.</p><span class="hub-link">Star the repo →</span></div></a>
                <a class="hub-card" href="https://github.com/hpkaushik121/Iso8583studio/releases"><div class="hub-body"><div class="hub-title">Downloads</div><p class="hub-desc">Latest releases for Windows, macOS and Linux.</p><span class="hub-link">Get the studio →</span></div></a>
                <a class="hub-card" href="https://github.com/users/hpkaushik121/projects/1"><div class="hub-body"><div class="hub-title">Roadmap</div><p class="hub-desc">What's shipping next across simulators and tools.</p><span class="hub-link">Follow along →</span></div></a>
            </div>
        </ui-section>
</main>

<aside class="pro-nudge"><span class="pn-tag">✦ Pro</span><p>Testing with a team, or certifying with a scheme? Pro raises the CPS ceiling, unlocks the full algorithm set and deep simulator tweaks, plus hosted endpoints and priority support.</p><a href="/pro">Register for Pro →</a></aside>`,
})
export class DocsPage {}
