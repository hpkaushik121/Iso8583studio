import { AnalyticsService } from '../../core/analytics';
import type { PageBehaviour } from './types';

/**
 * The home page's two autoplaying demos, ported from the inline <script> that
 * used to live in docs/index.html.
 *
 * Two changes from the original. Every interval and observer is returned in a
 * teardown so navigating away stops them — the old page leaked them, which did
 * not matter for a document that was about to be discarded but compounds on
 * every visit in a routed app. And the simulator tiles no longer start in a
 * `loading` state: that shimmer masked a load delay which prerendering removed,
 * and leaving it in meant a reader without JavaScript saw blanked-out tiles.
 */

interface Packet {
  proto: string;
  cls: 'emv' | 'iso' | 'hsm' | 'scheme';
  name: string;
  chips: ([string, string] | 'sep')[];
  status: string;
}

/** Keyed by tile index, as in the original. */
const PACKETS: Record<number, Packet> = {
  0: { proto: 'EMV · APDU', cls: 'emv', name: 'POS Simulator',
       chips: [['80AE8000', 'GEN AC'], ['9F02', '288.72'], 'sep', ['9F27', 'ARQC'], ['SW', '9000']], status: '9000' },
  1: { proto: 'APDU · TLV', cls: 'emv', name: 'APDU Simulator',
       chips: [['00B2010C', 'READ REC'], 'sep', ['70', 'TLV'], ['5A', 'PAN'], ['SW', '9000']], status: 'SW 9000' },
  2: { proto: 'ECR', cls: 'iso', name: 'ECR Simulator',
       chips: [['SALE', '288.72'], 'sep', ['RSP', 'APPROVED'], ['ref', '000028872']], status: 'APPROVED' },
  3: { proto: 'ISO 8583 · route', cls: 'iso', name: 'Switch Simulator',
       chips: [['0200', 'IN'], ['F32', 'ACQ 400001'], 'sep', ['route', '→ HOST-A'], ['F33', 'FWD']], status: 'ROUTED' },
  4: { proto: 'ISO 8583', cls: 'iso', name: 'Host Simulator',
       chips: [['0200', 'AUTH'], ['F2', '4541 22•• 9640'], ['F4', '$288.72'], 'sep', ['0210', ''], ['F39', '00']], status: 'F39 00' },
  5: { proto: 'Scheme auth', cls: 'scheme', name: 'Scheme Simulator',
       chips: [['0100', 'AUTH'], 'sep', ['0110', ''], ['F39', '00'], ['auth', '831000']], status: '0110 00' },
  6: { proto: 'payShield 10K', cls: 'hsm', name: 'HSM Simulator',
       chips: [['M4', 'GEN MAC'], 'sep', ['M5', '00'], ['MAC', '3C1F9A22']], status: 'M5 00' },
  7: { proto: 'HSM console', cls: 'hsm', name: 'HSM Command Console',
       chips: [['$ A0', 'GEN KEY'], 'sep', ['A1', '00'], ['KCV', 'A1B2C3']], status: 'A1 00' },
  8: { proto: 'NDC / DDC', cls: 'scheme', name: 'ATM Simulator',
       chips: [['NDC', 'TXN REQ'], 'sep', ['REPLY', 'DISPENSE'], ['notes', '03 03']], status: 'DISPENSE' },
};

/** Host, Switch, HSM, Console, POS, APDU, Scheme, ATM, ECR. */
const ORDER = [4, 3, 6, 7, 0, 1, 5, 8, 2];
const RAIL_INTERVAL = 2400;
const BOARD_INTERVAL = 1900;
const METER_INTERVAL = 700;
const METER_BARS = 9;

const barHeight = () => `${18 + Math.random() * 74}%`;

