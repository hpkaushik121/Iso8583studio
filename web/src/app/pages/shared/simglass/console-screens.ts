import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { HgCmd, SIM_GLASS, SimTab, hgHx } from './sim-glass';

/**
 * The HSM Command Console's screens as glass: the connected console running
 * an NO / NP exchange, Connection Settings, Message Framing (with the framed
 * bytes previewed), SSL/TLS and its cipher-suite list, A0 in the Console tab,
 * the Scenario Builder, the Load Test results and the Logs tab.
 *
 *   <app-console-screen id="overview" />
 *
 * Input: id (ConsoleScreenId, required). CONSOLE_SCREENS[id].sub is the name
 * the caption under a screen starts with. Frame, sizing and loop: sim-glass.ts.
 */

export type ConsoleScreenId =
  | 'overview' | 'connection' | 'framing' | 'ssl' | 'ciphers' | 'console' | 'scenario' | 'load' | 'logs';

export interface ConsoleScreenSpec {
  title: string;
  sub: string;
  tabs: readonly SimTab[];
  tab: number;
  steps: number;
  period: number;
  settled: number;
  aria: string;
}

const TABS: readonly SimTab[] = [['terminal-window', 'Console'], ['flow-arrow', 'Scenario'], ['gauge', 'Load Test'], ['list-dashes', 'Logs']];
const CFG_TABS: readonly SimTab[] = [[null, 'Connection Settings'], [null, 'SSL/TLS Configuration'], [null, 'Load Test Settings']];
const TITLE = 'HSM Host Console - local';
const CFG = 'HSM Host Console Configuration - local';

export const CONSOLE_SCREENS: Record<ConsoleScreenId, ConsoleScreenSpec> = {
  overview: { title: TITLE, sub: 'Console', tabs: TABS, tab: 0, steps: 9, period: 1200, settled: 4, aria: 'The HSM Host Console connected to a Thales payShield at 127.0.0.1:9090: the 120-command list on the left and an NO / NP HSM Status exchange as formatted request and response beside the framed raw bytes' },
  connection: { title: CFG, sub: 'Connection Settings', tabs: CFG_TABS, tab: 0, steps: 10, period: 1000, settled: 6, aria: 'Connection Settings: the console named HSM Host Console - local, the seven HSM type tiles with Thales payShield selected, 127.0.0.1:9090 with a 30 second timeout, and the TCP length header enabled' },
  framing: { title: CFG, sub: 'Message Framing', tabs: CFG_TABS, tab: 0, steps: 8, period: 1100, settled: 5, aria: 'Message Framing: TCP length header enabled, header format 2-byte binary length, message header 0000, empty trailer and message header length 4, with the framed NO command previewed byte by byte' },
  ssl: { title: CFG, sub: 'SSL/TLS Configuration', tabs: CFG_TABS, tab: 1, steps: 10, period: 1000, settled: 8, aria: 'SSL/TLS Configuration: encryption enabled, TLS 1.2, a PKCS#12 bundle, CA-Signed Only verification and empty client certificate, private key and keystore password fields' },
  ciphers: { title: CFG, sub: 'Cipher Suites', tabs: CFG_TABS, tab: 1, steps: 7, period: 1000, settled: 3, aria: 'The cipher suite checklist with TLS_AES_256_GCM_SHA384 and TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384 selected, each entry labelled with its strength and TLS version' },
  console: { title: TITLE, sub: 'Console', tabs: TABS, tab: 0, steps: 12, period: 900, settled: 9, aria: 'The Console tab with A0 Generate a Key selected, its six parameter fields filled in for a TDES key block, and the response panel showing the decoded key block and its check value' },
  scenario: { title: TITLE, sub: 'Scenario', tabs: TABS, tab: 1, steps: 11, period: 1100, settled: 7, aria: 'The Scenario Builder: a two-step flow, A0 Generate a Key then A6 Import a Key, where the import step references the generated key as [1][A0][KEY], run to completion' },
  load: { title: TITLE, sub: 'Load Test', tabs: TABS, tab: 2, steps: 9, period: 1000, settled: 5, aria: 'Load test results for the NO HSM Status playlist: 96 sent, 96 received, 96 successes, 0 failures, 0.9 ms average latency and 9.6 tps at a 100% success rate' },
  logs: { title: TITLE, sub: 'Logs', tabs: TABS, tab: 3, steps: 11, period: 1000, settled: 6, aria: 'The Logs tab streaming connection events and a Thales NO / NP exchange expanded into its formatted request, formatted response, raw hex and parsed views' },
};

const COMMANDS: readonly HgCmd[] = [
  ['NO', 'HSM Status', 'Diagnostics', 'n'], ['NC', 'Diagnostic Test', 'Diagnostics', 'n'], ['A0', 'Generate a Key', 'Key Management', 'p'],
  ['A6', 'Import a Key', 'Key Management', 'p'], ['A8', 'Export a Key', 'Key Management', 'p'], ['BU', 'Generate Key Check Value', 'Key Management', 'p'],
  ['CA', 'Translate PIN: TPK → ZPK', 'PIN Translation', 'b'], ['DC', 'Verify PIN (VISA PVV)', 'PIN Verification', 'g'],
  ['M0', 'Encrypt Data Block', 'Data Encryption', 'o'], ['M6', 'Generate MAC', 'MAC', 't'],
];

const NP_RSP = ['Message Header........... = [0000]', 'Command Code............. = [NP] HSM Status Response', 'Error Code............... = [00] No error', 'I/O Buffer Size.......... = [3]', 'Ethernet Type............ = [1]', 'Number of TCP Sockets.... = [64]', 'Firmware number.......... = [1448-0001]', 'DSP fitted............... = [1]', 'DSP Firmware Version..... = [9100]'];
const A0_KB = 'S10096P0TB00E0003' + hgHx('cc-a0-kb', 64) + hgHx('cc-a0-mac', 16);
const A0_KCV = hgHx('cc-a0-kcv', 6);
const TPS = [9.2, 9.8, 10.1, 9.6, 9.4, 10.2, 9.7, 9.9, 9.3, 9.6, 10.0, 9.5, 9.8, 9.6, 9.4, 10.1, 9.7, 9.5, 9.9, 9.6];

