import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { UiIcon } from '../../../ui';
import { SgBtn, SgFld, SgGlass, SgTab, SgTogRow } from './sg-core';
import {
  AG_AID, AG_ATR, AG_CFG, AG_CFG_TABS, AG_PORT, AG_PROFILE, AG_SESSION_TABS, AG_WIN, AgFirmware, AgL3, AgSession,
  AgSniff, AgTrace,
} from './apdu-glass';

/**
 * The five tabs of APDU Simulator Configuration as glass screens — Mode &
 * Transport (the three roles and the transport that follows the pick), Card
 * Profile, Terminal Profile, Risk & Behavior and Test Plans — plus the screen
 * registry and <ag-screen>, which draws any APDU Simulator screen by id.
 *
 * Styles: styles/bundles/_sims-pos.css (sg- primitives, ag- for these screens).
 */

// ---------- Mode & Transport ----------

const MODES: readonly (readonly [string, string, string])[] = [
  ['globe', 'Loopback (software-only)', 'EMV runtime in-process against the active card profile. No hardware. Ideal for developing test plans and personalization.'],
  ['broadcast', 'Reader (PC/SC) — drive a real card', 'Studio acts as the terminal. Drives a physical card via a PC/SC reader (e.g. ACS ACR39U-I1). Card profile is informational only — the real card is the source of truth.'],
  ['usb', 'Card emulator (USB-CDC to STM32)', 'Studio pushes APDU responses to the Nucleo-L432KC firmware over USB-CDC. The firmware emulates a contact card on the XCRFID pinboard, readable by an external POS terminal.'],
];

