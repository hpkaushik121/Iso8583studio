import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { HgCmd, SIM_GLASS, SimTab, hgHx } from './sim-glass';

/**
 * The HSM Simulator's own screens as glass: HSM Handler, Key Management,
 * Host Commands (NC, A0, CA, DC), Secure Commands, Logs, the Configuration
 * Profile and Network tabs, and an LMK slot.
 *
 *   <app-hsm-screen id="handler" />
 *
 * Input: id (HsmScreenId, required). HSM_SCREENS[id].sub is the name the
 * caption under a screen starts with. Frame, sizing and loop: sim-glass.ts.
 */

export type HsmScreenId =
  | 'handler' | 'keys' | 'host-nc' | 'host-a0' | 'host-ca' | 'host-dc' | 'secure' | 'logs' | 'profile' | 'network' | 'lmk';

type HostCmdCode = 'NC' | 'A0' | 'CA' | 'DC';

export interface HsmScreenSpec {
  title: string;
  sub: string;
  tabs: readonly SimTab[];
  tab: number;
  /** Which body to draw. */
  body: 'handler' | 'keys' | 'host' | 'secure' | 'logs' | 'profile' | 'network' | 'lmk';
  /** Host Commands only: the command on show. */
  cmd?: HostCmdCode;
  steps: number;
  period: number;
  settled: number;
  aria: string;
}

const APP_TABS: readonly SimTab[] = [
  ['arrows-left-right', 'HSM Handler'], ['key', 'Key Management'], ['terminal-window', 'Host Commands'],
  ['lock', 'Secure Commands'], ['list-dashes', 'Logs'],
];
const CFG_TABS: readonly SimTab[] = [[null, 'Profile'], [null, 'Network'], [null, 'Security'], [null, 'Keys'], [null, 'Advanced']];

interface HostCmd {
  rsp: string;
  name: string;
  desc: string;
  /** [label, value, is a drop-down] */
  params: readonly (readonly [string, string, boolean?])[];
  wire: string;
  fields: readonly (readonly [string, string])[];
  raw: string;
}

const HOST_CMDS: Record<HostCmdCode, HostCmd> = {
  NC: { rsp: 'ND', name: 'Diagnostic Test', desc: 'Health-check command. Confirms HSM connectivity and LMK status.', params: [], wire: '0000NC  →  0000ND 00 [status]', fields: [['Status', 'NC005BCF24BE833E0010-E000']], raw: '0000ND00NC005BCF24BE833E0010-E000' },
  A0: { rsp: 'A1', name: 'Generate Key', desc: 'Generates a new DES/3DES key encrypted under LMK. Optionally also encrypted under a ZMK for transport.', params: [['Mode', '0 — Generate key under LMK', true], ['Key Type', '001 — ZPK', true], ['Key Scheme (LMK)', 'U — Double-length', true]], wire: '0000A0001U  →  0000A1 00 [key-under-LMK] [KCV]', fields: [['Key (LMK)', 'U' + hgHx('a0k', 32)], ['KCV', hgHx('a0kcv', 6)]], raw: '0000A100U' + hgHx('a0k', 32) + hgHx('a0kcv', 6) },
  CA: { rsp: 'CB', name: 'Translate PIN: TPK → ZPK', desc: 'Re-encrypts a PIN block from Terminal PIN Key to Zone PIN Key.', params: [['Source TPK (under LMK)', 'U' + hgHx('tpk', 32)], ['Destination ZPK (under LMK)', 'U' + hgHx('zpk', 32)], ['Maximum PIN Length', '12'], ['Source PIN Block', hgHx('pb', 16)], ['Source Format', '01 — ISO 9564-1 Format 0', true], ['Destination Format', '01 — ISO 9564-1 Format 0', true], ['Account Number', '476173000000']], wire: '0000CA[TPK][ZPK]12[PIN]01[ACCT]01[ACCT]  →  0000CB 00 [len] [pin-block]', fields: [['PIN Length', '04'], ['PIN Block (ZPK)', hgHx('cbpb', 16)]], raw: '0000CB0004' + hgHx('cbpb', 16) },
  DC: { rsp: 'DD', name: 'Verify PIN (VISA PVV)', desc: 'Verifies a PIN against a VISA Pin Verification Value (PVV).', params: [['ZPK (under LMK)', 'U' + hgHx('zpk2', 32)], ['PVK Pair (under LMK)', 'U' + hgHx('pvk', 32)], ['PIN Block', hgHx('pb2', 16)], ['PIN Block Format', '01 — ISO 9564-1 Format 0', true], ['Account Number', '476173000000'], ['PVKI', '1'], ['PVV', '5732']], wire: '0000DC[ZPK]01[PIN][ACCT][PVK][PVV]  →  0000DD 00', fields: [['Result', 'PIN verified successfully']], raw: '0000DD00' },
};