@Component({
  selector: 'app-console-screen',
  imports: [...SIM_GLASS, NgTemplateOutlet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'hg-screen' },
  template: `
    @let s = spec();
    <app-sim-glass #g [heading]="s.title" [sub]="s.sub" [tabs]="s.tabs" [tab]="s.tab" [aria]="s.aria" app="hsm-command-console" badge="CONSOLE"
                   [steps]="s.steps" [period]="s.period" [settled]="s.settled">
      @let t = g.t();
      @switch (id()) {
        @case ('overview') {
          <!-- Connect → NO selected → Send → the NO / NP exchange lands in four panes → Clear → Disconnect -->
          @let on = t >= 1 && t <= 7;
          @let req = t >= 3 && t < 6;
          @let rsp = t >= 4 && t < 6;
          <ng-container [ngTemplateOutlet]="connBar" [ngTemplateOutletContext]="{ on: on, press: t === 0 || t === 7 }" />
          <div class="hg-grid hg-g12 hg-split hg-split--240">
            <app-hg-cmdlist class="hg-w hg-list--flat" [items]="commands8" [sel]="on ? 'NO' : null" [dim]="!on" heading="Host Commands" sub="Thales payShield · 120 commands" [search]="true" [chips]="chips" />
            <div class="hg-grid hg-g10 hg-mw0 hg-ctop">
              <div class="hg-card hg-layer hg-fb hg-g12 hg-cmdbar" [style.--hd]="200">
                @if (on) {
                  <span class="hg-line hg-f hg-g12 hg-mw0"><span class="hg-ico hg-ico--36 hg-ico--blue"><ui-icon name="pulse" [style.--hi]="16" /></span><span class="hg-mw0"><span class="hg-cmdline"><span class="hg-cmd-code hg-fs15">NO</span><ui-icon name="arrow-right" class="hg-cf" [style.--hi]="11" /><span class="hg-cmd-rsp">NP</span><span class="hg-cmd-name hg-fs14">HSM Status</span></span><span class="hg-sub">Returns buffer size, socket count, firmware and DSP status. No parameters.</span></span></span>
                } @else {
                  <span class="hg-cf hg-fs11">Connect to the HSM, then pick a command from the list</span>
                }
                <span class="hg-f hg-g8 hg-none">@if (rsp) {<span class="hg-tag hg-tag--np hg-cg hg-line">NP 00 · 0.9 ms</span>}<span hgBtn kind="ghost" icon="x" [press]="t === 6" [dim]="!req">Clear</span><span hgBtn icon="paper-plane-tilt" [press]="t === 2" [dim]="!on">Send</span></span>
              </div>
              <div class="hg-grid hg-c2 hg-nc1 hg-g10">
                <app-hg-pane heading="Formatted Request" tone="req" icon="arrow-up-right" [count]="req ? 146 : 0" [h]="236" [hn]="120" [d]="300">@if (req) {<app-hg-lines [lines]="noReq" />}</app-hg-pane>
                <app-hg-pane heading="Formatted Response" tone="rsp" icon="arrow-down-left" [count]="rsp ? 400 : 0" [h]="236" [hn]="120" [d]="380">@if (rsp) {<app-hg-lines [lines]="npRsp" [step]="45" [wide]="5" />}</app-hg-pane>
                <app-hg-pane heading="Raw Request (framed)" tone="req" icon="code" [count]="req ? 10 : 0" [h]="72" [hn]="56" [d]="460">@if (req) {<app-hg-lines [lines]="noRaw" />}</app-hg-pane>
                <app-hg-pane heading="Raw Response (framed)" tone="rsp" icon="code" [count]="rsp ? 28 : 0" [h]="72" [hn]="56" [d]="540">@if (rsp) {<app-hg-lines [lines]="npRaw" />}</app-hg-pane>
              </div>
            </div>
          </div>
        }

        @case ('connection') {
          <!-- name → vendor tile (port defaults to 1500) → address, port override, timeout → length header → Save, Launch -->
          @let live = t < 9;
          @let picked = t >= 2 && live;
          @let override = t >= 4 && live;
          <div class="hg-grid hg-g12 hg-start hg-c113 hg-nc1">
            <div class="hg-card hg-layer hg-pad" [style.--hd]="160">
              <div class="hg-card-h"><div class="hg-h"><ui-icon name="info" class="hg-cb" [style.--hi]="14" />Console Identity</div></div>
              <div class="hg-grid hg-g10">
                <app-hg-fld label="Name" icon="terminal-window" [value]="live ? 'HSM Host Console - local' : ''" [on]="t === 0" />
                <app-hg-fld label="Description" icon="file-text" ph="Optional description" />
              </div>
            </div>
            <div class="hg-card hg-layer hg-pad" [style.--hd]="240">
              <div class="hg-card-h"><div class="hg-h"><ui-icon name="plugs-connected" class="hg-cb" [style.--hi]="14" />Network</div></div>
              <div class="hg-grid hg-g10 hg-c1611 hg-nc2">
                <app-hg-fld label="IP Address" icon="laptop" [value]="t >= 3 && live ? '127.0.0.1' : ''" [on]="t === 3" />
                <app-hg-fld label="Port" icon="wifi-high" [value]="override ? '9090' : picked ? '1500' : ''" [on]="t === 2 || t === 4" />
                <app-hg-fld label="Timeout (s)" icon="timer" [value]="t >= 5 && live ? '30' : ''" [on]="t === 5" />
              </div>
              <div class="hg-sub hg-f hg-g6 hg-hint">@if (picked) {<ui-icon name="info" class="hg-cb" [style.--hi]="11" /><span class="hg-line">{{ override ? 'Port overridden — the Thales payShield default is 1500' : 'Vendor defaults applied: port 1500 · 2-byte binary length' }}</span>}</div>
            </div>
          </div>
          <div class="hg-card hg-layer hg-pad" [style.--hd]="320">
            <div class="hg-card-h"><div class="hg-h"><ui-icon name="buildings" class="hg-cb" [style.--hi]="14" />HSM Type</div></div>
            <div class="hg-subline">Select the vendor — its default port and framing follow</div>
            <div class="hg-grid hg-c4 hg-nc2 hg-g8">
              @for (vendor of vendors; track vendor[0]; let i = $index) {
                <div class="hg-tile hg-tile--66" [class.on]="i === 0 && picked" [class.hov]="i === 0 && t === 1">@if (i === 0 && picked) {<ui-icon name="check-circle" [style.--hi]="15" />} @else {<ui-icon name="buildings" [style.--hi]="15" />}<span>{{ vendor[0] }}</span><small>:{{ vendor[1] }}</small></div>
              }
            </div>
          </div>
          <div class="hg-grid hg-g12 hg-mid hg-c1a hg-nc1">
            <div class="hg-card hg-layer hg-pad-row" [style.--hd]="420"><div hgTogRow icon="ruler" heading="TCP Length Header" sub="2-byte binary length prefix, big-endian" [on]="t >= 6 && live" [d]="480"></div></div>
            <span class="hg-f hg-g8 hg-end hg-wrapf"><span hgBtn kind="ghost" icon="floppy-disk" [press]="t === 7">Save Configuration</span><span hgBtn kind="teal" icon="terminal-window" [press]="t === 8">Launch Console</span></span>
          </div>
        }

        @case ('framing') {
          <!-- the header toggle, five header formats, header / trailer / length fields — and the NO command framed byte by byte -->
          @let live = t < 7;
          @let binary = t >= 1 && live;
          @let framed = t >= 4 && live;
          <div class="hg-grid hg-g12 hg-start hg-c1151 hg-nc1">
            <div class="hg-card hg-layer hg-pad" [style.--hd]="160">
              <div class="hg-card-h"><div class="hg-h"><ui-icon name="brackets-square" class="hg-cb" [style.--hi]="14" />Message Framing</div></div>
              <div hgTogRow icon="ruler" heading="TCP Length Header Enabled" sub="Prefix every message with its length" [on]="live" [d]="220"></div>
              <div class="hg-fs11 hg-b6 hg-mb8 hg-mt6">Header Format</div>
              <div class="hg-grid hg-g6">
                @for (format of formats; track format[0]; let i = $index) {
                  <div class="hg-rise hg-optline hg-optline--3" [class.on]="i === 0 && binary" [style.--hd]="300 + i * 50"><i class="hg-rd" [class.on]="i === 0 && binary"></i><span class="hg-mw0"><span class="hg-optline-t">{{ format[0] }}</span><span class="hg-sub hg-mt1">{{ format[1] }}</span></span><span class="hg-tag hg-tag--plain hg-cn hg-w">{{ format[2] }}</span></div>
                }
              </div>
              <div class="hg-grid hg-c3 hg-nc2 hg-g10 hg-mt12">
                <app-hg-fld label="Message Header (hex)" [value]="t >= 2 && live ? '0000' : ''" [on]="t === 2" />
                <app-hg-fld label="Message Trailer (hex)" ph="optional" />
                <app-hg-fld label="Message Header Length" [value]="t >= 3 && live ? '4' : ''" [on]="t === 3" />
              </div>
            </div>
            <div class="hg-grid hg-g12">
              <div class="hg-card hg-layer hg-pad" [style.--hd]="300">
                <div class="hg-card-h"><div class="hg-h"><ui-icon name="code" class="hg-cb" [style.--hi]="14" />Frame Preview</div><span class="hg-monot">NO · HSM Status</span></div>
                <div class="hg-subline">How the console puts the command on the wire</div>
                <div class="hg-f hg-g6 hg-wrapf hg-stretch">
                  <span class="hg-seg-b" [class.on]="binary" [class.hg-seg-b--b]="binary"><small>Length</small><b>{{ binary ? '00 08' : '··' }}</b><small [class.hg-off]="!binary">2 B</small></span>
                  <span class="hg-seg-b" [class.on]="t >= 2 && live" [class.hg-seg-b--t]="t >= 2 && live"><small>Header</small><b>{{ t >= 2 && live ? '30 30 30 30' : '··' }}</b><small [class.hg-off]="!(t >= 2 && live)">0000</small></span>
                  <span class="hg-seg-b" [class.on]="framed" [class.hg-seg-b--x]="framed"><small>Command</small><b>{{ framed ? '4E 4F 30 30' : '··' }}</b><small [class.hg-off]="!framed">NO00</small></span>
                  <span class="hg-seg-b"><small>Trailer</small><b>··</b><small class="hg-off">none</small></span>
                </div>
                <div class="hg-sunk hg-mono hg-frame" [class.hg-cf]="!framed">@if (framed) {<span class="hg-line">00 08 30 30 30 30 4E 4F 30 30</span>} @else {— set the framing to preview the bytes —}</div>
                <div class="hg-fb hg-g10 hg-mt8 hg-fs10 hg-cn"><span>Payload <b class="hg-cx">8 B</b></span><span>On the wire <b class="hg-cx">{{ binary ? '10 B' : '8 B' }}</b></span><span>Big-endian</span></div>
              </div>
              <div class="hg-f hg-end"><span hgBtn icon="floppy-disk" [press]="t === 5">Save</span></div>
            </div>
          </div>
        }

        @case ('ssl') {
          <!-- enable → TLS 1.2 → PKCS#12 → CA-Signed Only → CA path → Save → Test Handshake -->
          @let live = t < 9;
          <div class="hg-grid hg-g12 hg-start hg-c121 hg-nc1">
            <div class="hg-card hg-layer hg-pad" [style.--hd]="160">
              <div class="hg-card-h"><div class="hg-h"><ui-icon name="shield-check" class="hg-cb" [style.--hi]="14" />SSL/TLS Configuration</div></div>
              <div hgTogRow icon="lock" heading="Enable SSL/TLS" sub="Encrypt the socket to the HSM host interface" [on]="live" [d]="220"></div>
              <div class="hg-fadeable" [class.dim]="!live">
                <div class="hg-fs11 hg-b6 hg-mb8 hg-mt6">TLS Version</div>
                <div class="hg-f hg-g6 hg-wrapf">@for (version of versions; track version; let i = $index) {<span class="hg-chip hg-chip--mono" [class.on]="i === 2 && t >= 1 && live">{{ version }}</span>}</div>
                <app-hg-fld class="hg-mt12" label="Certificate Type" icon="certificate" [value]="t >= 2 && live ? 'PKCS#12 bundle (.p12 / .pfx)' : ''" ph="Select…" [caret]="true" [on]="t === 2" />
                <div class="hg-fs11 hg-b6 hg-mb8 hg-mt12">Certificate Verification</div>
                <div class="hg-grid hg-c2 hg-g8">
                  <div hgTile icon="shield-slash" label="No Verification"></div>
                  <div hgTile icon="shield-warning" label="Trust All Certificates"></div>
                  <div hgTile icon="seal-check" label="CA-Signed Only" [on]="t >= 3 && live"></div>
                  <div hgTile icon="certificate" label="Custom CA Authority"></div>
                </div>
              </div>
            </div>
            <div class="hg-grid hg-g12">
              <div class="hg-card hg-layer hg-pad" [style.--hd]="300">
                <div class="hg-card-h"><div class="hg-h"><ui-icon name="key" class="hg-cb" [style.--hi]="14" />Key Material</div></div>
                <div class="hg-grid hg-g10">
                  <app-hg-fld label="CA Authority" icon="certificate" [value]="t >= 4 && live ? 'certs/hsm-root-ca.pem' : ''" ph="Path to the CA bundle" [on]="t === 4" />
                  <app-hg-fld label="Client Certificate" icon="file-text" ph="Not set" />
                  <app-hg-fld label="Client Private Key" icon="key" ph="Not set" />
                  <app-hg-fld label="Keystore Password" icon="password" ph="Not set" />
                </div>
              </div>
              <div class="hg-card hg-layer hg-pad hg-fb hg-g10 hg-wrapf" [style.--hd]="400">
                @let shaken = t >= 7 && live;
                <span class="hg-swap">
                  <span class="hg-line hg-f hg-g8 hg-fs11" [class.hg-off]="!shaken"><ui-icon name="check-circle" class="hg-cg" [style.--hi]="14" /><span><b class="hg-cg">Handshake OK</b><span class="hg-cn"> · TLS 1.2 · TLS_AES_256_GCM_SHA384 · 41 ms</span></span></span>
                  <span class="hg-fs11 hg-cf" [class.hg-off]="shaken">{{ t === 6 ? 'Negotiating…' : 'Test the handshake before saving' }}</span>
                </span>
                <span class="hg-f hg-g8"><span hgBtn kind="ghost" icon="handshake" [press]="t === 6" [dim]="!live">Test Handshake</span><span hgBtn icon="floppy-disk" [press]="t === 5">Save</span></span>
              </div>
            </div>
          </div>
        }

        @case ('ciphers') {
          <!-- the checklist, strength-labelled, the two defaults ticking on -->
          @let live = t < 6;
          @let first = t >= 1 && live;
          @let second = t >= 2 && live;
          @let n = (first ? 1 : 0) + (second ? 1 : 0);
          <div class="hg-card hg-layer hg-pad" [style.--hd]="160">
            <div class="hg-card-h"><div class="hg-h"><ui-icon name="lock" class="hg-cb" [style.--hi]="14" />Cipher Suites</div><span class="hg-tag hg-tag--sel" [class.on]="n > 0">{{ n }} selected</span></div>
            <div class="hg-subline">Offered in the ClientHello, strongest first — the defaults cover TLS 1.3 and TLS 1.2 endpoints</div>
            <div class="hg-grid hg-g6">
              @for (suite of suites; track suite[0]; let i = $index) {
                @let on = (i === 0 && first) || (i === 3 && second);
                <div class="hg-rise hg-optline hg-optline--suite" [class.hg-w]="i >= 6" [class.on]="on" [style.--hd]="240 + i * 45"><span class="hg-cb-box" [class.on]="on">@if (on) {<ui-icon name="check" />}</span><span class="hg-mono hg-suite">{{ suite[0] }}</span><span class="hg-tag hg-cn hg-w">TLS {{ suite[2] }}</span><span class="hg-tag hg-strength" [class]="'hg-strength--' + suite[1]">{{ suite[1] }}</span></div>
              }
            </div>
            <div class="hg-fb hg-g10 hg-mt12 hg-wrapf"><span class="hg-fs10 hg-cf">Weak suites are listed for legacy devices only</span><span class="hg-f hg-g8"><span hgBtn kind="ghost" icon="arrows-clockwise" [press]="t === 0">Use Defaults</span><span hgBtn icon="floppy-disk" [press]="t === 4">Save</span></span></div>
          </div>
        }

        @case ('console') {
          <!-- A0 Generate a Key — six fields fill, Send, the A1 response decodes the key block and its KCV -->
          @let resp = t >= 8 && t < 11;
          @let detail = t >= 9 && t < 11;
          <ng-container [ngTemplateOutlet]="connBar" [ngTemplateOutletContext]="{ on: true, press: false }" />
          <div class="hg-grid hg-g12 hg-split hg-split--240">
            <app-hg-cmdlist class="hg-w hg-list--flat" [items]="commands" sel="A0" heading="Host Commands" sub="Thales payShield · 120 commands" [search]="true" [chips]="chips" />
            <div class="hg-grid hg-g12 hg-mw0 hg-ctop">
              <div class="hg-card hg-layer hg-pad hg-cmdhead" [style.--hd]="200">
                <span class="hg-ico hg-ico--40 hg-ico--purple"><ui-icon name="key" [style.--hi]="18" /></span>
                <span class="hg-mw0"><span class="hg-cmdline"><span class="hg-cmd-code">A0</span><ui-icon name="arrow-right" class="hg-cf" [style.--hi]="11" /><span class="hg-cmd-rsp">A1</span><span class="hg-cmd-name">Generate a Key</span><span class="hg-tag hg-tag--purple hg-cp">Key Management</span></span><span class="hg-sub hg-fs105">Generates a random key under the LMK — as a variant or a key block — and optionally under a ZMK for transport.</span></span>
              </div>
              <div class="hg-grid hg-c2 hg-nc1 hg-g12 hg-start">
                <div class="hg-card hg-layer hg-pad" [style.--hd]="300">
                  <div class="hg-card-h"><div class="hg-h hg-cb">Parameters</div><span class="hg-monot">6 fields</span></div>
                  <div class="hg-params hg-grid hg-c2 hg-g10">
                    @for (param of a0Params; track param[0]; let i = $index) {
                      <app-hg-fld [label]="param[0]" [value]="t >= i + 1 && t < 11 ? param[1] : ''" [ph]="param[2] ? 'Select…' : ''" [caret]="param[2]" [on]="t === i + 1" />
                    }
                  </div>
                  <div class="hg-wire-l">Wire Format:</div>
                  <div class="hg-mono hg-mono--wrap hg-wire">0000A0 · mode 0 · type 001 · scheme S · LMK 03 · T B 00 E  →  0000A1 00 [key-block] [KCV]</div>
                  <span hgBtn class="hg-btn--exec" icon="paper-plane-tilt" [press]="t === 7" [kind]="resp ? 'ghost' : 'pri'">Send A0</span>
                </div>
                <div class="hg-card hg-layer hg-pad hg-resp" [style.--hd]="380">
                  <div class="hg-card-h"><div class="hg-h"><ui-icon name="arrow-square-out" class="hg-cx" [style.--hi]="14" />Response</div><span class="hg-monot" [class.hg-cg]="resp">{{ resp ? 'A1 00 · 1.4 ms' : 'A1 response' }}</span></div>
                  <div class="hg-swap">
                    <div class="hg-mono hg-mono--wrap hg-fs10">
                      <div class="hg-box hg-line" [class.hg-off]="!resp"><span class="hg-box-l">Response</span>{{ a0Header }}</div>
                      <div class="hg-line hg-cg hg-mt10" [class.hg-off]="!resp" [style.--ld]="150">✓  Key generated under LMK 03 as a TDES key block</div>
                      <div class="hg-line hg-kvline" [class.hg-off]="!detail">Key (LMK)       : {{ a0Kb }}</div>
                      <div class="hg-line hg-kvline" [class.hg-off]="!detail" [style.--ld]="90">Key Check Value : {{ a0Kcv }}</div>
                      <div class="hg-line hg-kvline hg-cn" [class.hg-off]="!detail" [style.--ld]="180">Header decoded  : v1 · P0 PIN encryption · TDES · mode B · KVN 00 · exportable</div>
                      <div class="hg-rule hg-line" [class.hg-off]="!detail" [style.--ld]="260">Raw Wire Response</div>
                      <div class="hg-line hg-mt6" [class.hg-off]="!detail" [style.--ld]="340">{{ a0Raw }}</div>
                    </div>
                    <div class="hg-empty hg-empty--70" [class.hg-off]="resp">{{ t === 7 ? 'Sending…' : 'Response will appear here after sending' }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        }

        @case ('scenario') {
          <!-- name → A0 step → A6 step referencing [1][A0][KEY] → Run → both steps pass → Save -->
          @let live = t < 10;
          @let st1 = t >= 1 && live;
          @let st2 = t >= 3 && live;
          @let ref = t >= 4 && live;
          @let d1 = t >= 6 && live;
          @let d2 = t >= 7 && live;
          <div class="hg-grid hg-g12 hg-split hg-split--240">
            <div class="hg-list hg-list--flat hg-card hg-layer hg-w" [style.--hd]="160">
              <div class="hg-list-h hg-fb"><span><span class="hg-list-t">Scenarios</span><span class="hg-list-s">3 saved · 1 playlist</span></span><span class="hg-ico hg-ico--24r hg-ico--white"><ui-icon name="plus" /></span></div>
              @for (item of scenarios; track item[0]; let i = $index) {
                <div class="hg-item hg-item--16 hg-rise" [class.on]="i === 0" [style.--hd]="260 + i * 40"><ui-icon name="flow-arrow" class="hg-item-ic" [style.--hi]="14" /><span class="hg-item-x"><b>{{ item[0] }}</b><small class="hg-item-name hg-item-name--mono">{{ item[1] }}</small></span></div>
              }
              <div class="hg-list-cap">Playlists</div>
              <div class="hg-item hg-item--16 hg-item--first hg-rise" [style.--hd]="420"><ui-icon name="playlist" class="hg-item-ic" [style.--hi]="14" /><span class="hg-item-x"><b>Nightly smoke</b><small class="hg-item-name hg-item-name--mono">NO · Generate + import ZPK · auto-advance</small></span></div>
            </div>
            <div class="hg-grid hg-g12 hg-mw0 hg-ctop">
              <div class="hg-card hg-layer hg-pad hg-grid hg-g12 hg-bottom hg-c1a hg-nc1" [style.--hd]="200">
                <app-hg-fld label="Scenario name" icon="flow-arrow" [value]="live ? 'Generate + import ZPK' : ''" ph="Name this scenario" [on]="t === 0" />
                <span class="hg-f hg-g8 hg-wrapf"><span hgBtn kind="ghost" icon="plus" [press]="t === 1 || t === 3">Add Step</span><span hgBtn icon="play" [press]="t === 5" [dim]="!st2">Run Scenario</span><span hgBtn kind="ghost" icon="floppy-disk" [press]="t === 8">Save</span></span>
              </div>
              <div class="hg-card hg-layer hg-pad" [style.--hd]="300">
                <div class="hg-card-h"><div class="hg-h"><ui-icon name="list-numbers" class="hg-cb" [style.--hi]="14" />Steps</div><span class="hg-monot">{{ (st1 ? 1 : 0) + (st2 ? 1 : 0) }} steps</span></div>
                <div class="hg-swap">
                  <div class="hg-grid hg-g8 hg-ctop">
                    <div class="hg-step hg-line" [class.hg-off]="!st1" [class.done]="d1" [class.running]="!d1 && t === 5">
                      <span class="hg-ico hg-ico--step" [class.done]="d1">@if (d1) {<ui-icon name="check" [style.--hi]="11" />} @else {1}</span>
                      <span class="hg-code hg-cp">A0</span>
                      <span class="hg-mw0"><span class="hg-step-t">Generate a Key</span><span class="hg-step-f"><span class="hg-tag">Mode: <b class="hg-cx">1</b></span><span class="hg-tag">Key Type: <b class="hg-cx">001</b></span><span class="hg-tag">Scheme: <b class="hg-cx">U</b></span><span class="hg-tag">ZMK: <b class="hg-cx">{{ zmk }}</b></span></span></span>
                      <span class="hg-step-r">@if (d1) {<span class="hg-tag hg-tag--np hg-cg hg-line">A1 00 · 1.2 ms</span>} @else if (t === 5) {<span class="hg-tag hg-tag--blue hg-cb hg-line">running…</span>} @else {<ui-icon name="dots-six-vertical" class="hg-cf" [style.--hi]="13" />}</span>
                    </div>
                    <div class="hg-step hg-line" [class.hg-off]="!st2" [class.done]="d2" [class.running]="!d2 && t === 6">
                      <span class="hg-ico hg-ico--step" [class.done]="d2">@if (d2) {<ui-icon name="check" [style.--hi]="11" />} @else {2}</span>
                      <span class="hg-code hg-cp">A6</span>
                      <span class="hg-mw0"><span class="hg-step-t">Import a Key</span><span class="hg-step-f"><span class="hg-tag">Key Type: <b class="hg-cx">001</b></span><span class="hg-tag">Scheme: <b class="hg-cx">U</b></span><span class="hg-tag">ZMK: <b class="hg-cx">{{ zmk }}</b></span>@if (ref) {<span class="hg-tag hg-tag--ref hg-line">Key under ZMK: <b>[1][A0][KEY]</b></span>} @else {<span class="hg-tag">Key under ZMK: <b class="hg-cx">—</b></span>}</span></span>
                      <span class="hg-step-r">@if (d2) {<span class="hg-tag hg-tag--np hg-cg hg-line">A7 00 · 1.1 ms</span>} @else if (t === 6) {<span class="hg-tag hg-tag--blue hg-cb hg-line">running…</span>} @else {<ui-icon name="dots-six-vertical" class="hg-cf" [style.--hi]="13" />}</span>
                    </div>
                    <div class="hg-swap hg-mt4">
                      <div class="hg-strip hg-line" [class.hg-off]="!d2"><span><i class="hg-dot hg-dot--g"></i>Scenario passed · 2 / 2 steps</span><span class="hg-cn">2.3 ms total</span></div>
                      <div class="hg-sub hg-line hg-f hg-g6" [class.hg-off]="d2 || !ref"><ui-icon name="link-simple" class="hg-ct" [style.--hi]="11" /><span><b class="hg-ct">[1][A0][KEY]</b> — step 1 · command A0 · response field KEY. Values from earlier steps feed later ones.</span></div>
                    </div>
                  </div>
                  <div class="hg-empty hg-empty--56" [class.hg-off]="st1">Add a step — each step is a command code plus its field values</div>
                </div>
              </div>
            </div>
          </div>
        }

        @case ('load') {
          <!-- Start → 96 NO commands over 10 s at 9.6 tps → Complete -->
          @let p = t < 1 || t >= 8 ? 0 : min(4, t) / 4;
          @let running = t >= 1 && t < 5;
          @let done = t >= 5 && t < 8;
          @let started = running || done;
          @let n = round(96 * p);
          @let bars = round(20 * p);
          <div class="hg-card hg-layer hg-pad hg-grid hg-g10 hg-bottom hg-loadcfg" [style.--hd]="160">
            <app-hg-fld label="Playlist / Scenario" icon="playlist" value="NO HSM Status" [caret]="true" />
            <app-hg-fld label="Connections" value="1" />
            <app-hg-fld label="Commands / s" value="10" />
            <app-hg-fld label="Duration (s)" value="10" />
            <app-hg-fld label="Pattern" value="Constant Rate" [caret]="true" />
            @if (running) { <span hgBtn class="hg-btn--34" kind="danger" icon="stop">Stop</span> } @else { <span hgBtn class="hg-btn--34" icon="play" [press]="t === 0">Start Load Test</span> }
          </div>
          <div class="hg-card hg-layer hg-progress" [style.--hd]="260">
            <div class="hg-fb hg-fs105 hg-mb8"><span class="hg-f hg-g8 hg-b6"><i class="hg-dot hg-dot--flat" [class.hg-dot--g]="done" [class.hg-dot--b]="running" [class.hg-dot--idle]="!started"></i>{{ done ? 'Complete' : running ? 'Running' : 'Idle' }}</span><span class="hg-monot">{{ (p * 10).toFixed(1) }} s / 10 s</span></div>
            <div class="hg-track"><div class="hg-track-bar" [style.--v]="p * 100"></div></div>
          </div>
          <div class="hg-grid hg-c4 hg-nc2 hg-g10">
            <div hgStat icon="paper-plane-tilt" tone="blue" [n]="n" label="Sent" [d]="340"></div>
            <div hgStat icon="arrow-down-left" tone="teal" [n]="n" label="Received" [d]="400"></div>
            <div hgStat icon="check-circle" tone="green" [n]="n" label="Successes" [d]="460"></div>
            <div hgStat icon="x-circle" tone="red" [n]="started ? 0 : '—'" label="Failures" [d]="520"></div>
          </div>
          <div class="hg-grid hg-g10 hg-loadrow">
            <div hgStat icon="timer" tone="text" [n]="started ? '0.9 ms' : '—'" label="Avg Latency" [d]="580"></div>
            <div hgStat icon="gauge" tone="amber" [n]="started ? '9.6' : '—'" label="TPS" [d]="640"></div>
            <div hgStat icon="percent" tone="ok" [n]="started ? '100%' : '—'" label="Success Rate" [d]="700"></div>
            <div class="hg-card hg-layer hg-progress hg-tput" [style.--hd]="760">
              <div class="hg-card-h"><div class="hg-h"><ui-icon name="chart-bar" class="hg-cb" [style.--hi]="14" />Throughput</div><span class="hg-monot">target 10 / s</span></div>
              <div class="hg-bars"><i class="hg-bars-target"></i>@for (bar of tps; track $index; let i = $index) {<span class="hg-bar" [class.on]="i < bars" [style.--v]="bar"></span>}</div>
            </div>
          </div>
        }

        @case ('logs') {
          <!-- connection events, the NO / NP exchange, its entry expanded into four views, then cleared -->
          @let n = t >= 9 ? 0 : min(t + 1, 6);
          @let open = t >= 4 && t < 9;
          @let tab = open ? min(t - 4, 3) : 0;
          <div class="hg-card hg-layer hg-logs hg-logs--360" [style.--hd]="160">
            <div class="hg-logs-h hg-wrapf hg-g10">
              <div class="hg-f hg-g14"><div class="hg-h hg-cb"><ui-icon name="list-dashes" [style.--hi]="15" />Logs</div><span class="hg-f hg-g10 hg-fs10 hg-cn hg-w"><span>Connections <b class="hg-cx">{{ n >= 2 ? 1 : 0 }}</b></span><span>Bytes in <b class="hg-cx">{{ n >= 4 ? 28 : 0 }}</b></span><span>Bytes out <b class="hg-cx">{{ n >= 3 ? 10 : 0 }}</b></span></span></div>
              <div class="hg-f hg-g6"><span hgBtn class="hg-btn--24" kind="ghost" icon="funnel">Filter</span><span hgBtn class="hg-btn--24" kind="ghost" icon="arrow-line-down">Auto</span><span class="hg-ico hg-ico--24 hg-ico--press" [class.press]="t === 8"><ui-icon name="trash" class="hg-cb" /></span></div>
            </div>
            <div class="hg-swap hg-logs-b">
              <div class="hg-mono hg-mono--wrap hg-logs-lines">
                @for (line of logLines; track $index; let i = $index) {
                  <div class="hg-line hg-logline" [class.hg-off]="i >= n"><span class="hg-cn">[{{ line[0] }}]</span><span class="hg-b6" [class.hg-cp]="line[1] === 'HSM'" [class.hg-cb]="line[1] !== 'HSM'">{{ line[1] }}</span><span class="hg-logtext"><span class="hg-mw0"><span class="hg-cb">: </span>{{ line[2] }}</span>@if (i === 3) {@if (open) {<ui-icon name="caret-down" class="hg-cb" [style.--hi]="10" />} @else {<ui-icon name="caret-right" class="hg-cb" [style.--hi]="10" />}}</span></div>
                  @if (i === 3) {
                    <div class="hg-sunk hg-line hg-logopen" [class.hg-off]="!open">
                      <div class="hg-f hg-g5 hg-mb8 hg-wrapf">@for (label of logTabs; track label; let k = $index) {<span class="hg-chip hg-chip--sm hg-chip--sans" [class.on]="k === tab">{{ label }}</span>}</div>
                      <div class="hg-logview">
                        @switch (tab) {
                          @case (0) { <app-hg-lines [lines]="noReq" [step]="40" /> }
                          @case (1) { <app-hg-lines [lines]="npRsp7" [step]="35" [wide]="5" /> }
                          @case (2) { <app-hg-lines [lines]="rawHex" [step]="45" /> }
                          @case (3) {
                            <div class="hg-grid hg-c2 hg-nc1 hg-parsed">@for (row of npParsed; track row[0]; let j = $index) {<div class="hg-line hg-parsed-row" [style.--ld]="j * 30"><span class="hg-cn">{{ row[0] }}</span><b class="hg-cx">{{ row[1] }}</b></div>}</div>
                          }
                        }
                      </div>
                    </div>
                  }
                }
              </div>
              <div class="hg-mono hg-logs-wait" [class.hg-off]="n > 0">› waiting for traffic<span class="hg-caret" aria-hidden="true"></span></div>
            </div>
            <div class="hg-logs-f hg-wrapf hg-g10"><span>Entries: {{ n }}/6</span><span>Global logging: <b class="hg-cg">ON</b></span><span>Auto-scroll: <b class="hg-cg">ON</b></span></div>
          </div>
        }
      }

      <!-- the connection header every console-tab screen carries -->
      <ng-template #connBar let-on="on" let-press="press">
        <div class="hg-card hg-layer hg-ctl hg-ctl--conn" [style.--hd]="160">
          <span class="hg-f hg-g10 hg-mw0">
            <i class="hg-dot hg-dot--conn" [class.on]="on"></i>
            <span class="hg-l115">{{ on ? 'Connected' : 'Disconnected' }}</span>
            <span class="hg-monot">Thales payShield · 127.0.0.1:9090</span>
            <span class="hg-tag hg-cn hg-w">2-byte length</span>
            <span class="hg-tag hg-cf hg-w">TLS off</span>
          </span>
          <span class="hg-f hg-g10">
            @if (on) { <span class="hg-fs10 hg-cn hg-w">Timeout <b class="hg-cx">30 s</b></span> }
            @if (on) { <span hgBtn kind="danger" icon="plugs" [press]="press">Disconnect</span> } @else { <span hgBtn icon="plugs-connected" [press]="press">Connect</span> }
          </span>
        </div>
      </ng-template>
    </app-sim-glass>
  `,
})
export class ConsoleScreen {
  readonly id = input.required<ConsoleScreenId>();