@Component({
  selector: 'ag-mode',
  imports: [UiIcon, SgBtn, SgFld],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'sg-c' },
  template: `
    <div class="sg-split">
      <div class="sg-card sg-layer sg-rail sg-wide" [style.--d]="160">
        <div class="sg-rail-top"><div class="sg-h"><ui-icon name="sim-card" class="sg-cb" [style.--s]="14" />APDU Simulator</div><ui-icon name="flask" class="sg-cp" [style.--s]="13" /></div>
        <div class="sg-rail-bar"><span>PROFILES · 1</span><span class="sg-rail-tools"><ui-icon name="plus" class="sg-cb" /><ui-icon name="upload-simple" class="sg-cb" /><ui-icon name="download-simple" class="sg-cb" /><ui-icon name="trash" class="sg-cr" /></span></div>
        <div class="sg-rail-item sg-tile" [style.--d]="300"><ui-icon name="sim-card" [style.--s]="13" /><span>Card-1</span><ui-icon name="check-circle" [style.--s]="13" /></div>
        <div class="sg-grow"></div>
        <sg-btn class="sg-btn--block" kind="ghost" icon="floppy-disk">Save All Configurations</sg-btn>
        <sg-btn class="sg-btn--launch" kind="teal" icon="sim-card">Launch APDU Simulator</sg-btn>
      </div>
      <div class="sg-stack">
        <div class="sg-card sg-layer sg-pad" [style.--d]="200">
          <div class="sg-cap sg-cap--icon"><ui-icon name="code" [style.--s]="11" />Operating mode</div>
          <div class="sg-sub sg-cap-sub">Pick the role this simulator plays. The transport section below adapts.</div>
          <div class="ag-opts">
            @for (m of modes; track m[1]; let i = $index) {
              <div class="sg-opt ag-opt ag-opt--icon sg-tile" [class.on]="sel() === i" [class.hov]="hov() === i" [style.--d]="260 + i * 60">
                <ui-icon [name]="m[0]" class="ag-opt-i" [style.--s]="14" />
                <span class="ag-opt-t"><span class="ag-opt-n">{{ m[1] }}</span><span class="ag-opt-d sg-wide">{{ m[2] }}</span></span>
                <i class="ag-radio" [class.on]="sel() === i"></i>
              </div>
            }
          </div>
        </div>
        @switch (sel()) {
          @case (2) {
            <div class="sg-card sg-layer sg-fresh sg-pad" [style.--d]="420">
              <div class="sg-cap sg-cap--icon"><ui-icon name="usb" [style.--s]="11" />Transport — STM32 / USB-CDC</div>
              <div class="sg-sub sg-cap-sub">Pick the serial port the firmware enumerated on.</div>
              <div class="ag-pickrow"><sg-fld label="Port" [value]="f(7) ? port : ''" ph="No port selected" [caret]="true" [on]="t() === 7" /><sg-btn class="sg-btn--tall" kind="ghost" icon="arrows-clockwise" [press]="t() === 6">Rescan</sg-btn></div>
              <div class="ag-stm"><sg-fld label="Baud rate" [value]="f(8) ? '115200' : ''" [caret]="true" [on]="t() === 8" /><sg-fld label="ATR override (hex, blank = use card profile)" [value]="f(8) ? atr : ''" ph="3B…" [on]="t() === 8" /></div>
              <div class="sg-sub ag-mode-note">On macOS the device shows up as /dev/cu.usbmodemXXXX after the firmware boots. The default baud is 115200 but the firmware framing is binary — baud only affects throughput, not protocol.</div>
            </div>
          }
          @case (1) {
            <div class="sg-card sg-layer sg-fresh sg-pad" [style.--d]="0">
              <div class="sg-cap sg-cap--icon"><ui-icon name="broadcast" [style.--s]="11" />Transport — PC/SC</div>
              <div class="sg-sub sg-cap-sub">Pick the reader holding the card. Studio drives it as the terminal.</div>
              <div class="ag-pickrow"><sg-fld label="Reader" value="ACS ACR39U ICC Reader 00 00" [caret]="true" [on]="t() === 4" /><sg-btn class="sg-btn--tall" kind="ghost" icon="arrows-clockwise">Rescan</sg-btn></div>
              <div class="sg-g3 ag-mt10"><sg-fld label="Share mode" value="Exclusive" [caret]="true" /><sg-fld label="Protocol" value="T=0 | T=1 (auto)" [caret]="true" /><sg-fld label="Warm reset before session" value="Yes" [caret]="true" /></div>
            </div>
          }
          @default {
            <div class="sg-card sg-layer sg-fresh sg-pad" [style.--d]="0">
              <div class="sg-cap sg-cap--icon"><ui-icon name="globe" [style.--s]="11" />Transport — in-process</div>
              <div class="sg-sub sg-cap-sub">No hardware. The EMV runtime answers in-process, on the session thread.</div>
              <div class="ag-loop"><sg-fld label="Runtime" value="Studio EMV kernel · contact T=0" [caret]="true" /><sg-fld label="Simulated response delay" value="0 ms" /></div>
            </div>
          }
        }
      </div>
    </div>
  `,
})
export class AgMode {
  readonly t = input.required<number>();

  protected readonly modes = MODES;
  protected readonly port = AG_PORT;
  protected readonly atr = AG_ATR.replace(/ /g, '');

  /** The selected role: the card emulator at rest, then a tour through loopback and the reader. */
  protected readonly sel = computed(() => {
    const t = this.t();
    return t < 0 || t >= 6 || t === 0 ? 2 : t <= 3 ? 0 : 1;
  });
  protected readonly hov = computed(() => (this.t() === 0 ? 0 : this.t() === 3 ? 1 : -1));

  /** Filled before the loop starts and from step k on. */
  protected f(k: number): boolean {
    return this.t() < 0 || this.t() >= k;
  }
}

// ---------- Card Profile ----------

/** [label, value] — a row without a value is a group heading; the third entry indents it. */
const SUMMARY: readonly (readonly [string, string?, boolean?])[] = [
  ['Scheme', 'MASTERCARD'], ['ATR', '3B6500002063CB6800'], ['Application 1'], ['AID', AG_AID], ['Label', 'DEBIT MASTERCARD'],
  ['PAN', '5555555555554444'], ['Expiry', '291231'], ['CVN', '1'], ['Records', '1 record(s)'], ['Issuer key id', 'mc-imk-1'],
  ['Issuer keys: 1'], ['mc-imk-1', 'TDES_AC — UDK 33333333…', true],
];

