import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { UiIcon } from '../../../ui';
import { SgBtn, SgFld, SgGlass, SgTab } from './sg-core';

/**
 * Glass recreations of the POS Simulator's screens, one per capture: the POS
 * Terminal window on its Device tab (power on → prerequisites tick → running →
 * APK installed), POS Terminal Configuration's Device tab (model picked →
 * variant verified → save, launch) and the six Hardware groups.
 *
 *   <pg-screen screen="hw-memory" />
 *
 * Styles: styles/bundles/_sims-pos.css (sg- primitives, pg- for these screens).
 */

const TERM_TABS: readonly SgTab[] = [
  ['device-mobile', 'Device'], ['credit-card', 'Card'], ['path', 'SDK Trace'], ['dots-nine', 'PIN Pad'],
  ['receipt', 'Receipts'], ['scan', 'Scanner'], ['list-bullets', 'Transactions'], ['flask', 'Scenarios'],
  ['list-dashes', 'Logs'],
];
const CFG_TABS: readonly SgTab[] = [
  [null, 'Device'], [null, 'Hardware'], [null, 'Peripherals'], [null, 'System & Boot'], [null, 'Card & Host'],
];
const TERM = 'POS Terminal - POS - 1';
const CFG = 'POS Terminal Configuration';

// ---------- POS Terminal › Device ----------

const PREREQ: readonly (readonly [string, string])[] = [
  ['Android SDK', '/Users/you/Library/Android/sdk'],
  ['Emulator binary', '/Users/you/Library/Android/sdk/emulator/emulator'],
  ['Command-line tools', '/opt/homebrew/bin/avdmanager'],
  ['adb', '/Users/you/Library/Android/sdk/platform-tools/adb'],
  ['System image', 'android-30 / google_apis / (host)'],
  ['ABI matches host', 'arm64-v8a — hardware accelerated'],
];
/** [label, value, mono?] */
const DEVICE: readonly (readonly [string, string, boolean?])[] = [
  ['Terminal', 'PAX A910S — 5" HD · Android 12'],
  ['Identity', 'pax-a910s:5in', true],
  ['Screen', '720 x 1280 @ 320 dpi', true],
  ['Density', '320 dpi', true],
  ['Memory', '2048 MB', true],
  ['AVD name', 'ISO8583_PAX_A910S', true],
  ['Payment SDK', 'PAX Neptune Lite'],
  ['SDK status', 'DAL in progress'],
  ['Peripherals', 'Beeper, Camera, Cellular, GPS, Contact card reader, Status LEDs, Magnetic stripe reader, NFC, PIN entry device, Contactless reader, Thermal printer, SAM slots, Barcode scanner, Wi-Fi'],
];
const IDENT: readonly (readonly [string, string])[] = [
  ['ro.product.manufacturer', 'PAX'], ['ro.product.brand', 'UNISOC'], ['ro.product.model', 'A910S'],
  ['ro.product.device', 'uis8581e_5h10'], ['ro.product.name', 'uis8581e_5h10_Natv'],
];