const HOST_LIST: readonly HgCmd[] = [
  ['NC', 'Diagnostic Test', 'Diagnostics', 'n'], ['CA', 'Translate PIN: TPK → ZPK', 'PIN Translation', 'b'],
  ['G0', 'Translate PIN: DUKPT (BDK) → BDK/ZPK', 'PIN Translation', 'b'], ['JC', 'Translate PIN: TPK → LMK', 'PIN Translation', 'b'],
  ['JE', 'Translate PIN: ZPK → LMK', 'PIN Translation', 'b'], ['JG', 'Translate PIN: LMK → ZPK', 'PIN Translation', 'b'],
  ['DA', 'Verify PIN (IBM 3624)', 'PIN Verification', 'g'], ['DC', 'Verify PIN (VISA PVV)', 'PIN Verification', 'g'],
  ['DE', 'Generate IBM PIN Offset', 'PIN Generation', 'o'], ['DG', 'Generate VISA PVV', 'PIN Generation', 'o'],
  ['EE', 'Derive PIN (IBM 3624 Offset)', 'PIN Generation', 'o'], ['A0', 'Generate Key', 'Key Management', 'p'],
];

const SEC_LIST: readonly HgCmd[] = [
  ['NC', 'Diagnostic Test', 'Diagnostic', 'n', 'shield'], ['VR', 'Version Info', 'Diagnostic', 'n', 'shield'],
  ['VT', 'View LMK Table', 'LMK', 'p', 'lock'], ['GK', 'Generate LMK Component', 'LMK', 'p', 'lock'],
  ['GC', 'Generate Key Component', 'Key Gen', 'b', 'key'], ['A0', 'Generate Key', 'Key Gen', 'b', 'key'],
  ['FK', 'Form Key from Components', 'Key Gen', 'b', 'key'], ['LA', 'Load Data to User Storage', 'Key Mgmt', 'g', 'users-three'],
  ['LE', 'Read from User Storage', 'Key Mgmt', 'g', 'users-three'], ['LD', 'Delete from User Storage', 'Key Mgmt', 'g', 'users-three'],
  ['BW', 'Translate Key (LMK Migration)', 'LMK', 'p', 'lock'], ['BG', 'Translate PIN (LMK Migration)', 'PIN', 'o', 'keyboard'],
];

const LMK_TABLE = [
  'ID | Authorized | Scheme      | Algorithm   | Status | Check  | Comments',
  '---|------------|-------------|-------------|--------|--------|----------',
  '00 | No         | VARIANT     | 3DES(2key)  | Live   | 5BCF24 | [Default]',
  '03 | No         | KEY_BLOCK   | AES-256     | Live   | AFC685 | [Default]',
  '', 'Key Change Storage Table:', 'No keys loaded in key change storage',
];

const TITLE = 'HSM Simulator - Thales PayShield 10k';
const CFG_TITLE = 'HSM Simulator Configuration';

const host = (cmd: HostCmdCode, aria: string): HsmScreenSpec => {
  const p = HOST_CMDS[cmd].params.length;
  return { title: TITLE, sub: 'Host Commands', tabs: APP_TABS, tab: 2, body: 'host', cmd, steps: p + 6, period: p ? 950 : 1200, settled: p + 3, aria };
};

export const HSM_SCREENS: Record<HsmScreenId, HsmScreenSpec> = {
  handler: { title: TITLE, sub: 'HSM Handler', tabs: APP_TABS, tab: 0, body: 'handler', steps: 7, period: 1200, settled: 3, aria: 'The HSM Handler tab: the server started on 0.0.0.0:9090, an NO / NP HSM State exchange in the formatted request, formatted response and raw hex panes' },
  keys: { title: TITLE, sub: 'Key Management', tabs: APP_TABS, tab: 1, body: 'keys', steps: 6, period: 1300, settled: 2, aria: 'The Key Management overview: 99 LMK slots, two loaded, and the slot map' },
  'host-nc': host('NC', 'The Host Commands tab running NC Diagnostic Test and reading the ND response'),
  'host-a0': host('A0', 'The Host Commands tab running A0 Generate Key for a ZPK under the LMK'),
  'host-ca': host('CA', 'The Host Commands tab translating a PIN block from TPK to ZPK with CA'),
  'host-dc': host('DC', 'The Host Commands tab verifying a PIN against a VISA PVV with DC'),
  secure: { title: TITLE, sub: 'Secure Commands', tabs: APP_TABS, tab: 3, body: 'secure', steps: 7, period: 1500, settled: 4, aria: 'The Secure Commands tab: console authorization granted through the custodian dialog, then VT View LMK Table executed' },
  logs: { title: TITLE, sub: 'Logs', tabs: APP_TABS, tab: 4, body: 'logs', steps: 9, period: 1000, settled: 6, aria: 'The Logs tab: an NC diagnostic and a VT LMK table exchange with their audit entries' },
  profile: { title: CFG_TITLE, sub: 'Profile', tabs: CFG_TABS, tab: 0, body: 'profile', steps: 9, period: 1000, settled: 5, aria: 'HSM Simulator Configuration on the Profile tab: the profile list, basic information, the six vendor tiles with Thales selected, model and firmware' },
  network: { title: CFG_TITLE, sub: 'Network', tabs: CFG_TABS, tab: 1, body: 'network', steps: 8, period: 1000, settled: 5, aria: 'HSM Simulator Configuration on the Network tab: TCP_IP selected, 0.0.0.0:9090, the length header on with a 4-character message header, SSL/TLS off' },
  lmk: { title: TITLE, sub: 'LMK Slot 00', tabs: APP_TABS, tab: 1, body: 'lmk', steps: 10, period: 900, settled: 8, aria: 'LMK Slot 00 loaded: VARIANT scheme, 3DES 2-key, 40 pairs, its check value and the derived key block protection key' },
};