@Component({
  selector: 'ag-profile',
  imports: [UiIcon, SgBtn, SgFld],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'sg-c' },
  template: `
    <div class="sg-card sg-layer sg-pad" [style.--d]="160">
      <div class="sg-cap sg-cap--icon"><ui-icon name="identification-card" [style.--s]="11" />Active card profile</div>
      <div class="sg-sub sg-cap-sub">The simulator emulates this profile when running.</div>
      <div class="ag-pickrow"><sg-fld label="Profile" [value]="filled() ? profile : ''" ph="Loading profiles…" [caret]="true" [on]="t() === 1" [mono]="true" /><sg-btn class="sg-btn--tall" kind="ghost" icon="arrows-clockwise" [press]="t() === 0">Refresh</sg-btn></div>
      <div class="sg-acts ag-fw-acts"><sg-btn icon="pencil-simple" [press]="t() === 5">Edit profile</sg-btn><sg-btn kind="ghost" icon="copy">Clone &amp; personalize</sg-btn><sg-btn kind="ghost">New blank</sg-btn></div>
    </div>
    <div class="sg-card sg-layer sg-pad" [style.--d]="260">
      <div class="sg-cap sg-cap--icon"><ui-icon name="table" [style.--s]="11" />Profile summary</div>
      <div class="sg-sub sg-cap-sub">Key fields — full editor available above.</div>
      @for (row of summary; track row[0]; let i = $index) {
        @if (row[1] === undefined) {
          <div class="sg-kv-head sg-tile" [style.--d]="320 + i * 30">{{ row[0] }}</div>
        } @else {
          <div class="sg-kv sg-kv--140 sg-tile" [style.--d]="320 + i * 30">
            <span class="sg-kv-k" [class.sg-kv-k--in]="row[2]">{{ row[0] }}</span>
            @if (filled()) {<span class="sg-kv-v sg-kv-v--mono sg-kv-v--clip sg-line">{{ row[1] }}</span>} @else {<span class="sg-cf">—</span>}
          </div>
        }
      }
    </div>
  `,
})
export class AgProfile {
  readonly t = input.required<number>();

  protected readonly summary = SUMMARY;
  protected readonly profile = AG_PROFILE + '   (MASTERCARD)';
  protected readonly filled = computed(() => this.t() !== 0);
}

// ---------- Terminal Profile ----------

const CAPS = ['Manual key entry', 'Magnetic stripe', 'ICC contact', 'Plaintext PIN (ICC)', 'Enciphered PIN online', 'Signature', 'Enciphered PIN offline', 'No CVM required', 'SDA', 'DDA', 'CDA'];
/** [AID, scheme, accepted] */
const AIDS: readonly (readonly [string, string, boolean])[] = [
  ['A0000000041010', 'Mastercard', true], ['A0000000043060', 'Maestro', true], ['A0000000031010', 'Visa', false],
  ['A0000000032010', 'Visa Electron', false], ['A00000002501', 'Amex', false], ['A0000000651010', 'JCB', false],
];