export const homeDemos: PageBehaviour = (root, analytics) => {
  const cleanups: (() => void)[] = [];
  const reduced = matchMedia('(prefers-reduced-motion: reduce)').matches;

  const every = (fn: () => void, ms: number) => {
    const id = setInterval(fn, ms);
    cleanups.push(() => clearInterval(id));
  };
  const on = <K extends keyof HTMLElementEventMap>(
    el: Element, type: K, fn: (e: HTMLElementEventMap[K]) => void, opts?: AddEventListenerOptions,
  ) => {
    el.addEventListener(type, fn as EventListener, opts);
    cleanups.push(() => el.removeEventListener(type, fn as EventListener, opts));
  };
  const onceVisible = (el: Element, fn: () => void) => {
    const io = new IntersectionObserver((entries) => {
      if (entries.some((e) => e.isIntersecting)) { fn(); io.disconnect(); }
    }, { threshold: .2 });
    io.observe(el);
    cleanups.push(() => io.disconnect());
  };

  // ---- transaction path: sequential highlight, pauses on hover -------------
  const rail = root.querySelector<HTMLElement>('#flowRail');
  if (rail) {
    const nodes = [...rail.querySelectorAll<HTMLElement>('.node')];
    let paused = false;
    let i = 0;

    on(rail, 'mouseenter', () => { paused = true; analytics.reportRailEngage(); });
    on(rail, 'mouseleave', () => { paused = false; });
    on(rail, 'touchstart', () => { paused = true; analytics.reportRailEngage(); }, { passive: true });
    on(rail, 'touchend', () => {
      const id = setTimeout(() => { paused = false; }, 2500);
      cleanups.push(() => clearTimeout(id));
    }, { passive: true });

    const activate = (k: number) => {
      nodes.forEach((n, j) => n.classList.toggle('on', j === k));
      const node = nodes[k];
      if (!node) return;
      const left = node.offsetLeft;
      const right = left + node.offsetWidth;
      const viewLeft = rail.scrollLeft;
      const viewRight = viewLeft + rail.clientWidth;
      if (left < viewLeft + 40 || right > viewRight - 40) {
        rail.scrollTo({
          left: Math.max(0, left - (rail.clientWidth - node.offsetWidth) / 2),
          behavior: 'smooth',
        });
      }
    };

    onceVisible(rail, () => {
      activate(0);
      i = 1;
      if (reduced) return;
      every(() => {
        if (paused) return;
        activate(i);
        i = (i + 1) % nodes.length;
      }, RAIL_INTERVAL);
    });
  }

  // ---- live simulator board -----------------------------------------------
  const grid = root.querySelector<HTMLElement>('#simGrid');
  if (grid) {
    const tiles = [...grid.querySelectorAll<HTMLElement>('.simtile')];
    const meters = [...grid.querySelectorAll<HTMLElement>('.st-meter')];
    const proto = root.querySelector<HTMLElement>('#simProto');
    const nameEl = root.querySelector<HTMLElement>('#simActive');
    const dataEl = root.querySelector<HTMLElement>('#simData');
    const pill = root.querySelector<HTMLElement>('#simPill');

    for (const meter of meters) {
      meter.replaceChildren();
      for (let b = 0; b < METER_BARS; b++) {
        const bar = document.createElement('i');
        bar.style.height = barHeight();
        meter.appendChild(bar);
      }
    }

    for (const tile of tiles) {
      const report = () => analytics.reportBoardEngage(
        tile.querySelector('.st-name')?.textContent?.trim() ?? '',
      );
      on(tile, 'mouseenter', report);
      on(tile, 'touchstart', report, { passive: true });
    }

    const chipTimers: ReturnType<typeof setTimeout>[] = [];
    cleanups.push(() => chipTimers.forEach(clearTimeout));

    const focus = (tileIndex: number) => {
      const packet = PACKETS[tileIndex];
      if (!packet || !proto || !nameEl || !dataEl || !pill) return;

      tiles.forEach((t, j) => t.classList.toggle('active', j === tileIndex));
      proto.className = `proto ${packet.cls}`;
      proto.textContent = packet.proto;
      nameEl.textContent = packet.name;
      pill.className = 'rcpill ok';
      pill.textContent = packet.status;

      dataEl.replaceChildren();
      packet.chips.forEach((chip, k) => {
        const span = document.createElement('span');
        if (chip === 'sep') {
          span.className = 'dchip sep';
          span.textContent = '⇒';
        } else {
          span.className = 'dchip';
          const label = document.createElement('span');
          label.className = 'de';
          label.textContent = chip[0];
          span.append(label, chip[1]);
        }
        dataEl.appendChild(span);
        chipTimers.push(setTimeout(() => span.classList.add('show'), 40 + k * 70));
      });
    };

    let idx = 0;
    focus(ORDER[0]);
    idx = 1;
    if (!reduced) {
      every(() => {
        for (const meter of meters) {
          for (const bar of [...meter.children] as HTMLElement[]) bar.style.height = barHeight();
        }
      }, METER_INTERVAL);
      every(() => {
        focus(ORDER[idx]);
        idx = (idx + 1) % ORDER.length;
      }, BOARD_INTERVAL);
    }
  }

  return () => cleanups.forEach((fn) => fn());
};
