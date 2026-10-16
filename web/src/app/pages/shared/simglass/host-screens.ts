import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { SIM_GLASS, SimTab } from './sim-glass';

/**
 * The Host Simulator's screens as glass: ISO8583 Transaction (0200 → 0210 in
 * four panes), Settings (transaction rules), Message Parser, Bitmap Analysis,
 * Response Settings, Log Settings, ISO8583 Template, Gateway Type and
 * Transmission Settings.
 *
 *   <app-host-screen id="txn" />
 *
 * Input: id (HostScreenId, required). HOST_SCREENS[id].sub is the name the
 * caption under a screen starts with. Frame, sizing and loop: sim-glass.ts.
 */

export type HostScreenId =
  | 'txn' | 'settings' | 'parser' | 'bitmap' | 'response' | 'logging' | 'template' | 'gateway' | 'connections';

export interface HostScreenSpec {
  title: string;
  sub: string;
  tabs: readonly SimTab[] | null;
  tab: number;
  steps: number;
  period: number;
  settled: number;
  aria: string;
}

const TABS: readonly SimTab[] = [
  ['arrows-left-right', 'ISO8583 Transaction'], ['list-dashes', 'Logs'], ['gear', 'Settings'],
  ['code', 'ISO8583 Template'], ['envelope-open', 'Unsolicited Message'],
];
const TITLE = 'Host Simulator - Hitachi - 1';
const CFG = 'Host Simulator Configuration - Hitachi - 1';

export const HOST_SCREENS: Record<HostScreenId, HostScreenSpec> = {
  txn: { title: TITLE, sub: 'ISO8583 Transaction', tabs: TABS, tab: 0, steps: 7, period: 1300, settled: 3, aria: 'The ISO8583 Transaction tab: the host started on 0.0.0.0:8080, a formatted 0200 request and 0210 response beside their raw hex' },
  settings: { title: TITLE, sub: 'Settings', tabs: TABS, tab: 2, steps: 9, period: 1000, settled: 5, aria: 'The Settings tab: SALE, REVERSAL and VOID transactions on the left, the Fields tab with 13 configured ISO8583 fields on the right' },
  parser: { title: TITLE, sub: 'Unsolicited Message', tabs: TABS, tab: 4, steps: 8, period: 1200, settled: 4, aria: 'The Message Parser: a raw ISO8583 hex message parsed into 12 fields, field 25 selected and its point of service condition code decoded' },
  bitmap: { title: TITLE, sub: 'Bitmap Analysis', tabs: TABS, tab: 0, steps: 7, period: 1100, settled: 4, aria: 'Bitmap Analysis for hex bitmap 3038048020C00014, highlighting the twelve set fields out of 64' },
  response: { title: CFG, sub: 'Gateway Configuration', tabs: null, tab: -1, steps: 7, period: 1100, settled: 3, aria: 'Response Settings with the Realistic response delay selected, Max Concurrent set to 50, and Advanced Options with Enable Detailed Logging checked' },
  logging: { title: CFG, sub: 'Log Settings', tabs: null, tab: -1, steps: 7, period: 1100, settled: 4, aria: 'Logging Options with logfile name and max size, and Logging Content set to Parsed data using the ISO8583 protocol' },
  template: { title: TITLE, sub: 'ISO8583 Template', tabs: TABS, tab: 3, steps: 7, period: 1100, settled: 3, aria: 'The ISO8583 Template tab: source bit templates beside the source advanced options, message format and customized message panels' },
  gateway: { title: CFG, sub: 'Gateway Type', tabs: null, tab: -1, steps: 6, period: 1200, settled: 2, aria: 'Gateway Type selector with Server, Client and Proxy tiles and the Synchronous / Asynchronous transmission type below' },
  connections: { title: CFG, sub: 'Transmission Settings', tabs: null, tab: -1, steps: 7, period: 1100, settled: 4, aria: 'Incoming and outgoing connection settings side by side, each with a connection type and a message length format' },
};

const BITS = [3, 4, 11, 12, 13, 22, 25, 35, 41, 42, 60, 62];