@Component({
  selector: 'ag-terminal',
  imports: [UiIcon, SgFld],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'sg-c' },
  template: `
    <div class="sg-card sg-layer sg-pad" [style.--d]="160">
      <div class="sg-cap sg-cap--icon"><ui-icon name="storefront" [style.--s]="11" />Terminal identity</div>
      <div class="sg-sub sg-cap-sub">Who the card thinks it is talking to.</div>
      <div class="sg-g3">
        <sg-fld label="Terminal type (9F35)" [value]="s(0) ? '22 — attended · online capable · merchant' : ''" [caret]="true" [on]="t() === 0" />
        <sg-fld label="Terminal ID (9F1C)" [value]="s(1) ? 'TERM0001' : ''" [on]="t() === 1" />
        <sg-fld label="Merchant ID (9F16)" [value]="s(2) ? 'ISO8583STUDIO00' : ''" [on]="t() === 2" />
        <sg-fld label="Merchant category (9F15)" [value]="s(3) ? '5411 — Grocery' : ''" [caret]="true" [on]="t() === 3" />
        <sg-fld label="Country (9F1A)" [value]="s(4) ? '0840 — United States' : ''" [caret]="true" [on]="t() === 4" />
        <sg-fld label="Currency (5F2A · 5F36)" [value]="s(5) ? '0840 USD · exponent 2' : ''" [caret]="true" [on]="t() === 5" />
      </div>
    </div>
    <div class="ag-term-cols">
      <div class="sg-card sg-layer sg-pad" [style.--d]="260">
        <div class="sg-cap sg-cap--icon"><ui-icon name="sliders-horizontal" [style.--s]="11" />Capabilities</div>
        <div class="sg-sub sg-cap-sub">Capability bytes, with what they claim.</div>
        <div class="sg-g2">
          <sg-fld label="Terminal capabilities (9F33)" [value]="s(6) ? 'E0 F8 C8' : ''" [on]="t() === 6" [mono]="true" />
          <sg-fld label="Additional capabilities (9F40)" [value]="s(7) ? 'F0 00 F0 A0 01' : ''" [on]="t() === 7" />
        </div>
        <div class="ag-caps">
          @for (cap of caps; track cap; let i = $index) {<span class="sg-chip ag-cap sg-tile" [class.on]="s(6)" [style.--d]="360 + i * 25">{{ cap }}</span>}
        </div>
      </div>
      <div class="sg-card sg-layer sg-pad" [style.--d]="340">
        <div class="sg-cap sg-cap--icon"><ui-icon name="scales" [style.--s]="11" />Risk parameters</div>
        <div class="sg-sub sg-cap-sub">What decides offline, online or decline.</div>
        <div class="sg-g2">
          <sg-fld label="Floor limit (9F1B)" [value]="s(8) ? '00 00 03 E8 — 10.00' : ''" [on]="t() === 8" />
          <sg-fld label="TAC – Denial" [value]="s(9) ? '00 00 00 00 00' : ''" [on]="t() === 9" />
          <sg-fld label="TAC – Online" [value]="s(9) ? 'FC 50 BC F8 00' : ''" [on]="t() === 9" />
          <sg-fld label="TAC – Default" [value]="s(9) ? 'FC 50 BC A0 00' : ''" [on]="t() === 9" />
        </div>
        <div class="sg-g3 sg-g3--keep ag-mt10">
          <sg-fld label="Random selection threshold" [value]="s(10) ? '0.00' : ''" [on]="t() === 10" />
          <sg-fld label="Target %" [value]="s(10) ? '20' : ''" />
          <sg-fld label="Max target %" [value]="s(10) ? '99' : ''" />
        </div>
      </div>
    </div>
    <div class="sg-card sg-layer sg-pad" [style.--d]="420">
      <div class="sg-cap sg-cap--icon"><ui-icon name="list-checks" [style.--s]="11" />Supported AIDs</div>
      <div class="sg-sub sg-cap-sub">Applications the terminal will select. Partial match allowed.</div>
      <div class="ag-aids">
        @for (aid of aids; track aid[0]; let i = $index) {
          <div class="ag-aid sg-tile" [style.--d]="480 + i * 40"><span class="ag-aid-t"><span class="ag-aid-n">{{ aid[1] }}</span><span class="sg-m10">{{ aid[0] }}</span></span><i class="sg-tog" [class.on]="aid[2] ? s(0) : i === 2 && t() === 11"></i></div>
        }
      </div>
    </div>
  `,
})
export class AgTerminal {
  readonly t = input.required<number>();

  protected readonly caps = CAPS;
  protected readonly aids = AIDS;

