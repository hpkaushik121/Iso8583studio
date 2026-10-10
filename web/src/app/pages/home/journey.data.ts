/**
 * The eight stages of the payment journey, in the order the three.js engine
 * (journey/story-engine.ts, STAGES) draws them: a row here is matched to a
 * scene there by index.
 */

export type StageAlign = 'left' | 'right' | 'center';
export type LabelPlace = 'left' | 'right' | 'verified' | 'from' | 'to';

export interface StageLabel {
  icon: string;
  text: string;
  place: LabelPlace;
  note?: string;
}

export interface JourneyStage {
  n: string;
  scene: string;
  align: StageAlign;
  tall?: boolean;
  /** Line above the stage number; only the first stage carries one. */
  lead?: string;
  label: string;
  title: [string, string?];
  desc?: string;
  /** Internal route the stage's "Learn more" goes to. */
  link?: string;
  /** Text alternative for the 3D scene. */
  alt: string;
  /** The three lines of the flat plate shown until (or instead of) the scene. */
  fallback: [string, string, string];
  handshake?: boolean;
  messages?: { glyph: string; name: string; note?: string }[];
  panel?: { title: string; rows: [string, string][] };
  labels?: StageLabel[];
  note?: string;
  response?: { mti: string; label: string; code: string; note: string };
  end?: { line: string; cta: string; link: string };
}

export const JOURNEY_STAGES: JourneyStage[] = [
  {
    n: '01', scene: 'scene-card', align: 'left', lead: 'The complete payment journey',
    label: 'APDU simulator', title: ['Simulate any card.', 'Run any L3 test case.'],
    desc: 'Drive the APDU dialogue, command by command.',
    link: '/simulator/apdu',
    alt: 'A test card turning in space, its metal EMV contacts catching the light',
    fallback: ['ISO8583STUDIO', '•••• 4242', 'DEBIT · 09/29'],
  },
  {
    n: '02', scene: 'scene-insert', align: 'right',
    label: 'POS simulator', title: ['Emulate the real', 'terminal on your desk.'],
    desc: 'Emulate real hardware and install your APK on it.',
    link: '/simulator/pos', handshake: true,
    alt: 'A payment terminal with a test card seated in its chip slot',
    fallback: ['POS TERMINAL', 'CARD INSERTED', 'READING CHIP…'],
  },
  {
    n: '03', scene: 'scene-process', align: 'center', tall: true,
    label: 'Terminal processing', title: ['A payment becomes', 'a message.'],
    messages: [
      { glyph: '◉', name: '0200', note: 'Financial request' },
      { glyph: '▦', name: 'BITMAP' },
      { glyph: '☷', name: 'FIELDS' },
    ],
    alt: 'A terminal assembling an ISO 8583 request, its message parts listed beside it',
    fallback: ['TERMINAL', 'PROCESSING', 'BUILDING 0200'],
  },
  {
    n: '04', scene: 'scene-host', align: 'left',
    label: 'Host simulator', title: ['The 0200 lands on', 'a host you control.'],
    desc: 'Answer any MTI on a host you run yourself.',
    link: '/simulator/host',
    panel: {
      title: '0200 / REQUEST',
      rows: [['MTI', '0200'], ['Bitmap', 'F23A8C0000800000'], ['Fields', '2, 3, 4, 7, 11…']],
    },
    alt: 'A backend host appliance with its status lights lit',
    fallback: ['HOST SIMULATOR', '0200 → 0210', 'PORT 8583'],
  },
  {
    n: '05', scene: 'scene-hsm', align: 'center', tall: true,
    label: 'HSM simulator', title: ['Inside the', 'secure boundary.'],
    desc: 'Decrypt, translate and verify without the appliance.',
    link: '/simulator/hsm',
    labels: [
      { icon: 'lock', text: 'Encrypted test block', place: 'left' },
      { icon: 'gear', text: 'Simulated operation', place: 'right' },
      { icon: 'check', text: 'Verified', place: 'verified' },
    ],
    note: 'Conceptual cutaway',
    alt: 'A conceptual HSM cutaway opening to reveal the secure chip inside',
    fallback: ['HSM SIMULATOR', 'PIN · MAC · KEYS', 'SECURE BOUNDARY'],
  },
  {
    n: '06', scene: 'scene-network', align: 'right',
    label: 'Hosted scheme endpoints', title: ['Keep the request', 'moving.'],
    desc: 'Route, delay or hand the request to the issuer.',
    // There is no /simulator/scheme page; the hosted scheme endpoints live here.
    link: '/cloud-simulators',
    labels: [
      { icon: 'lock', text: 'From host', place: 'from', note: '0200 Request' },
      { icon: 'lock', text: 'To issuer', place: 'to', note: 'Across the network' },
    ],
    alt: 'A faceted network globe turning, its nodes lit',
    fallback: ['SCHEME SIMULATOR', 'HOST → ISSUER', 'ROUTING'],
  },
  {
    n: '07', scene: 'scene-issuer', align: 'left',
    label: 'Issuer System', title: ['One decision.', 'A response begins.'],
    desc: 'Approve, decline or go silent, then send the 0210 back.',
    link: '/simulator/issuer',
    response: { mti: '0210', label: 'RESPONSE', code: '00', note: 'Approved by issuer' },
    alt: 'An issuer appliance approving the test request',
    fallback: ['ISSUER SYSTEM', '0210 · 00', 'APPROVED'],
  },
  {
    n: '08', scene: 'scene-approved', align: 'center', tall: true,
    label: 'The return', title: ['Back where it began.'],
    end: {
      line: 'One connected payment-testing workspace.',
      cta: 'Explore the simulators',
      link: '/simulator',
    },
    alt: 'The terminal displaying the approved response, the card still seated',
    fallback: ['APPROVED', '0210 · 00', 'BACK AT THE TERMINAL'],
  },
];