@Component({
  selector: 'app-host-screen',
  imports: [...SIM_GLASS],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'hg-screen' },
  template: `
    @let s = spec();
    <app-sim-glass #g [heading]="s.title" [sub]="s.sub" [tabs]="s.tabs" [tab]="s.tab" [aria]="s.aria" app="host-simulator" badge="HOST"
                   [steps]="s.steps" [period]="s.period" [settled]="s.settled">
      @let t = g.t();
      @switch (id()) {
        @case ('txn') {
          <!-- Start → 0200 request and 0210 response land in the four panes → Clear → Stop -->
          @let active = t < 6;
          @let req = t >= 1 && t < 5;
          @let rsp = t >= 2 && t < 5;
          <div class="hg-strip hg-line" [class.hg-off]="!active"><span><i class="hg-dot hg-dot--g"></i>Active <b class="hg-strip-addr">0.0.0.0:8080</b></span><span class="hg-cn">Count: {{ rsp ? '13400' : '13399' }}</span></div>
          <div class="hg-card hg-layer hg-ctl" [style.--hd]="160">
            <span class="hg-f hg-g8">
              @if (active) { <span hgBtn kind="danger" icon="stop">Stop</span> } @else { <span hgBtn icon="play">Start</span> }
              <span hgBtn kind="ghost" icon="x" [press]="t === 5">Clear</span>
              @if (!active) { <ui-icon name="gear" class="hg-cn" [style.--hi]="14" /><span class="hg-cn">Gateway Stopped</span> }
            </span>
            <span class="hg-f hg-g12 hg-cn">
              <span class="hg-f hg-g6"><i class="hg-tog"></i>Hold</span>
              <span class="hg-sunk hg-hold">60</span>
              <span hgBtn [kind]="active ? 'pri' : 'ghost'" icon="paper-plane-tilt" [dim]="!active">Send</span>
            </span>
          </div>
          <div class="hg-grid hg-c2 hg-nc1 hg-g10">
            <app-hg-pane heading="Formatted Request" tone="req" icon="arrow-up-right" [count]="req ? 25317 : 0" [h]="336" [hn]="150" [d]="300">@if (req) {<app-hg-lines [lines]="tReq" [step]="40" [wide]="7" />}</app-hg-pane>
            <app-hg-pane heading="Formatted Response" tone="rsp" icon="arrow-down-left" [count]="rsp ? 279 : 0" [h]="336" [hn]="150" [d]="380">@if (rsp) {<app-hg-lines [lines]="tRsp" [step]="45" [wide]="7" />}</app-hg-pane>
            <app-hg-pane heading="Raw Request (Hex)" tone="req" icon="code" [count]="req ? 580 : 0" [h]="100" [hn]="80" [d]="460">@if (req) {<div class="hg-line hg-raw hg-raw--clip">{{ tRawReq }}</div>}</app-hg-pane>
            <app-hg-pane heading="Raw Response (Hex)" tone="rsp" icon="code" [count]="rsp ? 152 : 0" [h]="100" [hn]="80" [d]="540">@if (rsp) {<div class="hg-line hg-raw">{{ tRawRsp }}</div>}</app-hg-pane>
          </div>
        }

        @case ('settings') {
          <!-- Settings: the Transaction Simulator — SALE selected, its response fields filling in, Save All -->
          @let sel = t < 8;
          @let rows = t < 1 || t >= 8 ? 0 : min(8, t * 2);
          <div class="hg-card hg-layer hg-ctl hg-ctl--lg" [style.--hd]="160">
            <span><span class="hg-cb hg-hd15">Transaction Simulator</span><span class="hg-sub">Configure and manage ISO8583 transaction templates</span></span>
            <span class="hg-f hg-g6">
              <span hgBtn class="hg-w" kind="ghost" icon="sliders-horizontal">Simulation</span>
              <span hgBtn class="hg-w" kind="ghost" icon="info">Field Info</span>
              <span hgBtn class="hg-w" kind="ghost" icon="upload-simple">Export</span>
              <span hgBtn class="hg-w" kind="ghost" icon="download-simple">Import</span>
              <span hgBtn icon="floppy-disk" [press]="t === 6">Save All</span>
            </span>
          </div>
          <div class="hg-grid hg-c2 hg-nc1 hg-g12 hg-start">
            <div class="hg-card hg-layer hg-txns" [style.--hd]="240">
              <div class="hg-txns-h"><span><span class="hg-list-t">Transactions</span><span class="hg-list-s hg-list-s--flat">3 total</span></span><span class="hg-ico hg-ico--26 hg-ico--white"><ui-icon name="plus" [style.--hi]="13" /></span></div>
              @for (txn of txns; track txn[0]; let i = $index) {
                <div class="hg-txn hg-rise" [class.on]="i === 0 && sel" [style.--hd]="340 + i * 60"><i></i><span><span class="hg-txn-n">{{ txn[0] }}</span><span class="hg-f hg-g6 hg-mt4"><span class="hg-tag hg-ct">MTI: {{ txn[1] }}</span><span class="hg-tag hg-ct">PC: {{ txn[2] }}</span></span></span><ui-icon name="caret-down" class="hg-cf" [style.--hi]="11" /></div>
              }
            </div>
            <div class="hg-card hg-layer hg-clip" [style.--hd]="300">
              <div class="hg-seg2"><span class="on">Fields</span><span>Config</span></div>
              <div class="hg-grid hg-g8 hg-p12">
                <div class="hg-fb"><b class="hg-fs125">ISO8583 Fields</b><span class="hg-fs10 hg-cn">{{ sel ? '13 fields configured' : 'select a transaction' }}</span></div>
                <div class="hg-fchips">@for (chip of fieldChips; track chip) {<span class="hg-chip hg-chip--xs hg-line" [class.hg-off]="!sel">{{ chip }}<ui-icon name="check" class="hg-ct" [style.--hi]="8" /></span>}</div>
                @for (field of fields; track field[0]; let i = $index) {
                  <div class="hg-frow hg-line" [class.hg-off]="i >= rows"><span class="hg-code hg-code--teal hg-cg">{{ field[0] }}</span><span class="hg-mw0"><b>{{ field[1] }}<ui-icon name="check" class="hg-ct" [style.--hi]="10" /></b><small>{{ field[2] }}</small></span><span class="hg-f hg-g10"><ui-icon name="trash" class="hg-cr" /><ui-icon name="caret-down" class="hg-cf" [style.--hi]="11" /></span></div>
                }
                <span hgBtn class="hg-btn--add" kind="ghost" icon="plus" [class.hg-off]="!sel" [press]="t === 5">Add Fields</span>
              </div>
            </div>
          </div>
        }

        @case ('parser') {
          <!-- Message Parser: raw hex in, twelve fields out, F25 decoded -->
          @let typed = t < 7;
          @let parsed = t >= 2 && t < 7;
          @let detail = t >= 3 && t < 7;
          @let analysis = t >= 4 && t < 7;
          <div class="hg-card hg-layer hg-pad hg-grid hg-g12" [style.--hd]="160">
            <div class="hg-fb hg-g10 hg-wrapf">
              <span class="hg-f hg-g12"><ui-icon name="code" class="hg-cb" [style.--hi]="18" /><span><span class="hg-hd15">Message Parser</span><span class="hg-sub">Real-time ISO8583 analysis</span></span>
                @if (t === 1) { <span class="hg-tag hg-tag--lg hg-line hg-cn">Parsing…</span> }
                @if (parsed) { <span class="hg-tag hg-tag--lg hg-tag--ok hg-line hg-cg">✓ Parsed successfully - 12 fields</span> }
              </span>
              <span class="hg-f hg-g8">
                @if (parsed) { <span class="hg-chip on hg-g5"><ui-icon name="code" [style.--hi]="11" />MTI: 200</span><span class="hg-chip on hg-g5"><ui-icon name="check-circle" [style.--hi]="11" />12 fields</span> }
                <span class="hg-rel"><span hgBtn class="hg-cx" kind="ghost" icon="grid-four">Bitmap</span>@if (parsed) {<span class="hg-tag hg-tag--count hg-line">12/64</span>}</span>
              </span>
            </div>
            <div><div class="hg-lab hg-f hg-g6"><ui-icon name="sign-in" [style.--hi]="11" />Raw ISO8583 Message (Hex)</div><div class="hg-sunk hg-mono hg-rawmsg" [class.hg-cf]="!typed">@if (typed) {<span class="hg-line">{{ rawMsg }}</span>} @else {Paste a hex message…}<span class="hg-caret" aria-hidden="true"></span></div></div>
          </div>
          <div class="hg-grid hg-c2 hg-nc1 hg-g12 hg-start">
            <div class="hg-grid hg-g12">
              <div class="hg-card hg-layer hg-f hg-g8 hg-p10" [style.--hd]="260"><div class="hg-fld hg-grow"><ui-icon name="magnifying-glass" class="hg-cf" /><span class="hg-ph">Search fields…</span></div><span class="hg-chip on">{{ parsed ? 12 : 0 }} Fields</span></div>
              <div class="hg-card hg-layer hg-pad" [style.--hd]="340">
                <div class="hg-card-h"><div class="hg-h"><ui-icon name="list-bullets" class="hg-ct" [style.--hi]="14" />Message Fields</div><span class="hg-fs10 hg-cn">{{ parsed ? '12 fields present' : 'no message' }}</span></div>
                <div class="hg-grid hg-g8">
                  @for (field of parsedFields; track field[0]; let i = $index) {
                    <div class="hg-frow hg-line" [class.hg-off]="!parsed" [style.--ld]="i * 70"><span class="hg-code hg-code--blue hg-cb">{{ field[0] }}</span><span class="hg-mw0"><span class="hg-tag hg-tag--bcd">BCD</span><b class="hg-frow-n">{{ field[1] }}</b></span><span class="hg-frow-r"><ui-icon name="caret-right" class="hg-cf" [style.--hi]="11" /><small>{{ field[2] }}</small></span></div>
                  }
                </div>
              </div>
            </div>
            <div class="hg-grid hg-g12">
              <div class="hg-card hg-layer hg-pad hg-swap hg-detail" [style.--hd]="420">
                <div class="hg-line" [class.hg-off]="!detail">
                  <div class="hg-fb hg-top"><span class="hg-f hg-g10"><span class="hg-code hg-code--f25">F25</span><span class="hg-grid hg-g4"><span class="hg-tag hg-tag--bcd">BCD</span><span class="hg-tag hg-tag--blue hg-cb">FIXED</span></span></span><span class="hg-f hg-g10 hg-cn"><ui-icon name="caret-left" /><ui-icon name="caret-right" /><ui-icon name="pencil-simple" /></span></div>
                  <div class="hg-ct hg-fs115 hg-mt8">Point of service condition code</div>
                  <div class="hg-sunk hg-mono hg-f25-v">00</div>
                  <div class="hg-f hg-g6 hg-mt8 hg-wrapf">@for (fact of f25; track fact[0]) {<span class="hg-tag hg-tag--fact">{{ fact[0] }}: <b class="hg-cx">{{ fact[1] }}</b></span>}</div>
                </div>
                <div class="hg-empty hg-empty--60" [class.hg-off]="detail">Select a field to inspect it</div>
              </div>
              <div class="hg-card hg-pad hg-line" [class.hg-off]="!analysis">
                <div class="hg-card-h"><div class="hg-h"><ui-icon name="storefront" class="hg-co" [style.--hi]="14" />POS Condition Code Analysis</div></div>
                <span class="hg-tag hg-tag--pos">Normal presentment</span>
              </div>
            </div>
          </div>
        }

        @case ('bitmap') {
          <!-- Bitmap Analysis: 3038048020C00014 lit bit by bit -->
          @let lit = t === 6 ? 0 : min(12, t * 3);
          <div class="hg-card hg-layer hg-pad" [style.--hd]="160">
            <div class="hg-f hg-g12"><span class="hg-ico hg-ico--40 hg-ico--blue hg-ico--round"><ui-icon name="grid-four" [style.--hi]="18" /></span><span><span class="hg-hd15">Bitmap Analysis</span><span class="hg-sub">{{ lit }} of 64 fields present</span></span></div>
            <div class="hg-sunk hg-mono hg-bm-hex">Hex: <b class="hg-cx">{{ t < 6 ? '3038048020C00014' : '————————————————' }}</b></div>
            <div class="hg-sunk hg-bm-box"><div class="hg-cells">@for (cell of cells; track cell.n) {<div class="hg-cell" [class.on]="cell.k >= 0 && cell.k < lit">{{ cell.n }}</div>}</div></div>
            <div class="hg-bm-legend"><span><ui-icon name="check-circle" class="hg-cb" />Set &amp; Present</span><span><ui-icon name="warning" class="hg-cr" />Set but Missing</span><span><ui-icon name="circle" class="hg-ce" />Not Set</span></div>
          </div>
        }

        @case ('response') {
          <!-- Gateway Configuration: response delay, max concurrent, advanced options -->
          @let live = t < 6;
          @let delay = t < 1 || t >= 6 ? 0 : 1;
          <div class="hg-card hg-layer hg-pad" [style.--hd]="160">
            <div class="hg-card-h"><div class="hg-h hg-caps"><ui-icon name="gauge" class="hg-cb" [style.--hi]="14" />Response Settings</div></div>
            <div class="hg-l115 hg-mb8">Response Delay</div>
            <div class="hg-seg">@for (option of delays; track option; let i = $index) {<span [class.on]="i === delay && live">{{ option }}</span>}</div>
            <div class="hg-f hg-g12 hg-mt14"><span class="hg-l115">Max Concurrent</span><div class="hg-fld hg-fld--70" [class.on]="t === 2">@if (t >= 2 && live) {<span class="hg-line">50</span>} @else {<span class="hg-ph">—</span>}</div><span class="hg-fs105 hg-cn">Maximum parallel transactions</span></div>
          </div>
          <div class="hg-card hg-layer hg-pad" [style.--hd]="300">
            <div class="hg-card-h"><div class="hg-h hg-caps"><ui-icon name="sliders-horizontal" class="hg-cb" [style.--hi]="14" />Advanced Options</div></div>
            <div class="hg-rise hg-optrow" [style.--hd]="420"><span class="hg-cb-box" [class.on]="t >= 3 && live">@if (t >= 3 && live) {<ui-icon name="check" />}</span><span><span class="hg-l115">Enable Detailed Logging</span><span class="hg-sub">Log all transactions and responses for debugging</span></span></div>
            <div class="hg-rise hg-optrow" [style.--hd]="500"><span class="hg-cb-box"></span><span><span class="hg-l115">Auto-start Simulator</span><span class="hg-sub">Start this simulator when application launches</span></span></div>
          </div>
        }

        @case ('logging') {
          <!-- Log Settings: file, size, content mode, protocol, template -->
          @let live = t < 6;
          @let parsed = t >= 2 && live;
          <div class="hg-card hg-layer hg-pad" [style.--hd]="160">
            <div class="hg-card-h"><div class="hg-h">Logging Options</div></div>
            <div class="hg-grid hg-logopts">
              <span class="hg-l115">Logfile name</span><div class="hg-fld" [class.on]="t === 0">@if (live) {<span class="hg-line">logs.txt</span>} @else {<span class="hg-ph"></span>}</div>
              <span class="hg-l115">Max size (MB)</span><div class="hg-fld hg-fld--90" [class.on]="t === 1">@if (t >= 1 && live) {<span class="hg-line">10</span>} @else {<span class="hg-ph"></span>}</div>
            </div>
          </div>
          <div class="hg-card hg-layer hg-pad" [style.--hd]="280">
            <div class="hg-card-h"><div class="hg-h">Logging Content</div></div>
            <div class="hg-grid hg-g9">@for (mode of logModes; track mode; let i = $index) {<div class="hg-f hg-g10 hg-fs115"><i class="hg-rd" [class.on]="i === 3 && parsed"></i>{{ mode }}</div>}</div>
            <div class="hg-grid hg-g10 hg-mt12 hg-line" [class.hg-off]="!parsed">
              <div class="hg-f hg-g12 hg-pl24"><span class="hg-l115">Protocol</span><span class="hg-chip hg-chip--proto" [class.on]="t >= 3 && live">ISO8583</span></div>
              <div class="hg-fb hg-g12 hg-pl24"><span class="hg-l115">Template File</span><span hgBtn kind="ghost" icon="file-text" [press]="t === 4">Change</span></div>
            </div>
          </div>
          <div class="hg-card hg-layer hg-pad" [style.--hd]="400">
            <div class="hg-card-h"><div class="hg-h">Advanced Options</div><ui-icon name="question" class="hg-cb" [style.--hi]="14" /></div>
            <div class="hg-fs105 hg-cn">Configure advanced logging parameters in this section to optimize performance and storage.</div>
          </div>
        }

        @case ('template') {
          <!-- ISO8583 Template: the source bit table beside the template-level options -->
          @let rows = t === 6 ? 0 : min(15, (t + 1) * 5);
          <div class="hg-grid hg-g12 hg-start hg-tpl">
            <div class="hg-card hg-layer hg-clip hg-fade82 hg-swap" [style.--hd]="160">
              <div>
                <div class="hg-trow head hg-bitcols"><span class="hg-trow-t">Source Bit Templates</span><span>Bit No.</span><span>Format Type</span><span>Length Type</span><span>Max Length</span><span>Description</span></div>
                @for (row of bitRows; track row[0]; let i = $index) {
                  <div class="hg-trow hg-bitcols hg-line" [class.hov]="t === 3 && i === 1" [class.hg-off]="i >= rows"><span><b>{{ row[0] }}</b></span><span>{{ row[1] }}</span><span>{{ row[2] }}</span><span>{{ row[3] }}</span><span>{{ row[4] }}</span></div>
                }
              </div>
              <div class="hg-tpl-empty" [class.hg-off]="rows > 0">Upload YAML or add bits…</div>
            </div>
            <div class="hg-grid hg-g12">
              <div class="hg-card hg-layer hg-pad" [style.--hd]="260">
                <div class="hg-card-h"><div class="hg-h">Source Advanced Options</div></div>
                <div class="hg-subline">Advanced options (be careful)</div>
                <div class="hg-grid hg-g9">
                  <div class="hg-f hg-top hg-g10 hg-fs11"><span class="hg-cb-box"></span><span>Iso8583 use Ascii<span class="hg-sub hg-fs9">Sends LLVAR/LLLVAR length prefixes as ASCII digits ("16") instead of packed BCD (0x16). Required by ASCII dialects such as BASE24 and Visa BASE I.</span></span></div>
                  @for (option of templateOptions; track option) { <div class="hg-f hg-top hg-g10 hg-fs11"><span class="hg-cb-box"></span>{{ option }}</div> }
                </div>
              </div>
              <div class="hg-card hg-layer hg-pad" [style.--hd]="360">
                <div class="hg-card-h"><div class="hg-h">Source Message Format</div></div>
                <div class="hg-grid hg-g10 hg-mid hg-c120"><span class="hg-fs11 hg-b6">Message Format:</span><div class="hg-fld"><span>Default</span><ui-icon name="caret-down" class="hg-cf hg-fld-caret" /></div></div>
                <div class="hg-tag hg-tag--hint">Binary/Hex format uses your existing ISO8583 pack/unpack methods. No YAML configuration needed.</div>
              </div>
              <div class="hg-card hg-layer hg-pad" [style.--hd]="460">
                <div class="hg-card-h"><div class="hg-h">Source Customized Message</div></div>
                <div class="hg-f hg-g10 hg-fs11"><span class="hg-cb-box"></span>Customized Message</div>
                <div class="hg-grid hg-g10 hg-mid hg-mt10 hg-fs11 hg-c12060"><span class="hg-b6">Ignore Request header</span><div class="hg-fld"><span>5</span></div><span class="hg-cn">bytes</span><span class="hg-b6">Fixed response Header</span><div class="hg-fld hg-span-rest"><span class="hg-ph"></span></div></div>
              </div>
              <div class="hg-f hg-end"><span hgBtn icon="floppy-disk" [press]="t === 4">Save</span></div>
            </div>
          </div>
        }

        @case ('gateway') {
          <!-- Gateway Type + Transmission Type -->
          @let live = t < 5;
          <div class="hg-card hg-layer hg-pad" [style.--hd]="160">
            <div class="hg-card-h"><div class="hg-h hg-caps"><ui-icon name="laptop" class="hg-cb" [style.--hi]="14" />Gateway Type</div></div>
            <div class="hg-subline">Select how the host simulator will operate in your network</div>
            <div class="hg-grid hg-c3 hg-nc1 hg-g10">
              <div class="hg-tile hg-big" [class.on]="t >= 1 && live" [class.hov]="t === 0"><ui-icon name="laptop" [style.--hi]="20" /><span class="hg-big-t">Server</span><span class="hg-big-s">Accept incoming connections</span></div>
              <div class="hg-tile hg-big"><span class="hg-tag hg-big-badge">🔧 Under Development</span><ui-icon name="plugs" [style.--hi]="20" /><span class="hg-big-t">Client</span><span class="hg-big-s">Connect to external host</span></div>
              <div class="hg-tile hg-big"><span class="hg-tag hg-big-badge">🔧 Under Development</span><ui-icon name="broadcast" [style.--hi]="20" /><span class="hg-big-t">Proxy</span><span class="hg-big-s">Bridge between systems</span></div>
            </div>
          </div>
          <div class="hg-card hg-layer hg-pad" [style.--hd]="320">
            <div class="hg-card-h"><div class="hg-h hg-caps"><ui-icon name="arrows-clockwise" class="hg-cb" [style.--hi]="14" />Transmission Type</div></div>
            <div class="hg-subline">Configure how data flows through the proxy connection</div>
            <div class="hg-grid hg-c2 hg-nc1 hg-g10">
              <div class="hg-tile hg-big" [class.outline]="t >= 2 && live"><ui-icon name="arrows-left-right" [style.--hi]="20" /><span class="hg-big-t">Synchronous</span><span class="hg-big-s">Wait for response before next request</span></div>
              <div class="hg-tile hg-big" [class.hov]="t === 3"><ui-icon name="shuffle" [style.--hi]="20" /><span class="hg-big-t">Asynchronous</span><span class="hg-big-s">Process multiple requests concurrently</span></div>
            </div>
          </div>
        }

        @case ('connections') {
          <!-- Transmission Settings: incoming listener and outgoing destination side by side -->
          @let live = t < 6;
          <div class="hg-grid hg-c2 hg-nc1 hg-g12 hg-start">
            <div class="hg-card hg-layer hg-pad" [style.--hd]="160">
              <div class="hg-card-h"><div class="hg-h hg-caps"><ui-icon name="arrow-down-left" class="hg-cb" [style.--hi]="14" />Incoming Connections</div></div>
              <div class="hg-subline">Configure how clients connect to this host simulator</div>
              <div class="hg-fs11 hg-b6 hg-mb8">Connection Type</div>
              <div class="hg-f hg-g6 hg-wrapf">
                <span class="hg-chip hg-g5" [class.on]="live"><ui-icon name="wifi-high" [style.--hi]="11" />TCP/IP</span>
                <span class="hg-chip hg-g5"><ui-icon name="plugs" [style.--hi]="11" />RS232</span>
                <span class="hg-chip hg-g5"><ui-icon name="phone" [style.--hi]="11" />Dial-up</span>
                <span class="hg-chip hg-g5"><ui-icon name="globe" [style.--hi]="11" />REST API</span>
              </div>
              <div class="hg-sunk hg-netbox"><div class="hg-cb hg-l115 hg-mb10">Network Configuration</div><div class="hg-grid hg-c21 hg-g10">
                <app-hg-fld label="IP Address" icon="laptop" [value]="t >= 1 && live ? '0.0.0.0' : ''" [on]="t === 1" />
                <app-hg-fld label="Port" icon="wifi-high" [value]="t >= 2 && live ? '8080' : ''" [on]="t === 2" />
              </div></div>
              <app-hg-fld class="hg-mt12" label="Incoming Message Length" [value]="t >= 3 && live ? 'BCD' : ''" ph="Select…" [caret]="true" [on]="t === 3" />
            </div>
            <div class="hg-card hg-layer hg-pad" [style.--hd]="280">
              <div class="hg-card-h"><div class="hg-h hg-caps"><ui-icon name="arrow-up-right" class="hg-cb" [style.--hi]="14" />Outgoing Connections</div></div>
              <div class="hg-subline">Configure connections to external hosts or services</div>
              <div class="hg-fs11 hg-b6 hg-mb8">Connection Type</div>
              <div class="hg-f hg-g6 hg-wrapf">
                <span class="hg-chip hg-g5" [class.on]="live"><ui-icon name="wifi-high" [style.--hi]="11" />TCP/IP</span>
                <span class="hg-chip hg-g5"><ui-icon name="plugs" [style.--hi]="11" />RS232</span>
                <span class="hg-chip hg-g5"><ui-icon name="phone" [style.--hi]="11" />Dial-up</span>
                <span class="hg-chip hg-g5"><ui-icon name="globe" [style.--hi]="11" />REST API</span>
              </div>
              <div class="hg-sunk hg-netbox"><div class="hg-cb hg-l115 hg-mb10">Destination Server</div><div class="hg-grid hg-c21 hg-g10">
                <app-hg-fld label="IP Address" icon="laptop" [value]="t >= 4 && live ? '127.0.0.1' : ''" [on]="t === 4" />
                <app-hg-fld label="Port" icon="wifi-high" [value]="t >= 4 && live ? '8080' : ''" [on]="t === 4" />
              </div></div>
              <app-hg-fld class="hg-mt12" label="Outgoing Message Length" [value]="t >= 5 && live ? 'HEX_HL' : ''" ph="Select…" [caret]="true" [on]="t === 5" />
            </div>
          </div>
        }
      }
    </app-sim-glass>
  `,
})
export class HostScreen {
  readonly id = input.required<HostScreenId>();

