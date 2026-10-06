import { ChangeDetectionStrategy, Component, ElementRef, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AccordionItem, UiAccordion, UiIcon, UiReveal, UiSectionHeading } from '../../ui';
import {
  ArtName, SolArt, SolBenefit, SolBenefits, SolBento, SolClose, SolHero, SolLive, SolMark, SolStrip, solCycle,
} from '../solutions';
import { SitePage } from './site-page';

interface Service { n: string; kicker: string; img: ArtName; title: string; desc: string; items: string[]; }
interface Area { title: string; blurb: string; tag: string; image: ArtName; description: string; points: string[]; }

const SERVICES: Service[] = [
  { n: '01', kicker: 'Platform engineering', img: 'stack', title: 'Android Kernel Customization', desc: 'Adapt Android to your smart POS platform, from system builds and access policies to device lockdown.', items: ['Device-specific AOSP builds', 'SELinux policy integration', 'Payment device configuration'] },
  { n: '02', kicker: 'Board to application', img: 'nfc', title: 'Hardware Integration', desc: 'Connect payment peripherals to the platform and work through device bring-up and integration testing.', items: ['NFC and secure elements', 'MSR, printers and PIN pads', 'Hardware interface bring-up'] },
  { n: '03', kicker: 'Peripheral control', img: 'driver', title: 'Device Driver Development', desc: 'Develop and maintain the Linux drivers that connect your payment hardware to the rest of the system.', items: ['Custom peripheral drivers', 'Embedded platform support', 'Device performance tuning'] },
];

const AREAS: Area[] = [
  { title: 'Android internals', blurb: 'HALs, services and system apps', tag: 'Android platform', image: 'stack', description: 'Connect hardware-facing interfaces to the Android framework, with platform services and system applications tailored to payment devices.', points: ['HALs', 'HIDL / AIDL', 'System services'] },
  { title: 'Linux kernel', blurb: 'Drivers, power and real-time patches', tag: 'Device & system layer', image: 'driver', description: 'Develop peripheral drivers and adapt power management and real-time behavior to the needs of an embedded payment platform.', points: ['Custom drivers', 'Power management', 'Real-time patches'] },
  { title: 'ARM architecture', blurb: 'TrustZone, secure boot and TEE', tag: 'Trusted computing', image: 'chip', description: 'Work with ARM-based platforms to integrate secure boot, trusted execution environments and the boundary between secure and normal worlds.', points: ['TrustZone', 'Secure boot', 'TEE integration'] },
  { title: 'Communication protocols', blurb: 'Card, peripheral and network links', tag: 'Peripheral connectivity', image: 'nfc', description: 'Bring card interfaces, contactless controllers, serial peripherals and network connections into a coherent device communication layer.', points: ['ISO 7816', 'NFC / ISO 14443', 'RS232 · USB · TCP/IP'] },
  { title: 'Security implementation', blurb: 'Keys, storage and attestation', tag: 'Platform protection', image: 'shield', description: 'Integrate key provisioning, protected storage and device attestation while aligning the design with payment hardware security requirements.', points: ['Key injection', 'Secure storage', 'PCI PTS alignment'] },
  { title: 'Real-time systems', blurb: 'Predictable transaction paths', tag: 'Timing & predictability', image: 'terminal', description: 'Engineer transaction paths around defined timing budgets, with attention to scheduling, peripheral response and predictable system behavior.', points: ['Latency budgets', 'Deterministic paths', 'Timing analysis'] },
];

const BENEFITS: SolBenefit[] = [
  ['01', 'Hardware optimization', 'Tune the platform to make effective use of the processor, memory and peripherals available on your device.'],
  ['02', 'Security integration', 'Integrate platform security into the system architecture, device interfaces and operating policies.'],
  ['03', 'Performance tuning', 'Measure and improve boot time, contactless interaction and peripheral response for your target device.'],
  ['04', 'Custom solutions', 'Build around your board and product requirements, with interfaces that fit your payment application.'],
  ['05', 'Scalable architecture', 'Design reusable platform components for related hardware models and future device generations.'],
  ['06', 'Development support', 'Keep the platform maintainable through security patches, operating-system updates and ongoing engineering.'],
];