@Component({
  selector: 'app-hsm-screen',
  imports: [...SIM_GLASS],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'hg-screen' },
  template: `
    @let s = spec();
    <app-sim-glass #g [heading]="s.title" [sub]="s.sub" [tabs]="s.tabs" [tab]="s.tab" [aria]="s.aria"
                   [steps]="s.steps" [period]="s.period" [settled]="s.settled">
      @let t = g.t();
      @switch (s.body) {
        @case ('handler') {
          <!-- Start → NO / NP exchange in the four panes → Clear → Stop -->
          @let active = t < 6;
          @let req = t >= 1 && t < 5;
          @let rsp = t >= 2 && t < 5;
          <div class="hg-strip hg-line" [class.hg-off]="!active"><span><i class="hg-dot hg-dot--g"></i>Active <b class="hg-strip-addr">0.0.0.0:9090</b></span><span class="hg-cn">Count: {{ rsp ? 1 : 0 }}</span></div>
          <div class="hg-card hg-layer hg-ctl" [style.--hd]="160">
            <span class="hg-f hg-g8">
              @if (active) { <span hgBtn kind="danger" icon="stop">Stop</span> } @else { <span hgBtn icon="play">Start</span> }
              <span hgBtn kind="ghost" icon="x" [press]="t === 5">Clear</span>
              @if (!active) { <ui-icon name="gear" class="hg-cn" [style.--hi]="14" /><span class="hg-cn">Gateway Stopped</span> }
            </span>
            <span class="hg-f hg-g12 hg-cn">
              @if (active) { <span class="hg-w">Connection Count <b class="hg-cx">10</b></span> }
              <span class="hg-f hg-g6"><i class="hg-tog"></i>Hold</span>
              <span class="hg-sunk hg-hold">60</span>
              <span hgBtn [kind]="active ? 'pri' : 'ghost'" icon="paper-plane-tilt" [dim]="!active">Send</span>
            </span>
          </div>
          <div class="hg-grid hg-c2 hg-nc1 hg-g10">
            <app-hg-pane heading="Formatted Request" tone="req" icon="arrow-up-right" [count]="req ? 146 : 0" [h]="250" [hn]="120" [d]="300">@if (req) {<app-hg-lines [lines]="hReq" />}</app-hg-pane>
            <app-hg-pane heading="Formatted Response" tone="rsp" icon="arrow-down-left" [count]="rsp ? 400 : 0" [h]="250" [hn]="120" [d]="380">@if (rsp) {<app-hg-lines [lines]="hRsp" [step]="45" />@if (t >= 3) {<app-hg-lines [lines]="hDbg" [step]="45" />}}</app-hg-pane>
            <app-hg-pane heading="Raw Request (Hex)" tone="req" icon="code" [count]="req ? 9 : 0" [h]="72" [hn]="56" [d]="460">@if (req) {<div class="hg-line">HDR1NO00</div>}</app-hg-pane>
            <app-hg-pane heading="Raw Response (Hex)" tone="rsp" icon="code" [count]="rsp ? 27 : 0" [h]="72" [hn]="56" [d]="540">@if (rsp) {<div class="hg-line">HDR1NP0031641448-000119100</div>}</app-hg-pane>
          </div>
        }

        @case ('keys') {
          <!-- Key Management → Overview: slot counters and the LMK slot map filling in -->
          @let loaded = t < 1 || t === 5 ? 0 : t === 1 ? 1 : 2;
          <div class="hg-card hg-layer hg-subtabs" [style.--hd]="160">
            <div class="hg-subtab on"><ui-icon name="squares-four" [style.--hi]="14" />Overview</div>
            <div class="hg-subtab"><ui-icon name="key" [style.--hi]="14" /><span><span class="hg-w">LMK Details</span><span class="hg-n">LMK</span></span></div>
            <div class="hg-subtab"><ui-icon name="list" [style.--hi]="14" /><span><span class="hg-w">User Storage</span><span class="hg-n">User</span></span></div>
            <div class="hg-subtab"><ui-icon name="clock-counter-clockwise" [style.--hi]="14" /><span><span class="hg-w">Operations Log</span><span class="hg-n">Operations</span></span></div>
          </div>
          <div class="hg-grid hg-c3 hg-g10">
            <div hgStat icon="squares-four" tone="blue" [n]="99" label="Total Slots" [d]="300"></div>
            <div hgStat icon="check-circle" tone="green" [n]="loaded" label="Loaded" [d]="380"></div>
            <div hgStat icon="circle" tone="grey" [n]="99 - loaded" label="Empty" [d]="460"></div>
          </div>
          <div class="hg-card hg-layer hg-pad" [style.--hd]="560">
            <div class="hg-card-h"><div class="hg-h"><ui-icon name="squares-four" class="hg-cb" [style.--hi]="14" />LMK Slot Map</div><span class="hg-cn">{{ loaded }} / 100 Loaded</span></div>
            <div class="hg-legend"><span><i class="hg-dot hg-dot--b"></i>Default</span><span><i class="hg-dot hg-dot--p"></i>Management</span><span><i class="hg-dot hg-dot--g"></i>Loaded</span><span><i class="hg-dot hg-dot--e"></i>Empty (click to generate)</span></div>
            <div class="hg-slots">
              @for (i of slots; track i) {
                @let on = (i === 0 && loaded >= 1) || (i === 3 && loaded >= 2);
                <div class="hg-slot hg-rise" [class.on]="on" [class.hov]="!on && t === 3 && i === 5" [style.--hd]="620 + i * 12"><ui-icon [name]="on ? 'star' : 'plus-circle'" [style.--hi]="13" />{{ pad(i) }}<small>{{ on ? '40p' : 'Empty' }}</small></div>
              }
            </div>
          </div>
        }

        @case ('host') {
          <!-- Host Commands: fill the parameters, execute, read the formatted response -->
          @let c = command();
          @let p = c.params.length;
          @let resp = t >= p + 2 && t < p + 5;
          @let detail = t >= p + 3 && t < p + 5;
          <div class="hg-grid hg-g12 hg-start hg-split">
            <app-hg-cmdlist class="hg-w" [items]="list()" [sel]="s.cmd ?? null" heading="Host Commands" sub="No smart-card authorization required" [search]="true" [chips]="hostChips" />
            <div class="hg-grid hg-g12 hg-mw0">
              <div class="hg-card hg-layer hg-pad hg-cmdhead" [style.--hd]="200">
                <span class="hg-ico hg-ico--40 hg-ico--blue"><ui-icon name="shield" [style.--hi]="18" /></span>
                <span class="hg-mw0"><span class="hg-cmdline"><span class="hg-cmd-code">{{ s.cmd }}</span><ui-icon name="arrow-right" class="hg-cf" [style.--hi]="11" /><span class="hg-cmd-rsp">{{ c.rsp }}</span><span class="hg-cmd-name">{{ c.name }}</span></span><span class="hg-sub hg-fs105">{{ c.desc }}</span></span>
              </div>
              <div class="hg-grid hg-c2 hg-nc1 hg-g12 hg-start">
                <div class="hg-card hg-layer hg-pad" [style.--hd]="300">
                  <div class="hg-card-h"><div class="hg-h hg-cb">Parameters</div></div>
                  @if (p === 0) {
                    <div class="hg-params hg-params--none">No parameters required for this command.</div>
                  } @else {
                    <div class="hg-params hg-grid hg-c2 hg-g10">
                      @for (param of c.params; track param[0]; let i = $index) {
                        <app-hg-fld [class.hg-span2]="param[0].length > 22" [label]="param[0]" [value]="t >= i + 1 && t < s.steps - 1 ? param[1] : ''"
                                    [ph]="param[2] ? 'Select…' : ''" [caret]="!!param[2]" [on]="t === i + 1" />
                      }
                    </div>
                  }
                  <div class="hg-wire-l">Wire Format:</div>
                  <div class="hg-mono hg-mono--wrap hg-wire">{{ c.wire }}</div>
                  <span hgBtn class="hg-btn--exec" icon="play" [press]="t === p + 1" [kind]="resp ? 'ghost' : 'pri'">Execute {{ s.cmd }}</span>
                </div>
                <div class="hg-card hg-layer hg-pad hg-resp" [style.--hd]="380">
                  <div class="hg-card-h"><div class="hg-h"><ui-icon name="arrow-square-out" class="hg-cx" [style.--hi]="14" />Response</div><span class="hg-monot">{{ c.rsp }} response</span></div>
                  <div class="hg-swap">
                    <div class="hg-mono hg-mono--wrap hg-fs10">
                      <div class="hg-box hg-line" [class.hg-off]="!resp"><span class="hg-box-l">Response</span>{{ header(s.cmd!, c.rsp) }}</div>
                      <div class="hg-line hg-cg hg-mt10" [class.hg-off]="!resp" [style.--ld]="150">✓  Command executed successfully</div>
                      @for (field of c.fields; track field[0]; let i = $index) {
                        <div class="hg-line hg-kvline" [class.hg-off]="!detail" [style.--ld]="i * 90">{{ field[0] + ' : ' + field[1] }}</div>
                      }
                      <div class="hg-rule hg-line" [class.hg-off]="!detail" [style.--ld]="260">Raw Wire Response</div>
                      <div class="hg-line hg-mt6" [class.hg-off]="!detail" [style.--ld]="340">{{ c.raw }}</div>
                    </div>
                    <div class="hg-empty hg-empty--70" [class.hg-off]="resp">Response will appear here after executing</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        }

        @case ('secure') {
          <!-- Secure Commands: not authorized → custodian dialog → authorized → Execute VT → LMK table -->
          @let auth = t >= 2 && t < 6;
          @let resp = t >= 4 && t < 6;
          <div class="hg-banner hg-layer" [class.ok]="auth" [class.warn]="!auth" [style.--hd]="160">
            <span class="hg-f hg-g12">
              @if (auth) { <ui-icon name="lock-open" class="hg-cg" [style.--hi]="18" /> } @else { <ui-icon name="lock" class="hg-cs" [style.--hi]="18" /> }
              <span><span class="hg-banner-t">{{ auth ? 'Console Authorized' : 'Console Not Authorized' }}</span><span class="hg-sub">{{ auth ? 'Valid for 07:59:56  ·  Granted to all console activities' : 'Secure commands require console authorization. In a real HSM, custodian cards must be inserted.' }}</span></span>
            </span>
            @if (auth) { <span hgBtn kind="rev">Revoke</span> } @else { <span hgBtn kind="warn" icon="key">Authorize Console</span> }
          </div>
          <div class="hg-grid hg-g12 hg-start hg-split">
            <app-hg-cmdlist class="hg-w" [items]="secList" sel="VT" heading="Secure Commands" sub="Administrator / LMK Operations" />
            <div class="hg-grid hg-g12 hg-mw0">
              <div class="hg-card hg-layer hg-pad hg-cmdhead" [style.--hd]="200">
                <span class="hg-ico hg-ico--40 hg-ico--purple"><ui-icon name="lock" [style.--hi]="18" /></span>
                <span><span class="hg-cmdline hg-cmdline--10"><span class="hg-cmd-code">VT</span><span class="hg-cmd-name">View LMK Table</span></span><span class="hg-sub hg-fs105">Displays the KCVs of all loaded LMK pairs</span></span>
              </div>
              <div class="hg-grid hg-g12 hg-start hg-sec-cols">
                <div class="hg-card hg-layer hg-pad" [style.--hd]="300">
                  <div class="hg-card-h"><div class="hg-h hg-cb">Parameters</div></div>
                  <div class="hg-wire-l hg-wire-l--first">Wire format:</div>
                  <div class="hg-mono hg-wire">HEADVT → HEADVU 00 [KCV_TABLE]</div>
                  <span hgBtn class="hg-btn--exec" icon="play" [press]="t === 3" [dim]="!auth">Execute VT</span>
                </div>
                <div class="hg-card hg-layer hg-pad hg-resp hg-resp--200" [style.--hd]="380">
                  <div class="hg-card-h"><div class="hg-h"><ui-icon name="arrow-square-out" class="hg-cx" [style.--hi]="14" />Response</div></div>
                  <div class="hg-swap">
                    <div class="hg-mono hg-table"><app-hg-lines [lines]="lmkResponse" [step]="70" [off]="!resp" /></div>
                    <div class="hg-empty hg-empty--60" [class.hg-off]="resp">Response will appear here after executing</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          @if (t === 1) {
            <div class="hg-dialog">
              <div class="hg-card hg-dialog-card">
                <div class="hg-h hg-fs14"><ui-icon name="shield-check" class="hg-cb" [style.--hi]="16" />Authorize HSM Console</div>
                <div class="hg-dialog-p">Simulates custodian smart card insertion. In a real PayShield 10K, this requires physical presence of authorized officers with their cards.</div>
                <div class="hg-dialog-l">Number of Officers: 1</div>
                <div class="hg-slider" [style.--v]="0"><b></b><i></i></div>
                <div class="hg-slider-marks"><span>1 Officer</span><span>2 Officers</span><span>3 Officers</span></div>
                <div class="hg-dialog-l hg-mt14">Authorization Duration: 8 hours</div>
                <div class="hg-slider" [style.--v]="30"><b></b><i></i></div>
                <div class="hg-sunk hg-dialog-list"><b class="hg-cx">Authorizing:</b>@for (item of authorizing; track item) {<div class="hg-f hg-g6"><ui-icon name="check-circle" class="hg-cb" [style.--hi]="11" />{{ item }}</div>}</div>
                <div class="hg-dialog-foot"><span class="hg-cb hg-dialog-cancel">Cancel</span><span hgBtn class="hg-btn--32" icon="key">Authorize</span></div>
              </div>
            </div>
          }
        }

        @case ('logs') {
          <!-- Logs: the NC / VT session arriving line by line -->
          @let n = t === 8 ? 0 : min(t + 1, 7);
          <div class="hg-card hg-layer hg-logs" [style.--hd]="160">
            <div class="hg-logs-h">
              <div class="hg-h hg-cb"><ui-icon name="list-dashes" [style.--hi]="15" />Logs</div>
              <div class="hg-f hg-g6"><span hgBtn class="hg-btn--24" kind="ghost" icon="funnel">Filter (21)</span><span hgBtn class="hg-btn--24 hg-cn" kind="ghost" icon="chart-line">Stats</span><span hgBtn class="hg-btn--24" kind="ghost" icon="arrow-line-down">Auto</span><span class="hg-ico hg-ico--24"><ui-icon name="trash" class="hg-cb" /></span></div>
            </div>
            <div class="hg-swap hg-logs-b">
              <div class="hg-mono hg-mono--wrap hg-logs-lines">
                @for (line of logLines; track $index; let i = $index) {
                  <div class="hg-line hg-logline" [class.hg-off]="i >= n"><span class="hg-cn">[{{ line[0] }}]</span><span class="hg-b6" [class.hg-cp]="line[1] === 'HSM'" [class.hg-cb]="line[1] !== 'HSM'">{{ line[1] }}</span><span><span class="hg-cb">: </span>{{ line[2] }}</span></div>
                }
              </div>
              <div class="hg-mono hg-logs-wait" [class.hg-off]="n > 0">› waiting for traffic<span class="hg-caret" aria-hidden="true"></span></div>
            </div>
            <div class="hg-logs-f"><span>Entries: {{ n }}/7</span><span>Size: 1 KB</span><span>Auto-scroll: <b class="hg-cg">ON</b></span></div>
          </div>
        }

        @case ('profile') {
          <!-- Configuration → Profile: identity, the vendor tiles, model and firmware, then Save and Launch -->
          <div class="hg-grid hg-g12 hg-prof">
            <div class="hg-card hg-layer hg-w hg-prof-rail" [style.--hd]="160">
              <div class="hg-fb"><div class="hg-h"><ui-icon name="shield" class="hg-cb" [style.--hi]="14" />HSM Simulator</div><ui-icon name="wrench" class="hg-co" [style.--hi]="13" /></div>
              <div class="hg-prof-count"><span>PROFILES · 2</span><span class="hg-f hg-g8"><ui-icon name="plus" class="hg-cb" /><ui-icon name="upload-simple" class="hg-cb" /><ui-icon name="download-simple" class="hg-cb" /><ui-icon name="trash" class="hg-cr" /></span></div>
              <div class="hg-tile on hg-rise hg-prof-item hg-prof-item--on" [style.--hd]="300"><ui-icon name="terminal-window" [style.--hi]="13" /><span class="hg-prof-name">Thales PayShield 10k</span><ui-icon name="check-circle" [style.--hi]="13" /></div>
              <div class="hg-tile hg-rise hg-prof-item" [style.--hd]="360"><ui-icon name="laptop" [style.--hi]="13" /><span>HSM-2</span></div>
              <div class="hg-grow"></div>
              <span hgBtn class="hg-btn--block hg-btn--32" kind="ghost" icon="floppy-disk" [press]="t === 5">Save All Configurations</span>
              <span hgBtn class="hg-btn--block hg-btn--launch" kind="teal" icon="terminal-window" [press]="t === 6">Launch HSM Simulator</span>
            </div>
            <div class="hg-grid hg-g12 hg-mw0">
              <div class="hg-card hg-layer hg-pad" [style.--hd]="220">
                <div class="hg-card-h"><div class="hg-h"><ui-icon name="info" class="hg-cb" [style.--hi]="14" />Basic Information</div></div>
                <div class="hg-grid hg-g10">
                  <app-hg-fld label="HSM Name" icon="cpu" [value]="t < 8 ? 'Thales PayShield 10k' : ''" [on]="t === 0" />
                  <app-hg-fld label="Description" icon="file-text" ph="Description" />
                  <app-hg-fld label="Serial Number" icon="hash" [value]="t >= 1 && t < 8 ? 'HSM001234567890' : ''" [on]="t === 1" />
                </div>
              </div>
              <div class="hg-card hg-layer hg-pad" [style.--hd]="320">
                <div class="hg-card-h"><div class="hg-h"><ui-icon name="buildings" class="hg-cb" [style.--hi]="14" />Vendor &amp; Model Configuration</div></div>
                <div class="hg-lab hg-lab--lg">Select HSM Vendor</div>
                <div class="hg-grid hg-c4 hg-nc2 hg-g8">
                  @for (vendor of vendors; track vendor; let i = $index) {
                    <div hgTile icon="buildings" [label]="vendor" [on]="i === 0 && t >= 2 && t < 8" [hov]="i === 1 && t === 3"></div>
                  }
                </div>
                <div class="hg-grid hg-c2 hg-nc1 hg-g10 hg-mt14">
                  <app-hg-fld label="Thales Model" icon="laptop" [value]="t >= 3 && t < 8 ? 'payShield 10K' : ''" ph="Select model…" [caret]="true" [on]="t === 3" />
                  <app-hg-fld label="Firmware Version" icon="code" [value]="t >= 4 && t < 8 ? '1.0.14' : ''" [on]="t === 4" />
                </div>
              </div>
              <div class="hg-card hg-layer hg-pad hg-pad--foot" [style.--hd]="420">
                <div class="hg-card-h"><div class="hg-h"><ui-icon name="gear" class="hg-cb" [style.--hi]="14" />Operational Configuration</div></div>
              </div>
            </div>
          </div>
        }

        @case ('network') {
          <!-- Configuration → Network: connection type, addresses, the payShield framing toggles -->
          @let live = t < 7;
          <div class="hg-card hg-layer hg-pad" [style.--hd]="160">
            <div class="hg-card-h"><div class="hg-h"><ui-icon name="gear" class="hg-cb" [style.--hi]="14" />Connection Configuration</div></div>
            <div class="hg-lab hg-lab--lg">Select Connection Type</div>
            <div class="hg-grid hg-c2 hg-g8">
              @for (type of connections; track type; let i = $index) { <div hgTile [label]="type" [on]="i === 0 && live"></div> }
            </div>
            <div class="hg-grid hg-g10 hg-mt14 hg-c21 hg-nc1">
              <app-hg-fld label="IP Address" icon="laptop" [value]="t >= 1 && live ? '0.0.0.0' : ''" [on]="t === 1" />
              <app-hg-fld label="Port" icon="wifi-high" [value]="t >= 2 && live ? '9090' : ''" [on]="t === 2" />
            </div>
            <app-hg-fld class="hg-mt10" label="Bind Address" icon="broadcast" [value]="t >= 3 && live ? '0.0.0.0' : ''" [on]="t === 3" />
            <div class="hg-rule-h">PayShield Protocol Settings</div>
            <div hgTogRow icon="ruler" heading="TCP/IP Length Header" sub="2-byte big-endian message length prefix" [on]="t >= 4 && live" [d]="420"></div>
            <app-hg-fld label="Message Header Length" icon="text-t" [value]="t >= 5 && live ? '4' : ''" [on]="t === 5" />
          </div>
          <div class="hg-card hg-layer hg-pad" [style.--hd]="300">
            <div class="hg-card-h"><div class="hg-h"><ui-icon name="shield-check" class="hg-cb" [style.--hi]="14" />SSL/TLS Configuration</div></div>
            <div hgTogRow icon="shield-check" heading="Enable SSL/TLS" [d]="520"></div>
          </div>
        }

        @case ('lmk') {
          <!-- Key Management → LMK Details: slot 00 and its derived KBPK -->
          @let derived = t >= 7 && t < 9;
          <div class="hg-card hg-layer hg-pad hg-fb hg-g12" [style.--hd]="160">
            <span class="hg-f hg-g12"><ui-icon name="arrow-left" class="hg-cb" [style.--hi]="14" /><span class="hg-ico hg-ico--40 hg-ico--lmk"><ui-icon name="key" [style.--hi]="18" /></span><span><span class="hg-lmk-t">LMK Slot 00</span><span class="hg-tag hg-tag--loaded hg-cg">Loaded</span></span></span>
            <span class="hg-f hg-g8"><span class="hg-ico hg-ico--32 hg-ico--amber"><ui-icon name="arrows-clockwise" class="hg-co" [style.--hi]="14" /></span><span class="hg-ico hg-ico--32 hg-ico--blue"><ui-icon name="upload-simple" [style.--hi]="14" /></span></span>
          </div>
          <div class="hg-card hg-layer hg-pad" [style.--hd]="260">
            <div class="hg-card-h"><div class="hg-h">Slot Information</div></div>
            @for (row of lmkRows; track row[0]; let i = $index) {
              <div class="hg-kv" [class.hg-kv--chips]="!!row[2]"><span>{{ row[0] }}</span><span>
                @if (t >= i && t < 9) {
                  @if (row[2]; as chips) {
                    @for (chip of chips; track chip; let j = $index) {<span class="hg-chip hg-line" [class.on]="j === 0" [style.--ld]="j * 60">{{ chip }}</span>}
                  } @else {<span class="hg-line">{{ row[1] }}</span>}
                } @else {<span class="hg-dash">—</span>}
              </span></div>
            }
          </div>
          <div class="hg-card hg-layer hg-pad" [style.--hd]="360">
            <div class="hg-card-h"><div class="hg-h"><ui-icon name="lock" class="hg-cb" [style.--hi]="14" />KBPK (Key Block Protection Key)</div><span class="hg-tag hg-tag--kbpk" [class.on]="derived">{{ derived ? 'DERIVED' : 'PENDING' }}</span></div>
            @for (row of kbpkRows; track row[0]) {
              <div class="hg-kv"><span>{{ row[0] }}</span><span>@if (derived) {<span class="hg-line">{{ row[1] }}</span>} @else {<span class="hg-dash">—</span>}</span></div>
            }
            <div class="hg-fb hg-cg hg-kbpk-eg"><span>Example: KBPK derived from LMK pair #0 (128 bits)</span><ui-icon name="copy" class="hg-ct" /></div>
            <div class="hg-sunk hg-mono hg-kbpk-key" [class.hg-cf]="!derived">@if (derived) {<span class="hg-line">{{ kbpk }}</span>} @else {— derive to reveal —}</div>
          </div>
        }
      }
    </app-sim-glass>
  `,
})
export class HsmScreen {
  readonly id = input.required<HsmScreenId>();