/** Power on → the six prerequisites tick green → booted, identity applied → Install APK… */
@Component({
  selector: 'pg-terminal',
  imports: [UiIcon, SgBtn],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'sg-c' },
  template: `
    <div class="sg-card sg-layer pg-bar" [style.--d]="160">
      <span class="pg-bar-l">
        <i class="sg-dot sg-led" [class.on]="running()"></i>
        <b class="pg-bar-name">{{ name }}</b>
        <span class="sg-m10 sg-wide">{{ image }}</span>
      </span>
      <span class="pg-bar-r">
        <span class="sg-m10">ISO8583_PAX_A910S</span>
        @for (s of [state()]; track s) {<b class="sg-line pg-state" [class.sg-cg]="running()">{{ s }}</b>}
      </span>
    </div>
    <div class="sg-card sg-layer sg-pad" [style.--d]="220">
      <div class="sg-acts">
        @if (running()) {
          <sg-btn kind="danger" icon="power" [press]="t() === 9">Power off</sg-btn>
        } @else {
          <sg-btn icon="play" [press]="t() === 0" [dim]="t() >= 1 && t() < 5">Power on</sg-btn>
        }
        <sg-btn kind="ghost" icon="wrench" [dim]="t() >= 1 && t() < 10">Prepare AVD</sg-btn>
        <sg-btn kind="ghost" icon="package" [press]="t() === 6" [dim]="!running()">Install APK…</sg-btn>
        @if (installed()) {<span class="sg-tag sg-tag--pill sg-tag--g sg-line">com.example.payapp installed · 12.4 MB</span>}
      </div>
      <div class="sg-sub pg-hint">Power on will create the AVD if needed, boot it, and apply the device identity.</div>
    </div>
    <div class="sg-card sg-layer pg-prereq" [style.--d]="300">
      <div class="sg-cap">Prerequisites</div>
      <div class="pg-checks">
        @for (row of prereq; track row[0]; let i = $index) {
          <div class="pg-check sg-tile" [style.--d]="360 + i * 40">
            @if (i < ticks()) {
              <ui-icon name="check-circle" class="pg-check-i on" [style.--s]="13" />
            } @else {
              <ui-icon name="circle" class="pg-check-i" [style.--s]="13" />
            }
            <span class="pg-check-t"><span class="pg-check-n">{{ row[0] }}</span><span class="pg-check-p">{{ row[1] }}</span></span>
          </div>
        }
      </div>
    </div>
    <div class="pg-cols">
      <div class="sg-card sg-layer sg-pad" [style.--d]="420">
        <div class="sg-cap">Device</div>
        @for (row of device; track row[0]; let i = $index) {
          <div class="sg-kv sg-tile" [style.--d]="480 + i * 30"><span class="sg-kv-k">{{ row[0] }}</span><span class="sg-kv-v" [class.sg-kv-v--mono]="row[2]">{{ row[1] }}</span></div>
        }
      </div>
      <div class="sg-card sg-layer sg-pad" [style.--d]="520">
        <div class="sg-cap">Spoofed identity</div>
        @for (row of ident; track row[0]; let i = $index) {
          <div class="sg-kv sg-tile" [style.--d]="580 + i * 40"><span class="sg-kv-k">{{ row[0] }}</span><span class="sg-kv-v sg-kv-v--mono">{{ row[1] }}</span></div>
        }
        <div class="sg-sub pg-applied">@if (running()) {<ui-icon name="check" class="sg-cg" [style.--s]="11" /><span class="sg-line">applied after boot via adb</span>}</div>
      </div>
    </div>
  `,
})
export class PgTerminal {
  readonly t = input.required<number>();

  protected readonly name = 'PAX A910S — 5" HD · Android 12';
  protected readonly image = 'android-30 / google_apis / (host)  host arm64-v8a';
  protected readonly prereq = PREREQ;
  protected readonly device = DEVICE;
  protected readonly ident = IDENT;

  protected readonly ticks = computed(() => {
    const t = this.t();
    return t < 0 || t >= 10 ? 6 : t === 0 ? 0 : Math.min(6, t * 2);
  });
  protected readonly running = computed(() => this.t() >= 5 && this.t() < 10);
  protected readonly installed = computed(() => this.t() >= 7 && this.t() < 10);
  protected readonly state = computed(() => {
    const t = this.t();
    return this.running() ? 'Running' : t >= 3 && t < 5 ? 'Booting…' : t >= 1 && t < 3 ? 'Preparing AVD…' : 'Not running';
  });
}

// ---------- POS Terminal Configuration › Device ----------

