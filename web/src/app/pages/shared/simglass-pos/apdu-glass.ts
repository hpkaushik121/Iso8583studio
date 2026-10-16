import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { UiIcon } from '../../../ui';
import { SgBtn, SgPane, SgTab, sgHex, sgPairs } from './sg-core';

/**
 * Glass recreations of the APDU Simulator's session window: Card Session
 * (Connect, then a contact purchase from SELECT PSE to GENERATE AC in the four
 * panes), Trace Log, Wire Sniff, L3 Report and Firmware — each a scripted
 * loop. The configuration tabs and the screen registry are in
 * apdu-glass-cfg.ts.
 *
 * Styles: styles/bundles/_sims-pos.css (sg- primitives, ag- for these screens).
 */

export const AG_SESSION_TABS: readonly SgTab[] = [
  ['arrows-left-right', 'Card Session'], ['file-text', 'Trace Log'], ['play', 'Test Plans'], ['wifi-high', 'Wire Sniff'],
  ['seal-check', 'L3 Report'], ['cpu', 'Firmware'], ['gear', 'Settings'],
];
export const AG_CFG_TABS: readonly SgTab[] = [
  [null, 'Mode & Transport'], [null, 'Card Profile'], [null, 'Terminal Profile'], [null, 'Risk & Behavior'], [null, 'Test Plans'],
];
export const AG_WIN = 'APDU Simulator - Card-1';
export const AG_CFG = 'APDU Simulator Configuration';
export const AG_PORT = 'cu.usbmodem2103';
export const AG_PROFILE = 'MasterCard Debit (Test)';
export const AG_AID = 'A0000000041010';
export const AG_ATR = '3B 9F 95 80 1F C7 80 31 E0 73 FE 21 1B 63 00 4C 45 54 4F 4E';

// ---------- the contact purchase every session screen replays: seven exchanges, formatted and raw ----------

const ARQC = sgHex('arqc', 16);
const IAD = '0110A00003220000000000000000000000FF';
const REC = '5A0855555555555544445F24032912315F3401018C219F02069F03069F1A0295055F2A029A039C019F37049F35019F45029F4C089F34038D0C910A8A0295059F37049F4C088E0E000000000000000041031E031F039F0D05B45084800C9F0E0500000000009F0F05B47084800C';

type Exchange = readonly [phase: string, name: string, cmd: readonly string[], rsp: readonly string[], rawCmd: string, rawRsp: string, ms: number];