  /** Filled from step k until the loop resets. */
  protected s(k: number): boolean {
    return this.t() >= k && this.t() < 12;
  }
}

// ---------- Risk & Behavior ----------

const POLICY: readonly (readonly [string, string])[] = [
  ['Follow terminal request', 'ARQC, TC or AAC as the terminal asks — the default for a compliant card.'],
  ['Always ARQC — force online', 'Every GENERATE AC returns an ARQC, whatever P1 says.'],
  ['Always AAC — decline', 'Every GENERATE AC declines. For decline-path testing.'],
  ['TC when under floor limit', 'Approve offline below the terminal floor limit, otherwise ARQC.'],
];

@Component({
  selector: 'ag-risk',
  imports: [UiIcon, SgFld, SgTogRow],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'sg-c' },
  template: `
    <div class="ag-risk-a">
      <div class="sg-card sg-layer sg-pad" [style.--d]="160">
        <div class="sg-cap sg-cap--icon"><ui-icon name="lightning" [style.--s]="11" />Cryptogram policy</div>
        <div class="sg-sub sg-cap-sub">What GENERATE AC returns.</div>
        <div class="ag-opts">
          @for (p of policy; track p[0]; let i = $index) {
            <div class="sg-opt ag-opt ag-opt--compact sg-tile" [class.on]="sel() === i" [class.hov]="t() === 2 && i === 1" [style.--d]="220 + i * 50">
              <span class="ag-opt-t"><span class="ag-opt-n">{{ p[0] }}</span><span class="ag-opt-d sg-wide">{{ p[1] }}</span></span>
              <i class="ag-radio" [class.on]="sel() === i"></i>
            </div>
          }
        </div>
      </div>
      <div class="sg-card sg-layer sg-pad" [style.--d]="240">
        <div class="sg-cap sg-cap--icon"><ui-icon name="keyboard" [style.--s]="11" />PIN &amp; counters</div>
        <div class="sg-sub sg-cap-sub">Offline PIN and the counters the card reports.</div>
        <sg-togrow icon="lock" heading="Offline PIN (VERIFY)" sub="Plaintext and enciphered PIN by ICC" [on]="s(0)" [style.--d]="300" />
        <div class="sg-g2 ag-mt4">
          <sg-fld label="Reference PIN" [value]="s(0) ? '••••' : ''" [on]="t() === 0" />
          <sg-fld label="PIN try counter" [value]="s(1) ? '3' : ''" [on]="t() === 1" />
          <sg-fld label="ATC start (9F36)" [value]="s(1) ? '00 2E' : ''" />
          <sg-fld label="LCOL · UCOL (9F14 · 9F23)" [value]="s(1) ? '03 · 05' : ''" />
        </div>
      </div>
    </div>
    <div class="ag-risk-b">
      <div class="sg-card sg-layer sg-pad" [style.--d]="320">
        <div class="sg-cap sg-cap--icon"><ui-icon name="shield-check" [style.--s]="11" />Issuer authentication</div>
        <div class="sg-sub sg-cap-sub">Second GENERATE AC and scripts.</div>
        <sg-togrow icon="check-circle" heading="Verify ARPC" sub="EXTERNAL AUTHENTICATE · CVN 1 method" [on]="s(2)" [style.--d]="380" />
        <sg-togrow icon="file-code" heading="Accept issuer scripts" sub="Tags 71 and 72 · PIN change, unblock, counters" [on]="s(6)" [style.--d]="420" />
        <sg-togrow icon="hand" heading="Require ARPC before TC" sub="Decline the second GENERATE AC without it" [style.--d]="460" />
      </div>
      <div class="sg-card sg-layer sg-pad ag-fault" [style.--d]="400">
        <div class="sg-cap sg-cap--icon"><ui-icon name="bug" [style.--s]="11" />Fault injection</div>
        <div class="sg-sub sg-cap-sub">Off by default. Applies to the next session.</div>
        <div class="sg-g2">
          <sg-fld label="Response delay" [value]="s(7) ? '120 ms' : '0 ms'" [on]="t() === 7" />
          <sg-fld label="Wrong SW on command" [value]="s(8) ? 'READ RECORD · SFI 2' : ''" ph="None" [caret]="true" [on]="t() === 8" />
        </div>
        <div class="ag-mt4">
          <sg-togrow icon="warning-circle" heading="Return 6A82 on that command" sub="Record not found instead of the data" [on]="s(8)" [style.--d]="480" />
          <sg-togrow icon="scissors" heading="Tear at GENERATE AC" sub="Drop the response half-way through" [style.--d]="520" />
          <sg-togrow icon="shuffle" heading="Corrupt one cryptogram byte" sub="ARQC fails issuer verification" [on]="t() === 10" [style.--d]="560" />
        </div>
      </div>
    </div>
  `,
})
export class AgRisk {
  readonly t = input.required<number>();