  protected readonly spec = computed(() => HSM_SCREENS[this.id()]);
  protected readonly command = computed(() => HOST_CMDS[this.spec().cmd ?? 'NC']);

  /** The command list, rotated so the command on show is at the top. */
  protected readonly list = computed(() => {
    const at = Math.max(0, HOST_LIST.findIndex((row) => row[0] === this.spec().cmd));
    return [...HOST_LIST.slice(at), ...HOST_LIST.slice(0, at)].slice(0, 9);
  });

  protected readonly min = Math.min;
  protected readonly hReq = ['Message Header........... = [HDR1]', 'Command Code............. = [NO] HSM State Request', 'Mode Flag................ = [00] Return Status Information'];
  protected readonly hRsp = ['Message Header........... = [HDR1]', 'Command Code............. = [NP] HSM State Response', 'Error Code............... = [00] No error', 'I/O Buffer Size.......... = [3]', 'Ethernet Type............ = [1]', 'Number of TCP Sockets.... = [64]', 'Firmware number.......... = [1448-0001]', 'DSP fitted............... = [1]', 'DSP Firmware Version..... = [9100]'];
  protected readonly hDbg = ['', '<Debug Info>', 'Wire Response          : HDR1NP0031641448-000119100'];
  /** 48 slots; the narrow layout shows the first 24. */
  protected readonly slots = Array.from({ length: 48 }, (_, i) => i);
  protected readonly hostChips = ['All', 'Diagnostics', 'PIN Translation', 'PIN Verification', 'PIN Generation'];
  protected readonly secList = SEC_LIST.slice(2).concat(SEC_LIST.slice(0, 2)).slice(0, 9);
  protected readonly lmkResponse = ['0000VU00LMK Table:', ...LMK_TABLE];
  protected readonly authorizing = ['Component key console', 'Admin console', 'Audit console', 'Misc console', 'Clear pin console'];
  protected readonly logLines: readonly (readonly [string, string, string])[] = [
    ['22:01:12.348', 'HSM', '▸ [HOST-CMD] CMD NC  →  0000NC'],
    ['22:01:12.357', 'INFO', 'AuditEntry(timestamp=2026-08-30T22:01:12.35469, entryType=CONSOLE_COMMAND, command=NC, user=CONSOLE, lmkId=, result=SUCCESS, details=Diagnostic test passed)'],
    ['22:01:12.357', 'INFO', 'AuditEntry(timestamp=2026-08-30T22:01:12.357762, entryType=HOST_COMMAND, command=0000, user=CONSOLE, lmkId=, result=SUCCESS, details=NC005BCF24BE833E0010-E000)'],
    ['22:01:12.358', 'HSM', '◂ [HOST-CMD] RSP NC  ←  0000ND00NC005BCF24BE833E0010-E000'],
    ['22:01:23.379', 'HSM', '▸ [SECURE-CMD] CMD VT  →  0000VT'],
    ['22:01:23.381', 'INFO', 'AuditEntry(timestamp=2026-08-30T22:01:23.381631, entryType=HOST_COMMAND, command=0000, user=CONSOLE, lmkId=, result=SUCCESS, details=LMK Table:\n' + LMK_TABLE.join('\n') + '\n)'],
    ['22:01:23.381', 'HSM', '◂ [SECURE-CMD] RSP VT  ←  0000VU00LMK Table:\n' + LMK_TABLE.join('\n')],
  ];
  protected readonly vendors = ['Thales', 'SafeNet Luna', 'Utimaco CryptoServer', 'Futurex Excrypt', 'nCipher nShield', 'Generic/Custom HSM'];
  protected readonly connections = ['TCP_IP', 'SERIAL', 'REST_API', 'WEBSOCKET'];
  /** [label, value, chips?] */
  protected readonly lmkRows: readonly (readonly [string, string, (readonly string[])?])[] = [
    ['Slot ID', '00'], ['Status', 'Loaded'], ['Scheme', '', ['VARIANT', 'KEY_BLOCK']],
    ['Algorithm', '', ['3DES(2key)', '3DES(3key)', 'AES-128', 'AES-192', 'AES-256']],
    ['Pair Count', '40 pairs'], ['Check Value', '5BCF24BE833E'], ['Created At', 'Jan 16, 11:49'],
  ];
  protected readonly kbpkRows = [
    ['Key Block Version', 'Version 0 (TDES)'], ['LMK Algorithm', '3DES(2key)'],
    ['Derivation Method', 'TDES-ECB counter KDF with "KBPK" label'], ['Source', 'Derived from each LMK pair on demand'],
  ];
  protected readonly kbpk = hgHx('kbpk0', 32).replace(/(.{4})/g, '$1 ').trim();

  protected pad(i: number): string {
    return String(i).padStart(2, '0');
  }

  protected header(cmd: string, rsp: string): string {
    return `Header        : 0000\nResponse Code : ${rsp}    (command ${cmd} → ${rsp})\nError Code    : 00    ✓ Success — no error`;
  }
}