const EX: readonly Exchange[] = [
  ['Selection', 'SELECT PSE',
    ['CLA INS P1 P2    00 A4 04 00', 'Lc               0E', 'Data             31 50 41 59 2E 53 59 53 2E 44 44 46 30 31', '                 ↳ "1PAY.SYS.DDF01"', 'Le               00'],
    ['6F    FCI Template', '  84    DF Name                    1PAY.SYS.DDF01', '  A5    FCI Proprietary Template', '    88    SFI of the Directory     01', '    5F2D  Language Preference      en', '    9F11  Issuer Code Table Index  01', 'SW    90 00   Success'],
    '00A404000E315041592E5359532E444446303100', '6F1E840E315041592E5359532E4444463031A50C8801015F2D02656E9F1101019000', 9],
  ['Selection', 'READ RECORD · PSE directory',
    ['CLA INS P1 P2    00 B2 01 0C', '                 ↳ SFI 1 · record 1', 'Le               00'],
    ['70    Record Template', '  61    Application Template', '    4F    ADF Name                 A0 00 00 00 04 10 10', '    50    Application Label        DEBIT MASTERCARD', '    87    Priority Indicator       01', 'SW    90 00   Success'],
    '00B2010C00', '7020611E4F07A000000004101050104445424954204D4153544552434152448701019000', 7],
  ['Selection', 'SELECT AID',
    ['CLA INS P1 P2    00 A4 04 00', 'Lc               07', 'Data             A0 00 00 00 04 10 10', '                 ↳ AID · Mastercard Debit', 'Le               00'],
    ['6F    FCI Template', '  84    DF Name                    A0 00 00 00 04 10 10', '  A5    FCI Proprietary Template', '    50    Application Label        DEBIT MASTERCARD', '    87    Priority Indicator       01', '    9F38  PDOL                     9F1A 02 · 9F35 01', 'SW    90 00   Success'],
    '00A4040007A000000004101000', '6F298407A0000000041010A51E50104445424954204D4153544552434152448701019F38069F1A029F35019000', 11],
  ['Initiate', 'GET PROCESSING OPTIONS',
    ['CLA INS P1 P2    80 A8 00 00', 'Lc               05', 'Data             83 03 08 40 22', '                 ↳ 9F1A 0840 · 9F35 22', 'Le               00'],
    ['77    Response Message Template', '  82    AIP                        18 00', '        ↳ CVM supported · terminal risk management', '  94    AFL                        08 01 01 00', '        ↳ SFI 1 · records 1–1 · none for ODA', 'SW    90 00   Success'],
    '80A8000005830308402200', '770A820218009404080101009000', 14],
  ['Read data', 'READ RECORD · SFI 1 rec 1',
    ['CLA INS P1 P2    00 B2 01 0C', '                 ↳ SFI 1 · record 1', 'Le               00'],
    ['70    Record Template', '  5A    PAN                        55 55 55 55 55 55 44 44', '  5F24  Expiry Date                29 12 31', '  5F34  PAN Sequence Number        01', '  8C    CDOL1                      9F02 06 9F03 06 9F1A 02 95 05 5F2A 02 9A 03 …', '  8D    CDOL2                      91 0A 8A 02 95 05 9F37 04 9F4C 08', '  8E    CVM List                   00 00 00 00 00 00 00 00 41 03 1E 03 1F 03', '  9F0D  IAC – Default              B4 50 84 80 0C', '  9F0E  IAC – Denial               00 00 00 00 00', '  9F0F  IAC – Online               B4 70 84 80 0C', 'SW    90 00   Success'],
    '00B2010C00', '706D' + REC + '9000', 12],
  ['CVM', 'VERIFY · plaintext PIN',
    ['CLA INS P1 P2    00 20 00 80', 'Lc               08', 'Data             24 •• •• FF FF FF FF FF', '                 ↳ PIN block format 2 · digits masked'],
    ['SW    90 00   PIN verified · try counter 3'],
    '002000800824••••FFFFFFFFFF', '9000', 6],
  ['Cryptogram', 'GENERATE AC · ARQC',
    ['CLA INS P1 P2    80 AE 80 00', '                 ↳ P1 80 · ARQC requested', 'Lc               2B', '9F02  Amount, Authorised         00 00 00 00 12 50', '9F03  Amount, Other              00 00 00 00 00 00', '9F1A  Terminal Country Code      08 40', '95    TVR                        00 00 00 80 00', '5F2A  Transaction Currency       08 40', '9A    Transaction Date           26 09 21', '9C    Transaction Type           00', '9F37  Unpredictable Number       1A 2B 3C 4D', '9F35  Terminal Type              22', '9F45  Data Authentication Code   00 00', '9F4C  ICC Dynamic Number         00 00 00 00 00 00 00 00', '9F34  CVM Results                41 03 02', 'Le               00'],
    ['77    Response Message Template', '  9F27  Cryptogram Information     80  ↳ ARQC', '  9F36  ATC                        00 2E', '  9F26  Application Cryptogram     ' + sgPairs(ARQC), '  9F10  Issuer Application Data    01 10 A0 00 03 22 00 00 00 00 …', 'SW    90 00   Success'],
    '80AE80002B000000001250000000000000084000000080000840260921001A2B3C4D220000000000000000000041030200', '77299F2701809F3602002E9F2608' + ARQC + '9F1012' + IAD + '9000', 38],
];
/** Seconds after Connect at which each exchange starts. */
const T0 = [1.204, 1.221, 1.236, 1.255, 1.277, 1.298, 1.402];