  protected readonly policy = POLICY;
  protected readonly sel = computed(() => (this.t() >= 3 && this.t() <= 5 ? 1 : 0));

  /** On from step k until the loop resets. */
  protected s(k: number): boolean {
    return this.t() >= k && this.t() < 11;
  }
}

// ---------- Test Plans ----------

const PLANS: readonly (readonly [string, string])[] = [
  ['Contact purchase · online ARQC', '7 steps · Mastercard'], ['Offline TC under floor limit', '6 steps · Mastercard'],
  ['PIN try counter exhaustion', '9 steps · any scheme'], ['Unknown AID → 6A82', '2 steps · any scheme'],
  ['Issuer script · PIN unblock', '8 steps · Mastercard'],
];
/** [step, command, expected data] */
const STEPS: readonly (readonly [string, string, string])[] = [
  ['SELECT PSE', '00 A4 04 00 0E …', '6F present'], ['READ RECORD · PSE', '00 B2 01 0C 00', '4F = A0000000041010'],
  ['SELECT AID', '00 A4 04 00 07 …', '9F38 present'], ['GET PROCESSING OPTIONS', '80 A8 00 00 05 …', '82 = 18 00'],
  ['READ RECORD · SFI 1', '00 B2 01 0C 00', '5A = 5555…4444'], ['VERIFY', '00 20 00 80 08 …', '—'],
  ['GENERATE AC', '80 AE 80 00 2B …', '9F27 = 80 · 9F26 8 bytes'],
];