  protected readonly spec = computed(() => HOST_SCREENS[this.id()]);
  protected readonly min = Math.min;

  protected readonly tReq = ['Message Type = 200', 'Field 3 = "000000"', 'Field 4 = "000000200000"', 'Field 11 = "000021"', 'Field 12 = "112701"', 'Field 13 = "0812"', 'Field 22 = "0051"', 'Field 24 = "0526"', 'Field 25 = "00"', 'Field 35 = "D383B8F5515209C33DBB6E824CE6C0BA64CE4B1D32B554525D59E755987C25C041E9858B91C09628"', 'Field 41 = "BPA00001"', 'Field 42 = "BPA000000000001"', 'Field 49 = "3335"', 'Field 52 = "363BA8936414BB07"', 'Field 53 = "1A00400000341409000060007B000034140900006000"', 'Field 55 = "43 9F 26 08 FC 9C 20 54 20 F6 29 5B 9F 27 01 80', '9F 10 12 01 10 A0 40 00 2C 00 00 00 00 00 00 00'];
  protected readonly tRsp = ['Message Type = 210', 'Field 3 = "000000"', 'Field 4 = "000000200000"', 'Field 11 = "000021"', 'Field 12 = "072334"', 'Field 13 = "0826"', 'Field 22 = "0051"', 'Field 37 = "418984027644"', 'Field 38 = "000000"', 'Field 39 = "00"', 'Field 41 = "BPA00001"', 'Field 42 = "BPA000000000001"', 'Field 63 = ""'];
  protected readonly tRawReq = '012002003038058020C09B04000000000000200000000002111270108120051052600 80D383B8F5515209C33DBB6E824CE6C0BA64CE4B1D32B554525D59E755987C25C041E9858B91C0962842504130303030314250413030303030303030303031333335363BA8936414BB071A00400000341409000060007B0000341409000060007B01439F2608FC9C205420F6295B9F2701809F10120110A040002C0000…';
  protected readonly tRawRsp = '004A0210303804000EC0000200000000000200000000021072334082600513431383938343032373634343030303030303030303042504130303030313425041303030303030303030303031000';