/** Card Session: Connect → the seven exchanges land in the four panes → Disconnect. */
@Component({
  selector: 'ag-session',
  imports: [UiIcon, SgBtn, SgPane],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'sg-c' },
  template: `
    <div class="sg-card sg-layer ag-strip" [style.--d]="160">
      <span class="ag-strip-l"><i class="sg-dot sg-led" [class.on]="on()"></i>@for (s of [status()]; track s) {<span class="sg-line" [class.sg-cg]="on()">{{ s }}</span>}<b class="sg-cx">Emulate</b>·<span>{{ link }}</span></span>
      <span class="sg-wide">{{ profile }} · MASTERCARD <b class="sg-cx">Exchanges: {{ n() }}</b></span>
    </div>
    <div class="sg-card sg-layer ag-ctl" [style.--d]="220">
      <span class="sg-acts">
        @if (on()) {
          <sg-btn kind="danger" icon="plugs" [press]="t() === 11">Disconnect</sg-btn>
        } @else {
          <sg-btn icon="play" [press]="t() === 0">Connect</sg-btn>
        }
        <sg-btn kind="ghost" icon="arrow-counter-clockwise" [dim]="!on()">Reset</sg-btn>
        <sg-btn kind="ghost" icon="x" [dim]="k() < 0">Clear</sg-btn>
      </span>
      <span class="ag-hold"><ui-icon name="lightning" class="sg-cf" /><i class="sg-tog"></i>Hold next APDU</span>
    </div>
    <div class="sg-card sg-layer ag-stats" [style.--d]="280">
      <span class="ag-stat"><span class="ag-stat-k">Status</span>@for (v of [status()]; track v) {<b class="sg-line ag-stat-v" [class.sg-cg]="on()">{{ v }}</b>}</span>
      <span class="ag-stat"><span class="ag-stat-k">Phase</span>@for (v of [phase()]; track v) {<b class="sg-line ag-stat-v">{{ v }}</b>}</span>
      <span class="ag-stat"><span class="ag-stat-k">Last AID</span>@for (v of [lastAid()]; track v) {<b class="sg-line ag-stat-v">{{ v }}</b>}</span>
      <span class="ag-stat"><span class="ag-stat-k">Exchanges</span>@for (v of [n()]; track v) {<b class="sg-line ag-stat-v">{{ v }}</b>}</span>
    </div>
    <div class="ag-panes">
      <sg-pane class="sg-pane--fmt" heading="Formatted Command" tone="req" icon="arrow-up-right" [count]="counts()[0]" [style.--d]="340">
        @for (e of shown(); track e.k) {
          <div class="sg-pre sg-line sg-cb ag-cmd">▸ {{ e.ex[1] }}</div>
          @for (line of e.ex[2]; track $index) {<div class="sg-pre sg-line" [style.--ld]="$index * 40">{{ line }}</div>}
        } @empty {
          <span class="sg-cf">(no command yet)</span>
        }
      </sg-pane>
      <sg-pane class="sg-pane--fmt" heading="Formatted Response" tone="rsp" icon="arrow-down-left" [count]="counts()[1]" [style.--d]="420">
        @for (e of shown(); track e.k) {
          <div class="ag-rsp">@for (line of e.ex[3]; track $index) {<div class="sg-pre sg-line" [style.--ld]="$index * 40">{{ line }}</div>}</div>
        } @empty {
          <span class="sg-cf">(no response yet)</span>
        }
      </sg-pane>
      <sg-pane class="sg-pane--raw" heading="Raw Command" tone="req" icon="code" [count]="counts()[2]" [style.--d]="500">
        @for (e of shown(); track e.k) {<div class="sg-wrapline sg-line">{{ e.ex[4] }}</div>}
      </sg-pane>
      <sg-pane class="sg-pane--raw" heading="Raw Response" tone="rsp" icon="file-text" [count]="counts()[3]" [style.--d]="580">
        @for (e of shown(); track e.k) {<div class="sg-wrapline sg-line">{{ e.ex[5] }}</div>}
      </sg-pane>
    </div>
  `,
})
export class AgSession {
  readonly t = input.required<number>();

  protected readonly link = AG_PORT + ' @ 115200';
  protected readonly profile = AG_PROFILE;