const FAQ: AccordionItem[] = [
  { q: 'What does an engagement usually start from?', a: 'A board and a target product. We work through bring-up of the interfaces that matter first — card reader, contactless controller, PIN pad, printer — then build the platform around them.' },
  { q: 'Do you build the Android platform itself?', a: 'Yes. Device-specific AOSP builds, HAL and HIDL/AIDL work, SELinux policy, platform services and device lockdown for a payment terminal rather than a general-purpose phone.' },
  { q: 'Can you work with our existing kernel tree?', a: 'That is the common case. We take the vendor tree, add or maintain the peripheral drivers, and adapt power management and real-time behaviour to the timing budgets a transaction needs.' },
  { q: 'How does this relate to EMV certification?', a: 'Kernel engineering and certification usually run together: the L2 kernel and the platform it sits on are developed here, then taken through pre-certification and lab submission on the EMV certification service.' },
  { q: 'What about secure boot, TEE and key injection?', a: 'We integrate secure boot, TrustZone and trusted execution, protected storage, key provisioning and attestation, and align the design with payment hardware security requirements such as PCI PTS.' },
  { q: 'What do we get at the end?', a: 'A maintainable platform: source, build configuration, driver and integration documentation, plus ongoing support for security patches, OS updates and the next hardware revision.' },
];

const MARKS: SolMark[] = [
  ['device-mobile', 'Android'], ['cpu', 'ARM'], ['hard-drive', 'Linux'], ['broadcast', 'NFC'],
  ['sim-card', 'Secure element'], ['printer', 'Peripherals'], ['lock-key', 'TEE'], ['timer', 'Real-time'],
  ['wrench', 'Bring-up'],
];