  protected readonly spec = computed(() => CONSOLE_SCREENS[this.id()]);
  protected readonly min = Math.min;
  protected readonly round = Math.round;

  protected readonly commands = COMMANDS;
  protected readonly commands8 = COMMANDS.slice(0, 8);
  protected readonly chips = ['All', 'Diagnostics', 'Key Management', 'PIN', 'MAC'];
  protected readonly noReq = ['Message Header........... = [0000]', 'Command Code............. = [NO] HSM Status Request', 'Mode Flag................ = [00] Status Info'];
  protected readonly npRsp = NP_RSP;
  protected readonly npRsp7 = NP_RSP.slice(0, 7);
  protected readonly noRaw = ['0000NO00', 'LEN 00 08 · 8 B payload · 10 B on the wire'];
  protected readonly npRaw = ['0000NP0031641448-000119100', 'LEN 00 1A · 26 B payload · 28 B on the wire'];

  /** [vendor, default port] */
  protected readonly vendors = [['Thales payShield', '1500'], ['Futurex Excrypt', '2000'], ['SafeNet Luna', '1500'], ['Utimaco CryptoServer', '3001'], ['nCipher nShield', '9004'], ['Utimaco Atalla', '7000'], ['Generic HSM', '1500']];
  /** [name, description, used by] */
  protected readonly formats = [['2-byte Binary Length', 'Two-byte big-endian length prefix', 'Thales · Futurex · Luna · Utimaco'], ['4-byte ASCII Length', 'Four ASCII digits of length', 'nCipher nShield'], ['STX / ETX Framing', 'Start / end control bytes bracket the message', 'Utimaco Atalla'], ['No Header / Framing', 'Raw payload, no length prefix', '—'], ['Custom Header', 'User-defined header / trailer bytes', '—']];
  protected readonly versions = ['TLS 1.0', 'TLS 1.1', 'TLS 1.2', 'TLS 1.3'];
  /** [suite, strength, TLS version]; the narrow layout shows the first six. */
  protected readonly suites = [['TLS_AES_256_GCM_SHA384', 'Strong', '1.3'], ['TLS_AES_128_GCM_SHA256', 'Strong', '1.3'], ['TLS_CHACHA20_POLY1305_SHA256', 'Strong', '1.3'], ['TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384', 'Strong', '1.2'], ['TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384', 'Strong', '1.2'], ['TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256', 'Strong', '1.2'], ['TLS_RSA_WITH_AES_256_CBC_SHA256', 'Medium', '1.2'], ['TLS_RSA_WITH_AES_128_CBC_SHA', 'Weak', '1.2'], ['TLS_RSA_WITH_3DES_EDE_CBC_SHA', 'Weak', '1.2']];