  protected readonly on = computed(() => this.t() >= 1 && this.t() < 12);
  /** Index of the exchange in the panes; -1 before the first. */
  protected readonly k = computed(() => {
    const t = this.t();
    return t >= 2 && t <= 8 ? t - 2 : t > 8 && t < 12 ? 6 : -1;
  });
  protected readonly n = computed(() => this.k() + 1);
  protected readonly shown = computed(() => (this.k() >= 0 ? [{ k: this.k(), ex: EX[this.k()] }] : []));
  protected readonly status = computed(() =>
    !this.on() ? 'Idle' : this.k() < 0 ? 'Connected' : this.k() === 6 ? 'Online' : 'In session');
  protected readonly phase = computed(() => (this.k() >= 0 ? EX[this.k()][0] : '—'));
  protected readonly lastAid = computed(() => (this.k() >= 2 ? AG_AID : '—'));
  /** Line and byte counters of the four panes. */
  protected readonly counts = computed(() => {
    const ex = this.k() >= 0 ? EX[this.k()] : null;
    return ex ? [ex[2].length + 1, ex[3].length, ex[4].length / 2, ex[5].length / 2] : [0, 0, 0, 0];
  });
}

// ---------- Trace Log: the L2 phases ticking off above the exchanges arriving one by one ----------

/** [label, exchanges needed before it is reached, outcome] */
const PHASES: readonly (readonly [string, number, ('skip' | 'wait')?])[] = [
  ['Selection', 1], ['Initiate', 4], ['Read data', 5], ['ODA', 5, 'skip'], ['Restrictions', 5], ['CVM', 6], ['TRM', 6],
  ['TAA', 7], ['Cryptogram', 7], ['Online', 7, 'wait'],
];

const TRACE = EX.map((e, i) => ({
  time: '+' + T0[i].toFixed(3),
  phase: e[0],
  name: e[1],
  apdu: sgPairs(e[4].slice(0, 20)) + (e[4].length > 20 ? ' …' : ''),
  ms: e[6],
}));

@Component({
  selector: 'ag-trace',
  imports: [UiIcon, SgBtn],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'sg-c' },
  template: `
    <div class="sg-card sg-layer ag-head" [style.--d]="160">
      <span class="ag-head-l"><span class="sg-ico ag-head-i"><ui-icon name="file-text" [style.--s]="15" /></span><span class="ag-head-t"><span class="ag-head-n">Trace Log</span><span class="sg-sub">Session #12 · Card-1 · {{ n() }} exchanges · {{ ms() }} ms</span></span></span>
      <span class="ag-head-r"><sg-btn class="sg-btn--xs" kind="ghost" icon="funnel">Filter</sg-btn><sg-btn class="sg-btn--xs" kind="ghost" icon="export">Export</sg-btn><span class="sg-ico"><ui-icon name="trash" class="sg-cb" /></span></span>
    </div>
    <div class="ag-phases sg-layer" [style.--d]="220">
      @for (p of phases; track p[0]) {
        <span class="sg-chip ag-phase" [class.sg-line]="n() >= p[1]" [class.ok]="n() >= p[1] && !p[2]" [class.skip]="n() >= p[1] && p[2] === 'skip'" [class.wait]="n() >= p[1] && p[2] === 'wait'">
          @if (n() >= p[1]) {
            @switch (p[2]) {
              @case ('skip') {<ui-icon name="minus-circle" [style.--s]="11" />}
              @case ('wait') {<ui-icon name="clock" [style.--s]="11" />}
              @default {<ui-icon name="check-circle" [style.--s]="11" />}
            }
          }{{ p[0] }}
        </span>
      }
    </div>
    <div class="sg-card sg-layer ag-table ag-trace" [style.--d]="300">
      <div class="sg-trow ag-trace-row ag-trace-head"><span>Time</span><span class="sg-wide">Phase</span><span>Command</span><span class="sg-wide">C-APDU</span><span>SW</span><span class="sg-wide">ms</span></div>
      @for (row of rows(); track $index; let last = $last) {
        <div class="sg-trow ag-trace-row sg-line" [class.cur]="last">
          <span class="sg-m10">{{ row.time }}</span>
          <span class="sg-wide"><span class="sg-tag sg-tag--b ag-trace-tag">{{ row.phase }}</span></span>
          <b>{{ row.name }}</b>
          <span class="sg-m10 sg-wide">{{ row.apdu }}</span>
          <span class="ag-sw sg-cg">90 00</span>
          <span class="sg-m10 sg-wide">{{ row.ms }}</span>
        </div>
      }
      @if (n() === 0) {<div class="ag-wait">› waiting for the terminal<span class="sg-caret" aria-hidden="true"></span></div>}
      <div class="sg-grow"></div>
      <div class="ag-foot"><span>Entries: {{ n() }}/7</span><span class="sg-wide">Timestamps: relative to Connect</span><span>Auto-scroll: <b class="sg-cg">ON</b></span></div>
    </div>
  `,
})
export class AgTrace {
  readonly t = input.required<number>();