  protected readonly txns = [['SALE', '200', '000000'], ['REVERSAL', '400', '000000'], ['VOID', '200', '020000']];
  protected readonly fieldChips = ['2 PAN', '3 Processing Code', '4 Amount', '11 STAN', '12 Time', '13 Date', '22 POS Entry Mode', '37 Retrieval Ref', '41 Terminal ID'];
  protected readonly fields = [['F2', '19 Primary account number (PAN)', '[SV]'], ['F3', 'Processing Code', '000000'], ['F4', 'Amount Transaction', '[SV]'], ['F11', 'System trace audit number (STAN)', '[SV]'], ['F12', 'Local transaction time (hhmmss)', '[TIME]'], ['F13', 'Local transaction date (MMDD)', '[TIME]'], ['F22', 'Point of service entry mode', '[SV]'], ['F37', 'Retrieval reference number', '[RAND]']];

  protected readonly rawMsg = '005902003038048020C0001400000000000099990000000771114120204002200384541822000289640D1002201203894210000003131313131313131313131313131313131313132333435363738000630303030303033000630303030303233';
  protected readonly parsedFields = [['F3', 'Processing Code', '6B'], ['F4', 'Amount Transaction', '12B'], ['F11', 'System trace audit number (STAN)', '6B'], ['F12', 'Local transaction time (hhmmss)', '6B'], ['F13', 'Local transaction date (MMDD)', '4B']];
  protected readonly f25 = [['Length', '2 bytes'], ['Type', 'BCD'], ['Hex Length', '2 chars']];