/** [name, line, only the first has a payment-SDK integration] */
const MODELS: readonly (readonly [string, string, boolean?])[] = [
  ['PAX A910S', '4 variants · 720 x 1280 @ 320 dpi', true], ['PAX A920', '720 x 1280 @ 320 dpi'],
  ['PAX A920Pro', '720 x 1280 @ 320 dpi'], ['PAX A80', '480 x 640 @ 240 dpi'], ['PAX IM30', '720 x 1280 @ 320 dpi'],
  ['Ingenico AXIUM DX8000', '3 variants · 720 x 1440 @ 320 dpi'], ['Ingenico AXIUM DX4000', '720 x 1280 @ 320 dpi'],
  ['Ingenico AXIUM EX8000', '720 x 1440 @ 320 dpi'], ['Kozen N2', '2 variants · 480 x 480 @ 160 dpi'],
  ['Kozen P10', '720 x 1280 @ 320 dpi'], ['Kozen P12', '720 x 1440 @ 320 dpi'], ['Newland N910', '720 x 1280 @ 320 dpi'],
  ['Newland NQuire 750', '800 x 1280 @ 240 dpi'], ['Sunmi P2', '720 x 1280 @ 320 dpi'], ['Sunmi P2 Pro', '720 x 1440 @ 320 dpi'],
  ['Sunmi V2', '720 x 1280 @ 320 dpi'], ['Verifone T650c', '720 x 1280 @ 320 dpi'], ['Verifone X990', '720 x 1440 @ 320 dpi'],
  ['NexGo N86', '720 x 1280 @ 320 dpi'], ['Castles Technology S1F2', '720 x 1280 @ 320 dpi'], ['Telpo M1', '720 x 1280 @ 320 dpi'],
  ['Generic Android terminal (720x1440)', '720 x 1440 @ 320 dpi'], ['Generic square terminal (480x480)', '480 x 480 @ 160 dpi'],
];
/** [name, line, badge, verified] */
const VARIANTS: readonly (readonly [string, string, string, boolean])[] = [
  ['5" HD · Android 12', '720 x 1280 @ 320 dpi', '✓ Verified from device', true],
  ['5.5" HD+', '720 x 1440 @ 320 dpi', '⚠ Unverified', false],
  ['Wi-Fi only', '720 x 1280 @ 320 dpi', '⚠ Unverified', false],
  ['Android 10 units', '720 x 1280 @ 320 dpi', '⚠ Unverified', false],
];