  protected readonly phases = PHASES;
  protected readonly n = computed(() => {
    const t = this.t();
    return t < 0 || t > 9 ? 0 : Math.min(t + 1, 7);
  });
  protected readonly rows = computed(() => TRACE.slice(0, this.n()));
  protected readonly ms = computed(() => this.rows().reduce((sum, row) => sum + row.ms, 0));
}

// ---------- Wire Sniff: the USB-CDC link frame by frame ----------

interface Frame {
  time: string;
  /** true: Studio → firmware (card side); false: firmware → Studio (terminal side). */
  out: boolean;
  kind: string;
  body: string;
  quiet: boolean;
  bytes: number;
}

const frame = (time: string, out: boolean, kind: string, body: string): Frame => {
  const size = /\((\d+) B/.exec(body);
  return { time, out, kind, body, quiet: kind === 'HELLO' || kind === 'ACK' || kind === 'RESET', bytes: size ? +size[1] : 16 };
};

const WIRE: readonly Frame[] = [
  frame('+0.000', false, 'HELLO', 'fw 0.4.2 · NUCLEO-L432KC · pinboard XCRFID · T=0'),
  frame('+0.012', true, 'ATR', AG_ATR),
  frame('+0.014', false, 'ACK', 'ATR armed · waiting for terminal reset'),
  frame('+1.198', false, 'RESET', 'VCC 5.0 V · CLK 4 MHz · RST ↑ · ATR replayed'),
  ...EX.flatMap((e, i) => {
    const c = sgPairs(e[4]), r = sgPairs(e[5]);
    return [
      frame('+' + T0[i].toFixed(3), false, 'C-APDU', (c.length > 62 ? c.slice(0, 62) + ' …' : c) + '   (' + e[4].length / 2 + ' B)'),
      frame('+' + (T0[i] + e[6] / 1000).toFixed(3), true, 'R-APDU', (r.length > 44 ? r.slice(0, 44) + ' … ' + r.slice(-5) : r) + '   (' + e[5].length / 2 + ' B · ' + e[6] + ' ms)'),
    ];
  }),
];

@Component({
  selector: 'ag-sniff',
  imports: [UiIcon, SgBtn],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'sg-c' },
  template: `
    <div class="sg-card sg-layer ag-head" [style.--d]="160">
      <span class="ag-head-l"><span class="sg-ico ag-head-i"><ui-icon name="wifi-high" [style.--s]="15" /></span><span class="ag-head-t"><span class="ag-head-n">Wire Sniff</span><span class="sg-sub">{{ port }} · 115200 8N1 · binary framing · {{ n() }} frames · {{ size() }} KB</span></span></span>
      <span class="ag-head-r">
        @if (running()) {
          <sg-btn class="sg-btn--xs" kind="ghost" icon="pause">Pause</sg-btn>
        } @else {
          <sg-btn class="sg-btn--xs" kind="ghost" icon="play">Resume</sg-btn>
        }
        <sg-btn class="sg-btn--xs" kind="ghost" icon="floppy-disk">Save .pcap</sg-btn>
      </span>
    </div>
    <div class="ag-legend sg-layer" [style.--d]="220"><span><span class="ag-arrow sg-cb">←</span>from firmware · terminal side</span><span><span class="ag-arrow sg-ct">→</span>to firmware · card side</span></div>
    <div class="sg-card sg-layer ag-table ag-sniff" [style.--d]="300">
      <div class="ag-wire">
        @for (f of frames(); track $index) {
          <div class="ag-frame sg-line"><span class="sg-cm">{{ f.time }}</span><span [class.sg-ct]="f.out" [class.sg-cb]="!f.out">{{ f.out ? '→' : '←' }}</span><span class="ag-frame-k sg-wide" [class.sg-ct]="f.out" [class.sg-cb]="!f.out">{{ f.kind }}</span><span [class.sg-cm]="f.quiet"><span class="sg-narrow">{{ f.kind + '  ' }}</span>{{ f.body }}</span></div>
        }
        @if (n() === 0) {<div class="sg-cf">› opening {{ port }}<span class="sg-caret" aria-hidden="true"></span></div>}
      </div>
      <div class="ag-foot"><span>Frames: {{ n() }}/{{ total }}</span><span>CRC errors: <b class="sg-cg">0</b></span><span>Retries: 0</span></div>
    </div>
  `,
})
export class AgSniff {
  readonly t = input.required<number>();