@Component({
  selector: 'ag-plans',
  imports: [UiIcon, SgBtn],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'sg-c' },
  template: `
    <div class="ag-plans">
      <div class="sg-list sg-card sg-layer sg-wide" [style.--d]="160">
        <div class="sg-list-h"><span class="sg-list-t">Test Plans</span><span class="sg-list-s">Stored with Card-1 · 5 plans</span></div>
        @for (plan of plans; track plan[0]; let i = $index) {
          <div class="sg-item sg-tile" [class.on]="i === 0" [style.--d]="260 + i * 40">
            @if (i === 0) {<ui-icon name="play-circle" class="sg-cb" [style.--s]="14" />} @else {<ui-icon name="file-text" class="sg-cm" [style.--s]="14" />}
            <span class="sg-item-t"><b>{{ plan[0] }}</b><small>{{ plan[1] }}</small></span>
          </div>
        }
        <div class="sg-list-f"><sg-btn class="sg-btn--flex" kind="ghost" icon="plus">New</sg-btn><sg-btn class="sg-btn--flex" kind="ghost" icon="upload-simple">Import</sg-btn></div>
      </div>
      <div class="sg-stack">
        <div class="sg-card sg-layer ag-plan" [style.--d]="200">
          <span class="ag-head-l">
            <span class="sg-ico ag-plan-i"><ui-icon name="play-circle" [style.--s]="16" /></span>
            <span class="ag-head-t"><span class="ag-head-n">Contact purchase · online ARQC</span><span class="sg-sub">7 steps · {{ profile }} · 12.50 USD · @if (done()) {<b class="sg-cg">7 / 7 passed · 296 ms</b>} @else {<span>{{ progress() }}</span>}</span></span>
          </span>
          <span class="sg-acts">
            @if (done()) {
              <sg-btn kind="ghost" icon="check" [press]="t() === 0" [dim]="running()">Passed</sg-btn>
            } @else {
              <sg-btn icon="play" [press]="t() === 0" [dim]="running()">Run plan</sg-btn>
            }
            <sg-btn kind="ghost" icon="copy">Duplicate</sg-btn>
          </span>
        </div>
        <div class="sg-card sg-layer ag-steps" [style.--d]="300">
          <div class="sg-trow sg-trow--head ag-step"><span>#</span><span>Step</span><span class="sg-wide">Command</span><span>Expect SW</span><span class="sg-wide">Expect data</span><span></span></div>
          @for (step of steps; track step[0]; let i = $index) {
            <div class="sg-trow ag-step sg-tile" [class.hov]="running() && i === passed()" [class.ok]="i < passed()" [style.--d]="360 + i * 40">
              <span class="sg-m10">{{ i + 1 }}</span>
              <b>{{ step[0] }}</b>
              <span class="sg-m10 sg-wide">{{ step[1] }}</span>
              <span class="ag-sw" [class.sg-cg]="i < passed()">90 00</span>
              <span class="sg-m10 sg-wide">{{ step[2] }}</span>
              <span class="ag-step-s">
                @if (i < passed()) {
                  <ui-icon name="check-circle" class="sg-cg" [style.--s]="14" />
                } @else if (running() && i === passed()) {
                  <i class="sg-caret"></i>
                } @else {
                  <ui-icon name="circle" class="sg-cf" />
                }
              </span>
            </div>
          }
        </div>
      </div>
    </div>
  `,
})
export class AgPlans {
  readonly t = input.required<number>();

  protected readonly plans = PLANS;
  protected readonly steps = STEPS;
  protected readonly profile = AG_PROFILE;

  protected readonly passed = computed(() => {
    const t = this.t();
    return t < 1 || t > 10 ? 0 : Math.min(t, 7);
  });
  protected readonly running = computed(() => this.t() >= 1 && this.t() < 8);
  protected readonly done = computed(() => this.passed() === 7);
  protected readonly progress = computed(() => (this.running() ? this.passed() + ' / 7 passed…' : 'not run yet'));
}

// ---------- the screen registry ----------

interface ScreenSpec {
  title: string;
  sub: string;
  tabs: readonly SgTab[];
  tab: number;
  aria: string;
  steps: number;
  period: number;
  settled: number;
}