/** The profile rail, the vendor filter, the model grid and the variant chooser. */
@Component({
  selector: 'pg-config-device',
  imports: [UiIcon, SgBtn, SgFld],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'sg-c' },
  template: `
    <div class="sg-split">
      <div class="sg-card sg-layer sg-rail sg-wide" [style.--d]="160">
        <div class="sg-rail-top"><div class="sg-h"><ui-icon name="device-mobile" class="sg-cb" [style.--s]="14" />POS Simulator</div><ui-icon name="flask" class="sg-cp" [style.--s]="13" /></div>
        <div class="sg-rail-bar"><span>PROFILES · 1</span><span class="sg-rail-tools"><ui-icon name="plus" class="sg-cb" /><ui-icon name="upload-simple" class="sg-cb" /><ui-icon name="download-simple" class="sg-cb" /><ui-icon name="trash" class="sg-cr" /></span></div>
        <div class="sg-rail-item sg-tile" [style.--d]="300"><ui-icon name="device-mobile" [style.--s]="13" /><span>POS - 1</span><ui-icon name="check-circle" [style.--s]="13" /></div>
        <div class="sg-grow"></div>
        <sg-btn class="sg-btn--block" kind="ghost" icon="floppy-disk" [press]="t() === 4">Save All Configurations</sg-btn>
        <sg-btn class="sg-btn--launch" kind="teal" icon="device-mobile" [press]="t() === 5">Launch POS Simulator</sg-btn>
      </div>
      <div class="sg-stack">
        <div class="sg-card sg-layer sg-pad14" [style.--d]="200">
          <div class="sg-card-h"><div class="sg-h"><ui-icon name="storefront" class="sg-cb" [style.--s]="14" />Vendor</div></div>
          <div class="pg-vendor">
            <sg-fld label="Manufacturer" value="All vendors" [caret]="true" />
            <sg-fld label="Search" icon="magnifying-glass" ph="Search model, variant or SKU" />
          </div>
        </div>
        <div class="sg-card sg-layer sg-pad14" [style.--d]="300">
          <div class="sg-card-h"><div class="sg-h"><ui-icon name="device-mobile" class="sg-cb" [style.--s]="14" />Model</div><span class="sg-m10">{{ models.length }} models · 9 vendors</span></div>
          <div class="pg-models">
            @for (m of models; track m[0]; let i = $index) {
              <div class="sg-opt pg-pick sg-tile" [class.on]="i === 0 && s(1)" [class.hov]="i === 1 && t() === 0" [style.--d]="360 + i * 22">
                <span class="pg-pick-t">
                  <span class="pg-pick-n">{{ m[0] }}</span>
                  <span class="pg-pick-s">{{ m[1] }}</span>
                  @if (!m[2]) {<span class="sg-tag pg-pick-tag">Hardware only</span>}
                </span>
                <i class="sg-rd" [class.on]="i === 0 && s(1)"></i>
              </div>
            }
          </div>
        </div>
        <div class="sg-card sg-layer sg-pad14" [style.--d]="440">
          <div class="sg-card-h"><div class="sg-h"><ui-icon name="stack" class="sg-cb" [style.--s]="14" />Variant</div>@if (s(2)) {<span class="sg-tag sg-tag--pill sg-tag--b sg-line">PAX A910S · 4 variants</span>}</div>
          <div class="sg-sub pg-variant-sub">Variants of the same model differ in screen, memory, peripherals or Android version — pick the one matching the unit you are targeting.</div>
          @if (s(2)) {
            <div class="pg-variants">
              @for (v of variants; track v[0]; let i = $index) {
                <div class="sg-opt pg-pick sg-tile sg-fresh" [class.on]="i === 0 && s(3)" [style.--d]="i * 60">
                  <span class="pg-pick-t">
                    <span class="pg-pick-n">{{ v[0] }}</span>
                    <span class="pg-pick-s">{{ v[1] }}</span>
                    <span class="sg-tag pg-pick-tag" [class.ok]="v[3]" [class.warn]="!v[3]">{{ v[2] }}</span>
                  </span>
                  <i class="sg-rd" [class.on]="i === 0 && s(3)"></i>
                </div>
              }
            </div>
          } @else {
            <div class="pg-variant-empty">Pick a model to list its variants</div>
          }
        </div>
      </div>
    </div>
  `,
})
export class PgConfigDevice {
  readonly t = input.required<number>();

  protected readonly models = MODELS;
  protected readonly variants = VARIANTS;

  /** True from step k until the loop resets. */
  protected s(k: number): boolean {
    return this.t() >= k && this.t() < 8;
  }
}

// ---------- POS Terminal Configuration › Hardware: the six groups ----------

export type PgHwGroup = 'hw-memory' | 'hw-display' | 'hw-graphics' | 'hw-input' | 'hw-cameras' | 'hw-sensors';

interface HwSpec {
  icon: string;
  title: string;
  steps: number;
  period: number;
  settled: number;
  cols?: 3;
  /** [label, value, select?] — one fills per step. */
  fields?: readonly (readonly [string, string, boolean?])[];
  /** [label, hw.* property key, ends up on?] */
  props?: readonly (readonly [string, string, boolean])[];
}