  protected readonly port = AG_PORT;
  protected readonly total = WIRE.length;
  protected readonly n = computed(() => {
    const t = this.t();
    return t < 0 || t > 19 ? 0 : Math.min(t + 2, WIRE.length);
  });
  protected readonly frames = computed(() => WIRE.slice(0, this.n()));
  protected readonly running = computed(() => this.n() > 0 && this.n() < WIRE.length);
  protected readonly size = computed(() => (this.frames().reduce((sum, f) => sum + f.bytes, 0) / 1024).toFixed(1));
}

// ---------- L3 Report: the ten checks of the last transaction ----------

type Verdict = 'ok' | 'skip' | 'note';

const L3: readonly (readonly [string, string, Verdict])[] = [
  ['Application selection', 'PSE listed one ADF · A0000000041010 selected with priority 01', 'ok'],
  ['Initiate application processing', 'AIP 18 00 · AFL one entry — SFI 1, record 1', 'ok'],
  ['Read application data', '1 record · mandatory tags 5A, 5F24, 8C, 8D, 8E present', 'ok'],
  ['Offline data authentication', 'AIP offers no SDA / DDA / CDA — skipped, TVR byte 1 bit 8 set', 'skip'],
  ['Processing restrictions', 'Application version matches · not expired · AUC allows domestic goods', 'ok'],
  ['Cardholder verification', 'Plaintext PIN by ICC · VERIFY 90 00 · CVM results 41 03 02', 'ok'],
  ['Terminal risk management', '12.50 over the 10.00 floor limit → TVR byte 4 bit 8 set', 'ok'],
  ['Terminal action analysis', 'TVR 00 00 00 80 00 ∧ TAC-Online FC 50 BC F8 00 → ARQC requested', 'ok'],
  ['Card action analysis', 'GENERATE AC returned ARQC · ATC 00 2E · CID 80 as requested', 'ok'],
  ['Online processing', 'No host in this session — cryptogram not verified. Pair with the Host Simulator to complete.', 'note'],
];
const VERDICT: Record<Verdict, string> = { ok: 'pass', skip: 'skipped', note: 'note' };