@Component({
  selector: 'page-kernel',
  imports: [
    RouterLink, UiAccordion, UiIcon, UiReveal, UiSectionHeading,
    SolArt, SolBenefits, SolBento, SolClose, SolHero, SolLive, SolStrip,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  hostDirectives: [SitePage],
  host: { class: 'static-page page-kernel' },
  template: `
    <sol-hero sect="Engineering Services" crumb="Kernel development" eyebrow="EMV L2 · Android · Embedded Linux"
              heading="Android and Linux platforms for payment terminals"
              sub="Build the software inside your payment device: EMV processing, Android platforms, Linux drivers and hardware integration. From the first boot to the next tap."
              cta="Discuss your board" [headingMax]="760" [subMax]="650" [padBottom]="46" [explode]="640">
      <a solHeroCta class="btn btn--secondary" routerLink="/kernel" fragment="expertise">Explore the engineering layers<ui-icon name="arrow-down" [size]="16" /></a>
      <div class="sol-kn-stage" role="img"
           aria-label="Exploded payment platform stack — platform, kernel and hardware layers — wired to an NFC module and a smart POS terminal">
        <div class="sol-kn-ambient"></div>
        <div class="sol-kn-floor"></div>
        <svg class="sol-kn-wires" viewBox="0 0 720 520" preserveAspectRatio="none" aria-hidden="true">
          <path class="sol-kn-track" d="M468 198H597Q612 198 612 184V132" />
          <path class="sol-kn-track" d="M136 404H185Q200 404 200 390V376" />
          <path class="sol-kn-flow" d="M468 198H597Q612 198 612 184V132" />
          <path class="sol-kn-flow sol-kn-flow--b" d="M200 376V390Q200 404 185 404H136" />
        </svg>
        <div class="sol-kn-obj sol-kn-o-stack"><img solArt="stack" priority sizes="(max-width: 720px) 62vw, 446px"></div>
        <div class="sol-kn-obj sol-kn-o-nfc"><img solArt="nfc" eager sizes="(max-width: 720px) 16vw, 115px"><span>NFC</span></div>
        <div class="sol-kn-obj sol-kn-o-term"><img solArt="terminal" eager sizes="(max-width: 720px) 15vw, 108px"><span>SMART POS</span></div>
        <div class="sol-kn-scan" aria-hidden="true"></div>
        <div class="sol-kn-layer sol-kn-layer--platform"><i></i>PLATFORM</div>
        <div class="sol-kn-layer sol-kn-layer--kernel"><i></i>KERNEL</div>
        <div class="sol-kn-layer sol-kn-layer--hardware"><i></i>HARDWARE</div>
        <span class="sol-kn-caption">ENGINEERED FROM THE INSIDE</span>
        <span class="sol-kn-marker">EMV · ANDROID · LINUX · ARM</span>
      </div>
    </sol-hero>

    <sol-strip [tight]="true" caption="AOSP · SELinux · Linux drivers · TrustZone · ISO 7816 · ISO 14443" [marks]="marks" />

    <section class="sol-sec" id="services" data-sect="Kernel Development Services" uiReveal [uiRevealDelay]="250" solLive>
      <ui-section-heading heading="Kernel, hardware and drivers for your board"
                          sub="Bring the operating system, device interfaces and payment peripherals together as one engineered platform." />
      <div class="sol-grid3">
        @for (s of services; track s.n; let i = $index) {
          <sol-bento class="sol-kn-card ds-item" [style.--d]="500 + i * 120" [heading]="s.title" [badge]="s.n"
                     [desc]="s.desc" [glow]="i === 0" [radial]="i === 1">
            <div class="sol-mono sol-mono--teal sol-kicker">{{ s.kicker }}</div>
            <ul class="sol-checks">
              @for (t of s.items; track t) { <li><ui-icon name="check" [size]="15" />{{ t }}</li> }
            </ul>
            <div class="sol-kn-svc" aria-hidden="true"><img [solArt]="s.img" sizes="158px"></div>
          </sol-bento>
        }
      </div>
    </section>

    <section class="sol-sec" id="expertise" data-sect="Technical Expertise Areas" uiReveal [uiRevealDelay]="250" solLive>
      <ui-section-heading align="left" [heading]="areasHeading"
                          sub="Explore the engineering disciplines behind a payment device, from low-level interfaces to system services." />
      <div class="sol-picker sol-picker--even ds-hold">
        <div class="sol-picker-list">
          @for (a of areas; track a.title; let n = $index) {
            <button type="button" class="sol-pick" aria-controls="area-detail"
                    [class.is-active]="n === cycle.index()" [attr.aria-pressed]="n === cycle.index()"
                    (click)="cycle.pick(n)">
              <span class="sol-pick-n">{{ pad(n) }}</span>
              <span class="sol-pick-text">
                <span class="sol-pick-label">{{ a.title }}</span>
                <span class="sol-pick-blurb">{{ a.blurb }}</span>
              </span>
            </button>
          }
        </div>
        <div #scene class="sol-panel" id="area-detail">
          <span class="sol-panel-radial" aria-hidden="true"></span>
          <div class="sol-kn-art" aria-hidden="true">
            @for (a of areas; track a.title; let n = $index) {
              <img [solArt]="a.image" sizes="186px" [hidden]="n !== cycle.index()">
            }
          </div>
          <div class="sol-detail-copy">
            @for (a of areas; track a.title; let n = $index) {
              <div class="sol-step-copy sol-kn-area" [hidden]="n !== cycle.index()">
                <div class="sol-detail-head">
                  <span class="sol-mono sol-mono--teal">{{ a.tag }}</span>
                  <span class="sol-mono">{{ pad(n) }} / 06</span>
                </div>
                <h3>{{ a.title }}</h3>
                <p>{{ a.description }}</p>
                <div class="sol-tags sol-kn-points">
                  @for (p of a.points; track p) { <span class="sol-kn-point">{{ p }}</span> }
                </div>
              </div>
            }
          </div>
        </div>
      </div>
      <div class="sol-foot-row">
        <p class="sol-mono sol-mono--plain">Six disciplines, one platform team.</p>
        <button type="button" class="btn btn--secondary btn--sm sol-cycle-toggle" (click)="cycle.toggle()">
          @if (cycle.paused()) {
            <ui-icon name="play" [size]="14" />Play preview
          } @else {
            <ui-icon name="pause" [size]="14" />Pause preview
          }
        </button>
      </div>
    </section>

    <section class="sol-sec" id="benefits" data-sect="Kernel Development Benefits" uiReveal [uiRevealDelay]="250">
      <ui-section-heading align="left" heading="A platform your team can maintain after handover"
                          sub="Give your team control over the platform's performance, security and long-term evolution." />
      <sol-benefits [items]="benefits" />
    </section>

    <section class="sol-sec sol-sec--faq" id="faq" data-sect="faq" uiReveal [uiRevealDelay]="250">
      <ui-section-heading heading="Platform engineering questions" sub="Boards, Android builds, drivers and what we hand over." />
      <div class="sol-faq"><ui-accordion [items]="faq" [stagger]="110" [base]="500" /></div>
    </section>

    <sol-close sect="Ready to develop?" kicker="Your next device platform" cta="Discuss your board"
               text="Bring your board, your platform challenge or your next payment device. Let's work through the engineering together.">
      <p solCloseFoot class="pro-nudge sol-pro-line">
        <a routerLink="/pro">ISO8583Studio Pro</a>
        <span>Advanced testing tools, hosted endpoints and priority support for your team.</span>
      </p>
      <div class="sol-kn-close">
        <img class="sol-i-base" solArt="base" sizes="(max-width: 900px) 65vw, 370px">
        <img class="sol-i-stack" solArt="stack" sizes="(max-width: 900px) 39vw, 220px">
      </div>
    </sol-close>
  `,
})
export class KernelPage {
  protected readonly services = SERVICES;
  protected readonly areas = AREAS;
  protected readonly benefits = BENEFITS;
  protected readonly faq = FAQ;
  protected readonly marks = MARKS;
  protected readonly areasHeading = 'Six disciplines inside\na payment terminal';

  private readonly scene = viewChild<ElementRef<HTMLElement>>('scene');

  /** Areas advance every 5.2 s until the reader picks one or pauses. */
  protected readonly cycle = solCycle(
    { count: AREAS.length, interval: 5200 },
    () => this.scene()?.nativeElement,
  );

  protected pad(i: number): string {
    return String(i + 1).padStart(2, '0');
  }
}