export const APDU_SCREENS: Record<string, ScreenSpec> = {
  session: { title: AG_WIN, sub: 'Card Session', tabs: AG_SESSION_TABS, tab: 0, steps: 15, period: 1000, settled: 8, aria: 'The Card Session tab of the APDU Simulator window: Connect, Reset and Clear, the Hold next APDU switch, status, phase, last AID and exchange counters, and the four panes showing a contact purchase — SELECT PSE, READ RECORD, SELECT AID, GET PROCESSING OPTIONS, READ RECORD, VERIFY and GENERATE AC — formatted and raw' },
  trace: { title: AG_WIN, sub: 'Trace Log', tabs: AG_SESSION_TABS, tab: 1, steps: 12, period: 900, settled: 8, aria: 'The Trace Log tab: the L2 phases ticking off above the seven exchanges of the session, each with its phase, command, raw bytes, status word and timing' },
  sniff: { title: AG_WIN, sub: 'Wire Sniff', tabs: AG_SESSION_TABS, tab: 3, steps: 22, period: 650, settled: 16, aria: 'The Wire Sniff tab: the USB-CDC frames between Studio and the STM32 firmware — hello, ATR, reset, then every command in and response out with sizes and timing' },
  l3: { title: AG_WIN, sub: 'L3 Report', tabs: AG_SESSION_TABS, tab: 4, steps: 16, period: 700, settled: 12, aria: 'The L3 Report tab: ten phase checks of the last transaction, eight passed, offline data authentication skipped and online processing noted' },
  firmware: { title: AG_WIN, sub: 'Firmware', tabs: AG_SESSION_TABS, tab: 5, steps: 12, period: 900, settled: 7, aria: 'The Firmware tab: the NUCLEO-L432KC board, its contact pin map, installed and latest firmware versions, and an update flashed and verified' },
  mode: { title: AG_CFG, sub: 'Mode & Transport', tabs: AG_CFG_TABS, tab: 0, steps: 10, period: 1100, settled: 8, aria: 'APDU Simulator Configuration on the Mode & Transport tab: the profile rail, the three operating modes — Loopback, Reader (PC/SC) and Card emulator (USB-CDC to STM32) — and the transport card for the selected mode with port, baud rate and ATR override' },
  profile: { title: AG_CFG, sub: 'Card Profile', tabs: AG_CFG_TABS, tab: 1, steps: 9, period: 1000, settled: 5, aria: 'The Card Profile tab: the active profile MasterCard Debit (Test) with Refresh, Edit profile, Clone & personalize and New blank, and the profile summary — scheme, ATR, application 1 with AID, label, PAN, expiry, CVN, records and issuer key' },
  terminal: { title: AG_CFG, sub: 'Terminal Profile', tabs: AG_CFG_TABS, tab: 2, steps: 13, period: 800, settled: 11, aria: 'The Terminal Profile tab: terminal identity, capability bytes with their decoded meaning, floor limit and terminal action codes, and the supported AIDs' },
  risk: { title: AG_CFG, sub: 'Risk & Behavior', tabs: AG_CFG_TABS, tab: 3, steps: 12, period: 900, settled: 7, aria: 'The Risk & Behavior tab: cryptogram policy, offline PIN and counters, issuer authentication toggles and fault injection switches' },
  plans: { title: AG_CFG, sub: 'Test Plans', tabs: AG_CFG_TABS, tab: 4, steps: 13, period: 700, settled: 9, aria: 'The Test Plans tab: five built-in plans, the seven steps of the online ARQC purchase with expected status words and data, and a run passing step by step' },
};

/**
 * AgScreen — one APDU Simulator screen in its window; `screen` is a key of
 * APDU_SCREENS. `active` is false while the screen is a hidden tab panel.
 */
@Component({
  selector: 'ag-screen',
  imports: [SgGlass, AgSession, AgTrace, AgSniff, AgL3, AgFirmware, AgMode, AgProfile, AgTerminal, AgRisk, AgPlans],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'sg-host' },
  template: `
    @let s = spec();
    <sg-glass #g [heading]="s.title" [sub]="s.sub" [tabs]="s.tabs" [tab]="s.tab" app="apdu-simulator" badge="APDU"
              [aria]="s.aria" [steps]="s.steps" [period]="s.period" [settled]="s.settled" [active]="active()">
      @switch (screen()) {
        @case ('session') { <ag-session [t]="g.t()" /> }
        @case ('trace') { <ag-trace [t]="g.t()" /> }
        @case ('sniff') { <ag-sniff [t]="g.t()" /> }
        @case ('l3') { <ag-l3 [t]="g.t()" /> }
        @case ('firmware') { <ag-firmware [t]="g.t()" /> }
        @case ('mode') { <ag-mode [t]="g.t()" /> }
        @case ('profile') { <ag-profile [t]="g.t()" /> }
        @case ('terminal') { <ag-terminal [t]="g.t()" /> }
        @case ('risk') { <ag-risk [t]="g.t()" /> }
        @case ('plans') { <ag-plans [t]="g.t()" /> }
      }
    </sg-glass>
  `,
})
export class AgScreen {
  readonly screen = input.required<string>();
  readonly active = input(true);

  protected readonly spec = computed(() => APDU_SCREENS[this.screen()]);
}