@Component({
  selector: 'ag-l3',
  imports: [UiIcon, SgBtn],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'sg-c' },
  template: `
    <div class="sg-card sg-layer ag-head" [style.--d]="160">
      <span class="ag-head-l"><span class="sg-ico ag-head-i"><ui-icon name="seal-check" [style.--s]="15" /></span><span class="ag-head-t"><span class="ag-head-n">L3 Report</span><span class="sg-sub">{{ profile }} · contact purchase · 12.50 USD · {{ n() ? n() + ' of 10 checks' : 'waiting for a transaction' }}</span></span></span>
      <span class="ag-head-r"><sg-btn class="sg-btn--xs" kind="ghost" icon="file-pdf" [dim]="!done()">Export PDF</sg-btn><sg-btn class="sg-btn--xs" kind="ghost" icon="copy" [dim]="!done()">Copy JSON</sg-btn></span>
    </div>
    <div class="ag-tally">
      <div class="sg-card sg-stat sg-tile ok" [style.--d]="240"><ui-icon name="check-circle" [style.--s]="18" />@for (v of [count('ok')]; track v) {<b class="sg-line">{{ v }}</b>}<span>Passed</span></div>
      <div class="sg-card sg-stat sg-tile skip" [style.--d]="300"><ui-icon name="minus-circle" [style.--s]="18" />@for (v of [count('skip')]; track v) {<b class="sg-line">{{ v }}</b>}<span>Skipped</span></div>
      <div class="sg-card sg-stat sg-tile note" [style.--d]="360"><ui-icon name="warning-circle" [style.--s]="18" />@for (v of [count('note')]; track v) {<b class="sg-line">{{ v }}</b>}<span>Notes</span></div>
    </div>
    <div class="sg-card sg-layer ag-checks" [style.--d]="360">
      @for (row of rows(); track $index) {
        <div class="ag-checkrow sg-line" [class.ok]="row[2] === 'ok'" [class.skip]="row[2] === 'skip'" [class.note]="row[2] === 'note'">
          <span class="ag-check-i">
            @switch (row[2]) {
              @case ('skip') {<ui-icon name="minus-circle" [style.--s]="13" />}
              @case ('note') {<ui-icon name="warning-circle" [style.--s]="13" />}
              @default {<ui-icon name="check-circle" [style.--s]="13" />}
            }
          </span>
          <span class="ag-check-t"><span class="ag-check-h"><span class="ag-check-n">{{ row[0] }}</span><span class="ag-check-v">{{ verdict[row[2]] }}</span></span><span class="ag-check-d">{{ row[1] }}</span></span>
        </div>
      }
      @if (n() === 0) {<div class="ag-checks-empty">The report fills in as the transaction runs</div>}
    </div>
  `,
})
export class AgL3 {
  readonly t = input.required<number>();

  protected readonly profile = AG_PROFILE;
  protected readonly verdict = VERDICT;
  protected readonly n = computed(() => {
    const t = this.t();
    return t < 0 || t > 13 ? 0 : Math.min(t + 1, 10);
  });
  protected readonly done = computed(() => this.n() === 10);
  protected readonly rows = computed(() => L3.slice(0, this.n()));

  protected count(kind: Verdict): number {
    return this.rows().filter((row) => row[2] === kind).length;
  }
}

// ---------- Firmware: the NUCLEO board, its pinboard map, and an update flashed from Studio ----------

const PINS: readonly (readonly [string, string, string])[] = [
  ['C1', 'VCC', 'PA0 · sense'], ['C2', 'RST', 'PB0 · EXTI'], ['C3', 'CLK', 'PA8 · TIM1_CH1'], ['C5', 'GND', 'GND'],
  ['C7', 'I/O', 'PA9 · USART1 half-duplex'],
];
const FLASH = ['Erasing sectors 0–23…', 'Writing 47,312 bytes…', 'Verifying…', 'Done · board reset · fw 0.4.3 reported'];
/** [label, value, mono] */
const BOARD: readonly (readonly [string, string, boolean])[] = [
  ['Board', 'NUCLEO-L432KC', false], ['MCU', 'STM32L432KC · 80 MHz · 256 KB', true], ['Debugger', 'ST-LINK/V2-1 · 066BFF38…', true],
  ['Port', AG_PORT, true], ['Pinboard', 'XCRFID contact card', false],
];