const HW: Record<PgHwGroup, HwSpec> = {
  'hw-memory': {
    icon: 'memory', title: 'Memory & Storage', steps: 7, period: 900, settled: 5,
    fields: [['RAM', '2048'], ['VM heap', '0'], ['Internal storage', '6G'], ['SD card size', '512M']],
    props: [['SD card present', 'hw.sdCard', true]],
  },
  'hw-display': {
    icon: 'monitor', title: 'Display', steps: 8, period: 850, settled: 6, cols: 3,
    fields: [['Screen width (px)', '720'], ['Screen height (px)', '1280'], ['Density (dpi)', '320', true], ['Colour depth', '16', true], ['Initial orientation', 'portrait', true], ['Touch screen type', 'multi-touch', true]],
  },
  'hw-graphics': {
    icon: 'cpu', title: 'Graphics & CPU', steps: 5, period: 1000, settled: 3,
    props: [['Hardware GPU', 'hw.gpu.enabled', true]],
  },
  'hw-input': {
    icon: 'hand-tap', title: 'Input', steps: 6, period: 1000, settled: 4,
    props: [['Hardware keyboard', 'hw.keyboard', true], ['Hardware back/home keys', 'hw.mainKeys', false], ['D-pad', 'hw.dPad', false], ['Trackball', 'hw.trackBall', false]],
  },
  'hw-cameras': {
    icon: 'camera', title: 'Cameras', steps: 4, period: 1100, settled: 2,
    fields: [['Rear camera', 'emulated', true], ['Front camera', 'emulated', true]],
  },
  'hw-sensors': {
    icon: 'broadcast', title: 'Sensors', steps: 10, period: 700, settled: 8,
    props: [['Accelerometer', 'hw.accelerometer', true], ['Gyroscope', 'hw.gyroscope', true], ['GPS', 'hw.gps', false], ['Battery', 'hw.battery', true], ['Microphone', 'hw.audioInput', true], ['Proximity', 'hw.sensors.proximity', true], ['Light', 'hw.sensors.light', true]],
  },
};

/** One Hardware group: the app's heading row, then the bordered card — fields filling and toggles flipping. */
@Component({
  selector: 'pg-hw',
  imports: [UiIcon, SgFld],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'sg-c' },
  template: `
    @let g = spec();
    <div class="sg-h pg-hw-h sg-layer" [style.--d]="160"><ui-icon [name]="g.icon" class="sg-cb" [style.--s]="16" />{{ g.title }}</div>
    <div class="sg-card sg-layer sg-pad pg-hw-card" [style.--d]="220">
      @if (group() === 'hw-graphics') {
        <div class="pg-prop sg-tile" [style.--d]="260"><span><span class="pg-prop-t">Hardware GPU</span><span class="sg-m10">hw.gpu.enabled</span></span><i class="sg-tog" [class.on]="s(0)"></i></div>
        <div class="pg-fields pg-gpu" [class.dim]="!s(0)">
          <sg-fld label="GPU mode" [value]="s(1) ? 'auto' : ''" ph="Select…" [caret]="true" [on]="t() === 1" />
          <sg-fld label="CPU cores" [value]="s(2) ? '8' : ''" [on]="t() === 2" />
        </div>
      } @else {
        @if (g.fields; as fields) {
          <div class="pg-fields" [class.pg-fields--3]="g.cols === 3">
            @for (f of fields; track f[0]; let i = $index) {
              <sg-fld [label]="f[0]" [value]="s(i) ? f[1] : ''" [ph]="f[2] ? 'Select…' : ''" [caret]="!!f[2]" [on]="t() === i" />
            }
          </div>
        }
        @if (g.props; as props) {
          <div [class.pg-props]="!!g.fields">
            @for (p of props; track p[1]; let i = $index; let last = $last) {
              <div class="pg-prop sg-tile" [class.last]="last" [style.--d]="propDelay(i)"><span><span class="pg-prop-t">{{ p[0] }}</span><span class="sg-m10">{{ p[1] }}</span></span><i class="sg-tog" [class.on]="propOn(i, p[2])"></i></div>
            }
          </div>
        }
      }
    </div>
  `,
})
export class PgHw {
  readonly group = input.required<PgHwGroup>();
  readonly t = input.required<number>();

  protected readonly spec = computed(() => HW[this.group()]);

  /** Filled from step k until the loop resets. */
  protected s(k: number): boolean {
    return this.t() >= k && this.t() < this.spec().steps - 1;
  }

  protected propDelay(i: number): number {
    const group = this.group();
    return group === 'hw-memory' ? 300 : group === 'hw-sensors' ? 260 + i * 40 : 260 + i * 50;
  }