  /** 128 bit cells; the narrow layout shows the first 64. `k` is the order a set bit lights in. */
  protected readonly cells = Array.from({ length: 128 }, (_, i) => ({ n: i + 1, k: BITS.indexOf(i + 1) }));

  protected readonly delays = ['Instant', 'Realistic', 'Slow Network', 'Custom'];
  protected readonly logModes = ['Simple', 'Write Raw data', 'Write Text data', 'Parsed data'];

  protected readonly bitRows = [['Bit 1', 'BINARY', 'FIXED', '8', 'Bitmap'], ['Bit 2', 'BCD', 'LLVAR', '20', '19 Primary account number (PAN)'], ['Bit 3', 'BCD', 'FIXED', '6', 'Processing Code'], ['Bit 4', 'BCD', 'FIXED', '12', 'Amount Transaction'], ['Bit 5', 'BCD', 'FIXED', '13', 'Amount, settlement'], ['Bit 6', 'BCD', 'FIXED', '12', 'Amount, cardholder billing'], ['Bit 7', 'BCD', 'FIXED', '10', 'Transmission date & time'], ['Bit 8', 'BCD', 'FIXED', '8', 'Amount, cardholder billing fee'], ['Bit 9', 'BCD', 'FIXED', '8', 'Conversion rate, settlement'], ['Bit 10', 'BCD', 'FIXED', '8', 'Conversion rate, cardholder billing'], ['Bit 11', 'BCD', 'FIXED', '6', 'System trace audit number (STAN)'], ['Bit 12', 'BCD', 'FIXED', '12', 'Local transaction time (hhmmss)'], ['Bit 13', 'BCD', 'FIXED', '4', 'Local transaction date (MMDD)'], ['Bit 14', 'BCD', 'FIXED', '4', 'Expiration date (YYMM)'], ['Bit 15', 'BCD', 'FIXED', '6', 'Settlement date']];
  protected readonly templateOptions = ["Don't use TPDU Header", 'Respond same message if unrecognized', 'Metfone message', 'Not update screen'];
}