@Component({
  selector: 'ag-firmware',
  imports: [UiIcon, SgBtn],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'sg-c' },
  template: `
    <div class="ag-fw">
      <div class="sg-card sg-layer sg-pad" [style.--d]="160">
        <div class="sg-cap sg-cap--icon"><ui-icon name="circuitry" [style.--s]="11" />Board</div>
        @for (row of board; track row[0]; let i = $index) {
          <div class="sg-kv sg-kv--90 sg-tile" [style.--d]="220 + i * 30"><span class="sg-kv-k">{{ row[0] }}</span><span class="sg-kv-v" [class.sg-kv-v--mono]="row[2]" [class.sg-kv-v--clip]="row[2]">{{ row[1] }}</span></div>
        }
        <div class="ag-fw-map">Contact map</div>
        <div class="sg-sunk ag-pins">
          @for (pin of pins; track pin[0]; let i = $index) {
            <div class="sg-trow ag-pin sg-tile" [style.--d]="380 + i * 40"><span class="sg-code ag-pin-c">{{ pin[0] }}</span><b>{{ pin[1] }}</b><span class="sg-m10">{{ pin[2] }}</span></div>
          }
        </div>
      </div>
      <div class="sg-stack">
        <div class="sg-card sg-layer sg-pad" [style.--d]="240">
          <div class="sg-cap sg-cap--icon"><ui-icon name="cpu" [style.--s]="11" />Firmware</div>
          <div class="ag-fw-vers">
            <div class="sg-sunk ag-fw-ver"><span class="ag-fw-k">Installed</span>@for (v of [version()]; track v) {<div class="ag-fw-v sg-line">{{ v }}</div>}<div class="sg-sub">{{ done() ? 'built 2026-09-19 · T=1 block chaining' : 'built 2026-09-14' }}</div></div>
            <div class="sg-sunk ag-fw-ver ag-fw-ver--latest" [class.done]="done()"><span class="ag-fw-k">Latest</span><div class="ag-fw-v">0.4.3</div><div class="sg-sub">{{ done() ? 'up to date' : 'adds T=1 block chaining · faster ATR' }}</div></div>
          </div>
          <div class="sg-acts ag-fw-acts">
            @if (done()) {
              <sg-btn kind="ghost" icon="check" [press]="t() === 0" [dim]="busy()">Up to date</sg-btn>
            } @else {
              <sg-btn icon="lightning" [press]="t() === 0" [dim]="busy()">Flash 0.4.3</sg-btn>
            }
            <sg-btn kind="ghost" icon="arrow-counter-clockwise" [dim]="busy()">Reset board</sg-btn>
            <sg-btn kind="ghost" icon="download-simple" [dim]="busy()">DFU mode</sg-btn>
          </div>
          <div class="ag-fw-prog">
            <div class="ag-fw-prog-h"><span>{{ stage() }}</span><b>{{ pct() }}%</b></div>
            <div class="ag-fw-bar"><b [class.done]="done()" [class.lit]="pct() > 0" [style.--p]="pct()"></b></div>
          </div>
        </div>
        <div class="sg-card sg-layer ag-fw-log" [style.--d]="320">
          <div class="sg-pre sg-cm">› st-flash · {{ port }}</div>
          @for (line of log(); track $index; let i = $index) {<div class="sg-pre sg-line" [class.sg-cg]="i === 3">{{ (i === 3 ? '✓ ' : '  ') + line }}</div>}
        </div>
      </div>
    </div>
  `,
})
export class AgFirmware {
  readonly t = input.required<number>();

  protected readonly port = AG_PORT;
  protected readonly board = BOARD;
  protected readonly pins = PINS;

  protected readonly busy = computed(() => this.t() >= 1 && this.t() <= 4);
  protected readonly done = computed(() => this.t() >= 5 && this.t() < 10);
  protected readonly pct = computed(() => {
    const t = this.t();
    return t < 1 || t > 9 ? 0 : Math.min(100, t * 25);
  });
  protected readonly version = computed(() => (this.done() ? '0.4.3' : '0.4.2'));
  protected readonly stage = computed(() =>
    this.busy() ? FLASH[Math.min(3, this.t() - 1)] : this.done() ? FLASH[3] : 'Ready');
  protected readonly log = computed(() => FLASH.slice(0, this.busy() ? this.t() : this.done() ? 4 : 0));
}