  /** [label, value, is a drop-down] */
  protected readonly a0Params: readonly (readonly [string, string, boolean])[] = [['Mode', '0 — Generate key', true], ['Key Type', '001 — ZPK', true], ['Key Scheme (LMK)', 'S — Key block', true], ['LMK Identifier', '03', false], ['Algorithm', 'T — TDES', true], ['Exportability', 'E — Exportable', true]];
  protected readonly a0Header = 'Header        : 0000\nResponse Code : A1    (command A0 → A1)\nError Code    : 00    ✓ Success — no error';
  protected readonly a0Kb = A0_KB;
  protected readonly a0Kcv = A0_KCV;
  protected readonly a0Raw = '0000A100' + A0_KB + A0_KCV;

  protected readonly scenarios = [['Generate + import ZPK', '2 steps · A0 → A6'], ['PIN translate TPK → ZPK', '3 steps · A0 → CA → DC'], ['Key check loop', '2 steps · A0 → BU']];
  protected readonly zmk = 'U' + hgHx('cc-zmk', 32).slice(0, 8) + '…';

  /** Commands per second, second by second; drawn against a 12 / s ceiling. */
  protected readonly tps = TPS.map((v) => (v / 12) * 100);

  protected readonly logLines: readonly (readonly [string, string, string])[] = [
    ['22:14:02.118', 'INFO', 'Connecting to 127.0.0.1:9090 (Thales payShield · 2-byte binary length)…'],
    ['22:14:02.131', 'INFO', 'Connected · socket #1 · TLS off · timeout 30 s'],
    ['22:14:05.402', 'HSM', '▸ CMD NO  →  0000NO00  (10 B on the wire)'],
    ['22:14:05.403', 'HSM', '◂ RSP NP  ←  0000NP0031641448-000119100  (28 B · 0.9 ms)'],
    ['22:14:05.403', 'INFO', 'AuditEntry(timestamp=2026-09-21T22:14:05.403, entryType=HOST_COMMAND, command=NO, response=NP, errorCode=00, latencyMs=0.9, result=SUCCESS)'],
    ['22:14:09.870', 'INFO', 'Idle · keep-alive sent'],
  ];
  protected readonly logTabs = ['Formatted Request', 'Formatted Response', 'Raw Hex', 'Parsed'];
  protected readonly rawHex = ['REQ  00 08 | 30 30 30 30 4E 4F 30 30', 'RSP  00 1A | 30 30 30 30 4E 50 30 30 33 31 36 34 31 34 34 38 2D', '             30 30 30 31 31 39 31 30 30'];
  protected readonly npParsed = [['Command', 'NP'], ['Error Code', '00 · No error'], ['I/O Buffer Size', '3'], ['Ethernet Type', '1'], ['TCP Sockets', '64'], ['Firmware', '1448-0001'], ['DSP fitted', '1'], ['DSP Firmware', '9100']];
}