  protected propOn(i: number, endsOn: boolean): boolean {
    const t = this.t();
    switch (this.group()) {
      case 'hw-memory': return this.s(4);
      // the keyboard comes on and stays; the other three are flicked one after another
      case 'hw-input': return endsOn ? this.s(0) : t === i + 1;
      // sensors flip on one after another; GPS is flicked on at step 3 and back off at step 6
      case 'hw-sensors': return endsOn ? t >= i && t < this.spec().steps - 1 : t >= 3 && t < 6;
      default: return this.s(0);
    }
  }
}

// ---------- the screen registry ----------

interface ScreenSpec {
  title: string;
  sub: string;
  tabs: readonly SgTab[];
  tab: number;
  badge?: string;
  aria: string;
  steps: number;
  period: number;
  settled: number;
}

const hw = (group: PgHwGroup, aria: string): ScreenSpec => ({
  title: CFG, sub: 'Hardware · ' + HW[group].title, tabs: CFG_TABS, tab: 1, aria,
  steps: HW[group].steps, period: HW[group].period, settled: HW[group].settled,
});

export const POS_SCREENS: Record<string, ScreenSpec> = {
  overview: {
    title: TERM, sub: 'Device', tabs: TERM_TABS, tab: 0, badge: 'TERMINAL', steps: 11, period: 1000, settled: 6,
    aria: 'The POS Terminal window on its Device tab: a PAX A910S profile with Power on, Prepare AVD and Install APK actions, the six prerequisites ticking green, the resolved device table and the spoofed ro.product identity',
  },
  device: {
    title: CFG, sub: 'Device', tabs: CFG_TABS, tab: 0, steps: 9, period: 1100, settled: 4,
    aria: 'POS Terminal Configuration on the Device tab: the profile rail, the vendor filter with its search, a grid of terminal models from PAX, Ingenico, Kozen, Newland, Sunmi, Verifone, NexGo, Castles and Telpo with PAX A910S selected, and its four variants with Verified and Unverified badges',
  },
  'hw-memory': hw('hw-memory', 'Memory and storage settings: RAM 2048, VM heap 0, internal storage 6G, SD card size 512M and SD card present enabled'),
  'hw-display': hw('hw-display', 'Display settings: screen width 720, height 1280, density 320 dpi, 16-bit colour depth, portrait orientation and a multi-touch screen'),
  'hw-graphics': hw('hw-graphics', 'Graphics and CPU settings: hardware GPU enabled, GPU mode auto and 8 CPU cores'),
  'hw-input': hw('hw-input', 'Input settings: hardware keyboard on, hardware back and home keys, D-pad and trackball off'),
  'hw-cameras': hw('hw-cameras', 'Camera settings with the rear and front cameras both set to emulated'),
  'hw-sensors': hw('hw-sensors', 'Sensor toggles for accelerometer, gyroscope, GPS, battery, microphone, proximity and light, each labelled with its hw property key'),
};

/**
 * PgScreen — one POS Simulator screen in its window; `screen` is a key of POS_SCREENS.
 * `active` is false while the screen is a hidden tab panel.
 */
@Component({
  selector: 'pg-screen',
  imports: [SgGlass, PgTerminal, PgConfigDevice, PgHw],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'sg-host' },
  template: `
    @let s = spec();
    <sg-glass #g [heading]="s.title" [sub]="s.sub" [tabs]="s.tabs" [tab]="s.tab" app="pos-simulator" [badge]="s.badge ?? 'POS'"
              [aria]="s.aria" [steps]="s.steps" [period]="s.period" [settled]="s.settled" [active]="active()">
      @switch (screen()) {
        @case ('overview') { <pg-terminal [t]="g.t()" /> }
        @case ('device') { <pg-config-device [t]="g.t()" /> }
        @default { <pg-hw [group]="hwGroup()" [t]="g.t()" /> }
      }
    </sg-glass>
  `,
})
export class PgScreen {
  readonly screen = input.required<string>();
  readonly active = input(true);

  protected readonly spec = computed(() => POS_SCREENS[this.screen()]);
  protected readonly hwGroup = computed(() => this.screen() as PgHwGroup);
}
