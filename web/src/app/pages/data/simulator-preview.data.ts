/**
 * The four simulators that are still in development — Switch, Issuer System,
 * ATM and ECR — as the concept guides present them
 * (pages/shared/preview/simulator-preview.ts).
 *
 * Every payload, code and outcome in `steps` and `scenarios` is a fixed
 * sample written for the illustration. None of it is a real message, and the
 * guides say so on the page.
 *
 * Icon names are Phosphor slugs; they are quoted literals here so the sprite
 * builder picks them up.
 */

export type PreviewId = 'switch' | 'issuer' | 'atm' | 'ecr';

/** One of the three devices drawn across the workflow panel. */
export interface PreviewNode {
  icon: string;
  label: string;
  detail: string;
}

/** One of the four steps of the default walk-through. */
export interface PreviewStep {
  title: string;
  body: string;
  /** Short verb shown above the payloads ("Route"). */
  tag: string;
  /** Left pane: the sample input. Lines are separated by \n. */
  request: string;
  /** Right pane: the sample state that follows. */
  response: string;
  /** Index of the node that is lit during this step. */
  activeNode: number;
}

/** What a scenario changes about one step; anything omitted falls back to the default step. */
export interface PreviewScenarioStep extends Partial<PreviewStep> {
  /** Which of the two wires carry traffic: 0 = first–second node, 1 = second–third. */
  connections: number[];
  /** True when the traffic runs right to left. */
  reverse: boolean;
}

export interface PreviewScenario {
  id: string;
  /** Tab label. */
  label: string;
  /** Headline of the last step. */
  result: string;
  /** Sample outcome code shown with the description on the last step. */
  code: string;
  description: string;
  /** Exactly one entry per default step. */
  steps: PreviewScenarioStep[];
}

export interface PreviewFeature {
  icon: string;
  title: string;
  body: string;
}

export interface SimulatorPreviewData {
  id: PreviewId;
  name: string;
  icon: string;
  /** The guide's URL. */
  link: string;
  /** Name under /media/video and /media/poster. */
  media: string;
  /** Mono line above the overview heading. */
  slug: string;
  /** The simulator type as the desktop app names it. */
  typeName: string;
  /** Hero meta line, joined with " · ". */
  protocols: string[];
  /** Hero lede. */
  description: string;
  /** First sentence of the workflow section's lede. */
  overviewTitle: string;
  /** Overview lede. */
  overviewText: string;
  /** Second overview paragraph; GuideRich markdown. */
  overviewMore: string;
  nodes: PreviewNode[];
  steps: PreviewStep[];
  scenarios: PreviewScenario[];
  /** The three capability cards. */
  features: PreviewFeature[];
  /** The full planned scope, as a list. */
  planned: string[];
  /** "Where it fits": how it sits beside the simulators that ship today; GuideRich markdown. */
  fit: string;
  /** The available simulator to use in the meantime. */
  related: { label: string; link: string; body: string };
}

export const SWITCH_PREVIEW: SimulatorPreviewData = {
  id: 'switch',
  name: 'Switch Simulator',
  icon: 'shuffle',
  link: '/simulator/payment-switch',
  media: 'switch',
  slug: 'switch-simulator',
  typeName: 'Payment Switch',
  protocols: ['BIN routing', 'Protocol translation', 'Stand-in'],
  description: 'A planned workspace for exploring how payments move between acquirers and issuers. Follow BIN routing, protocol translation and stand-in decisions in one connected flow.',
  overviewTitle: 'The connection behind a payment.',
  overviewText: 'A payment switch receives a request, identifies its destination and passes it to the right issuer. This simulator is being developed to make that path easier to explore, including message translation and the stand-in path when an issuer is unavailable.',
  overviewMore: 'The Payment Switch will sit in the middle of the network, receiving ISO 8583 transactions from acquiring endpoints and routing them to the correct issuer, with BIN-based routing, protocol translation and stand-in decisioning.',
  nodes: [
    { icon: 'storefront', label: 'Acquirer', detail: 'Starts the request' },
    { icon: 'shuffle', label: 'Switch', detail: 'Routes and translates' },
    { icon: 'bank', label: 'Issuer', detail: 'Returns a decision' },
  ],
  steps: [
    { title: 'Receive a payment', body: 'Begin with an illustrative payment request from an acquirer. Its sample BIN identifies the route to inspect.', tag: 'Receive', request: 'SOURCE   ACQUIRER_A\nBIN      SAMPLE_A\nACTION   PAYMENT', response: 'STATE    RECEIVED\nNEXT     ROUTE_LOOKUP', activeNode: 0 },
    { title: 'Choose the route', body: 'The planned routing flow matches the sample BIN to an issuer destination.', tag: 'Route', request: 'BIN      SAMPLE_A\nLOOKUP   ROUTING_RULE', response: 'MATCH    ROUTE_A\nTARGET   ISSUER_A', activeNode: 1 },
    { title: 'Translate and forward', body: 'Follow the concept of mapping the incoming message into the destination format before passing it onward.', tag: 'Translate', request: 'FROM     ACQUIRER_FORMAT\nTO       ISSUER_FORMAT', response: 'STATE    MAPPED\nTARGET   ISSUER_A', activeNode: 2 },
    { title: 'Trace the result', body: 'Return the illustrative issuer decision to the acquirer. Select an outcome above to explore a different fixed example.', tag: 'Return', request: 'SOURCE   ISSUER_A\nDECISION APPROVED', response: 'TARGET   ACQUIRER_A\nSTATE    COMPLETED', activeNode: 1 },
  ],
  features: [
    { icon: 'git-branch', title: 'BIN-based routing', body: 'Planned routing scenarios will show how a card range maps to an issuer destination.' },
    { icon: 'arrows-left-right', title: 'Protocol translation', body: 'Explore the planned message translation stage between acquirer and issuer connections.' },
    { icon: 'shield-check', title: 'Stand-in paths', body: 'The planned scope includes a stand-in decision path when an issuer cannot respond.' },
  ],
  planned: [
    'BIN / IIN-based routing tables to multiple destinations',
    'ISO 8583 protocol and format translation between endpoints',
    'Stand-in processing when a downstream host is unavailable',
    'Message enrichment, field mapping and MTI translation',
    'Per-route monitoring and throughput metrics',
  ],
  fit: 'Until the dedicated switch ships, the [Host Simulator](/simulator/host) in **Proxy** mode already bridges two endpoints and can inspect or modify traffic in transit.',
  scenarios: [
    { id: 'routed', label: 'Issuer available', result: 'Routed to issuer', code: 'ROUTE_MATCHED', description: 'The sample BIN matches Issuer A, which returns an illustrative approval.', steps: [
      { connections: [0], reverse: false, request: 'SOURCE   ACQUIRER_A\nBIN      SAMPLE_A\nACTION   PAYMENT', response: 'STATE    RECEIVED\nNEXT     ROUTE_LOOKUP' },
      { connections: [], reverse: false, request: 'BIN      SAMPLE_A\nLOOKUP   ROUTING_RULE', response: 'MATCH    ROUTE_A\nTARGET   ISSUER_A' },
      { connections: [1], reverse: false, request: 'FROM     ACQUIRER_FORMAT\nTO       ISSUER_FORMAT', response: 'STATE    MAPPED\nTARGET   ISSUER_A' },
      { connections: [0, 1], reverse: true, request: 'SOURCE   ISSUER_A\nDECISION SAMPLE_APPROVAL', response: 'TARGET   ACQUIRER_A\nSTATE    COMPLETED' },
    ] },
    { id: 'standin', label: 'Issuer unavailable', result: 'Stand-in path selected', code: 'STAND_IN', description: 'An unavailable issuer sends this fixed example through the planned stand-in path.', steps: [
      { connections: [0], reverse: false, request: 'SOURCE   ACQUIRER_A\nBIN      SAMPLE_A\nACTION   PAYMENT', response: 'STATE    RECEIVED\nNEXT     ROUTE_LOOKUP' },
      { connections: [], reverse: false, title: 'Check the destination', body: 'The sample BIN matches Issuer A, but this example marks that destination unavailable.', request: 'BIN      SAMPLE_A\nLOOKUP   ROUTING_RULE', response: 'MATCH    ROUTE_A\nISSUER   UNAVAILABLE' },
      { connections: [], reverse: false, title: 'Select the stand-in path', body: 'The concept follows a preset stand-in decision within the switch while the issuer is unavailable.', tag: 'Stand-in', request: 'ISSUER   UNAVAILABLE\nPATH     STAND_IN', response: 'SOURCE   SAMPLE_STAND_IN\nDECISION SAMPLE_APPROVAL', activeNode: 1 },
      { connections: [0], reverse: true, title: 'Return the stand-in result', body: 'Return the illustrative stand-in decision to the acquirer without an issuer response.', request: 'SOURCE   SAMPLE_STAND_IN\nDECISION SAMPLE_APPROVAL', response: 'TARGET   ACQUIRER_A\nSTATE    STAND_IN_COMPLETE', activeNode: 0 },
    ] },
    { id: 'unmatched', label: 'No matching route', result: 'Routing stops', code: 'NO_ROUTE', description: 'No sample BIN route matches, so the request remains at the switch.', steps: [
      { connections: [0], reverse: false, body: 'Begin with a fictional payment whose sample BIN has no configured destination in this example.', request: 'SOURCE   ACQUIRER_A\nBIN      SAMPLE_UNKNOWN\nACTION   PAYMENT', response: 'STATE    RECEIVED\nNEXT     ROUTE_LOOKUP' },
      { connections: [], reverse: false, title: 'Look for a route', body: 'The sample routing lookup finds no matching destination for this BIN.', request: 'BIN      SAMPLE_UNKNOWN\nLOOKUP   ROUTING_RULE', response: 'MATCH    NONE\nTARGET   NONE' },
      { connections: [], reverse: false, title: 'Stop forwarding', body: 'With no destination selected, this example stops before message translation or forwarding.', tag: 'Stop', request: 'MATCH    NONE\nTARGET   NONE', response: 'STATE    NOT_FORWARDED\nREASON   NO_ROUTE', activeNode: 1 },
      { connections: [0], reverse: true, title: 'Return the routing result', body: 'The acquirer receives the illustrative no-route result; no issuer decision is involved.', request: 'SOURCE   SWITCH\nREASON   NO_ROUTE', response: 'TARGET   ACQUIRER_A\nSTATE    ROUTING_STOPPED', activeNode: 0 },
    ] },
  ],
  related: { label: 'Host Simulator', link: '/simulator/host', body: 'Available now: configure host requests and responses while the Switch Simulator is in development.' },
};

export const ISSUER_PREVIEW: SimulatorPreviewData = {
  id: 'issuer',
  name: 'Issuer System',
  icon: 'bank',
  link: '/simulator/issuer',
  media: 'issuer',
  slug: 'issuer-system',
  typeName: 'Issuer System',
  protocols: ['PIN verification', 'ARQC verification', '0210 decisioning'],
  description: 'An issuer-side authorization workspace in development. Explore how PIN and ARQC verification, limits and response decisions fit into a payment journey.',
  overviewTitle: 'From a request to an answer.',
  overviewText: 'The issuer decides how to answer an authorization request. This system is being developed around the checks behind that decision: PIN verification, ARQC verification and limits, followed by an issuer response. The preview below uses fixed sample results.',
  overviewMore: 'The Issuer System will represent the card issuer at the far end of the network. It will authorize or decline requests against card and account state, verify online PINs and ARQCs, apply limits, and return `0210` responses with the right field 39 codes.',
  nodes: [
    { icon: 'shuffle', label: 'Payment request', detail: 'Arrives for authorization' },
    { icon: 'bank', label: 'Issuer System', detail: 'Evaluates the checks' },
    { icon: 'check-circle', label: 'Response', detail: 'Carries the decision' },
  ],
  steps: [
    { title: 'Receive the request', body: 'Start with a sample authorization request and a fictional account profile.', tag: 'Receive', request: 'ACTION   AUTHORIZE\nACCOUNT  DEMO_ACCOUNT\nAMOUNT   125.00', response: 'STATE    RECEIVED\nNEXT     VERIFICATION', activeNode: 0 },
    { title: 'Check verification results', body: 'The planned flow brings PIN and ARQC verification into the authorization journey. This preview displays preset results.', tag: 'Verify', request: 'CHECK    PIN\nCHECK    ARQC', response: 'PIN      SAMPLE_PASS\nARQC     SAMPLE_PASS', activeNode: 1 },
    { title: 'Evaluate the limit', body: 'Compare the example amount with an illustrative limit to explain where this check affects the decision.', tag: 'Evaluate', request: 'AMOUNT   125.00\nLIMIT    500.00', response: 'CHECK    WITHIN_LIMIT\nNEXT     DECISION', activeNode: 1 },
    { title: 'Build the response', body: 'The planned system will produce issuer decisions. This conceptual response summarizes a successful sample path.', tag: 'Respond', request: 'CHECKS   SAMPLE_PASS\nLIMIT    WITHIN_LIMIT', response: 'MTI      0210\nDECISION SAMPLE_APPROVAL', activeNode: 2 },
  ],
  features: [
    { icon: 'password', title: 'PIN verification', body: 'The planned authorization flow includes PIN verification as an issuer-side check.' },
    { icon: 'shield-check', title: 'ARQC verification', body: 'Explore where card cryptogram verification fits into the planned decision flow.' },
    { icon: 'sliders-horizontal', title: 'Limits and decisions', body: 'Planned limit checks feed into issuer-side decisioning and the resulting 0210 response.' },
  ],
  planned: [
    'Authorization decisioning against configurable card / account records',
    'Online PIN and ARQC (cryptogram) verification',
    'Balance, limit and velocity checks with stand-in rules',
    'Full ISO 8583 0200 → 0210 response construction',
    'Configurable approval / decline response-code scenarios',
  ],
  fit: 'The issuer answers what the acquiring side sends. Today you can model issuer responses with the [Host Simulator](/simulator/host) in **Server** mode; the dedicated Issuer System will add account-aware decisioning.',
  scenarios: [
    { id: 'approved', label: 'Checks pass', result: 'Sample approval', code: 'APPROVED', description: 'Both sample verification checks pass, and the amount remains within the illustrative limit.', steps: [
      { connections: [0], reverse: false, request: 'ACTION   AUTHORIZE\nACCOUNT  DEMO_ACCOUNT\nAMOUNT   125.00', response: 'STATE    RECEIVED\nNEXT     VERIFICATION' },
      { connections: [], reverse: false, request: 'CHECK    PIN\nCHECK    ARQC', response: 'PIN      SAMPLE_PASS\nARQC     SAMPLE_PASS' },
      { connections: [], reverse: false, request: 'AMOUNT   125.00\nLIMIT    500.00', response: 'CHECK    WITHIN_LIMIT\nNEXT     DECISION' },
      { connections: [1], reverse: false, request: 'CHECKS   SAMPLE_PASS\nLIMIT    WITHIN_LIMIT', response: 'MTI      0210\nDECISION SAMPLE_APPROVAL' },
    ] },
    { id: 'verification', label: 'Verification fails', result: 'Sample decline', code: 'VERIFY_FAILED', description: 'A failed sample PIN verification sends this example through the decline path.', steps: [
      { connections: [0], reverse: false, request: 'ACTION   AUTHORIZE\nACCOUNT  DEMO_ACCOUNT\nAMOUNT   125.00', response: 'STATE    RECEIVED\nNEXT     VERIFICATION' },
      { connections: [], reverse: false, body: 'This fixed example passes the sample ARQC check but fails the sample PIN check.', request: 'CHECK    PIN\nCHECK    ARQC', response: 'PIN      SAMPLE_FAIL\nARQC     SAMPLE_PASS' },
      { connections: [], reverse: false, title: 'Follow the decline path', body: 'The failed verification selects a sample decline; this example does not proceed to the limit check.', tag: 'Decline', request: 'PIN      SAMPLE_FAIL\nARQC     SAMPLE_PASS', response: 'LIMIT    NOT_EVALUATED\nNEXT     SAMPLE_DECLINE' },
      { connections: [1], reverse: false, body: 'Return a conceptual issuer decline reflecting the failed verification.', request: 'CHECKS   SAMPLE_FAIL\nREASON   PIN_VERIFICATION', response: 'MTI      0210\nDECISION SAMPLE_DECLINE' },
    ] },
    { id: 'limit', label: 'Limit exceeded', result: 'Limit check declines', code: 'LIMIT_EXCEEDED', description: 'Verification passes, but the sample amount exceeds the illustrative authorization limit.', steps: [
      { connections: [0], reverse: false, body: 'Start with a fictional authorization amount above the limit used in this example.', request: 'ACTION   AUTHORIZE\nACCOUNT  DEMO_ACCOUNT\nAMOUNT   625.00', response: 'STATE    RECEIVED\nNEXT     VERIFICATION' },
      { connections: [], reverse: false, request: 'CHECK    PIN\nCHECK    ARQC', response: 'PIN      SAMPLE_PASS\nARQC     SAMPLE_PASS' },
      { connections: [], reverse: false, body: 'The sample amount is greater than the illustrative limit, selecting the decline path.', request: 'AMOUNT   625.00\nLIMIT    500.00', response: 'CHECK    LIMIT_EXCEEDED\nNEXT     SAMPLE_DECLINE' },
      { connections: [1], reverse: false, body: 'Return a conceptual issuer decline reflecting the exceeded limit.', request: 'CHECKS   SAMPLE_PASS\nLIMIT    EXCEEDED', response: 'MTI      0210\nDECISION SAMPLE_DECLINE' },
    ] },
  ],
  related: { label: 'HSM Simulator', link: '/simulator/hsm', body: 'Available now: explore host commands, PIN operations and key management with the HSM Simulator.' },
};

export const ATM_PREVIEW: SimulatorPreviewData = {
  id: 'atm',
  name: 'ATM Simulator',
  icon: 'money',
  link: '/simulator/atm',
  media: 'atm',
  slug: 'atm-simulator',
  typeName: 'ATM Simulator',
  protocols: ['NDC / DDC', 'Device states', 'Withdrawal flows'],
  description: 'A planned environment for ATM journeys, from withdrawal and balance inquiries to PIN changes. See how customer actions, device states and host responses connect.',
  overviewTitle: 'A transaction beyond the screen.',
  overviewText: 'An ATM flow links a customer action to device behavior and a host response. This simulator is being developed for withdrawal, balance and PIN-change journeys with NDC/DDC device states, so the intended flow can be explored as a connected sequence.',
  overviewMore: 'The ATM Simulator will represent a self-service cash machine. It will originate ISO 8583 financial requests (withdrawal, balance, transfer, PIN change), model device-level NDC/DDC state flows, and drive them to a host or switch, the same way the [POS Simulator](/simulator/pos) models an attended terminal.',
  nodes: [
    { icon: 'user', label: 'Customer', detail: 'Chooses an action' },
    { icon: 'money', label: 'ATM', detail: 'Moves through states' },
    { icon: 'bank', label: 'Host', detail: 'Returns the result' },
  ],
  steps: [
    { title: 'Choose a transaction', body: 'Start the illustrative session with a withdrawal. Balance and PIN-change flows are also part of the planned scope.', tag: 'Select', request: 'SESSION  DEMO_SESSION\nACTION   WITHDRAWAL', response: 'STATE    AMOUNT_SELECTION\nNEXT     ENTER_AMOUNT', activeNode: 0 },
    { title: 'Prepare the request', body: 'The ATM concept moves from the selected customer action into a host request.', tag: 'Request', request: 'ACTION   WITHDRAWAL\nAMOUNT   100.00', response: 'STATE    WAITING_FOR_HOST\nTARGET   DEMO_HOST', activeNode: 1 },
    { title: 'Receive a host decision', body: 'A preset host approval advances this example. Each preview result is illustrative and repeatable.', tag: 'Authorize', request: 'SOURCE   DEMO_ATM\nACTION   WITHDRAWAL', response: 'DECISION SAMPLE_APPROVAL\nNEXT     DEVICE_ACTION', activeNode: 2 },
    { title: 'Follow the device state', body: 'The conceptual ATM completes the approved withdrawal and returns to its ready state. No physical device is connected.', tag: 'Complete', request: 'ACTION   SAMPLE_DISPENSE\nAMOUNT   100.00', response: 'RESULT   SAMPLE_COMPLETE\nSTATE    READY', activeNode: 1 },
  ],
  features: [
    { icon: 'money', title: 'Withdrawal journeys', body: 'Planned withdrawal flows will connect customer selections, host decisions and ATM device states.' },
    { icon: 'list-checks', title: 'Balance and PIN flows', body: 'Balance inquiries and PIN changes are included in the planned transaction scope.' },
    { icon: 'cpu', title: 'NDC/DDC device states', body: 'The simulator is being developed to represent device states alongside the transaction journey.' },
  ],
  planned: [
    'Cash withdrawal, balance inquiry, mini-statement and PIN-change transaction flows',
    'NDC / DDC device state modelling (card read, PIN entry, dispense, eject)',
    'Configurable cassettes and note denominations with dispense simulation',
    'ISO 8583 messaging to a host or switch over TCP/IP',
    'Electronic journal / transaction log',
  ],
  fit: 'The ATM sits at the acceptance edge of the network, alongside the [POS Simulator](/simulator/pos). Pair it with the [Host Simulator](/simulator/host) to authorize its transactions.',
  scenarios: [
    { id: 'withdrawal', label: 'Withdrawal', result: 'Withdrawal complete', code: 'CASH_DISPENSED', description: 'A sample approval advances the ATM through an illustrative cash-dispense state.', steps: [
      { connections: [0], reverse: false, request: 'SESSION  DEMO_SESSION\nACTION   WITHDRAWAL', response: 'STATE    AMOUNT_SELECTION\nNEXT     ENTER_AMOUNT' },
      { connections: [1], reverse: false, request: 'ACTION   WITHDRAWAL\nAMOUNT   100.00', response: 'STATE    WAITING_FOR_HOST\nTARGET   DEMO_HOST' },
      { connections: [1], reverse: true, request: 'SOURCE   DEMO_ATM\nACTION   WITHDRAWAL', response: 'DECISION SAMPLE_APPROVAL\nNEXT     DEVICE_ACTION' },
      { connections: [0], reverse: true, request: 'ACTION   SAMPLE_DISPENSE\nAMOUNT   100.00', response: 'RESULT   SAMPLE_COMPLETE\nSTATE    READY' },
    ] },
    { id: 'balance', label: 'Balance inquiry', result: 'Balance returned', code: 'BALANCE_RECEIVED', description: 'The sample host returns a fictional account balance for display at the ATM.', steps: [
      { connections: [0], reverse: false, title: 'Choose a balance inquiry', body: 'Begin the illustrative session with a balance inquiry for a fictional account.', request: 'SESSION  DEMO_SESSION\nACTION   BALANCE_INQUIRY', response: 'STATE    ACCOUNT_SELECTED\nNEXT     HOST_REQUEST' },
      { connections: [1], reverse: false, body: 'The conceptual ATM prepares a balance request for the sample host.', request: 'ACTION   BALANCE_INQUIRY\nACCOUNT  DEMO_ACCOUNT', response: 'STATE    WAITING_FOR_HOST\nTARGET   DEMO_HOST' },
      { connections: [1], reverse: true, title: 'Receive the balance', body: 'The fixed host response supplies a fictional available balance.', tag: 'Receive', request: 'SOURCE   DEMO_ATM\nACTION   BALANCE_INQUIRY', response: 'RESULT   SAMPLE_SUCCESS\nBALANCE  1250.00' },
      { connections: [0], reverse: true, title: 'Display the balance', body: 'The example displays the fictional balance and returns the ATM to its ready state.', request: 'ACTION   DISPLAY_BALANCE\nBALANCE  1250.00', response: 'RESULT   BALANCE_SHOWN\nSTATE    READY' },
    ] },
    { id: 'pin', label: 'PIN change', result: 'PIN change confirmed', code: 'PIN_UPDATED', description: 'The sample host confirms a conceptual PIN change without processing a real PIN.', steps: [
      { connections: [0], reverse: false, title: 'Choose a PIN change', body: 'Begin a conceptual PIN-change session; this preview does not request or store a real PIN.', request: 'SESSION  DEMO_SESSION\nACTION   PIN_CHANGE', response: 'STATE    SAMPLE_INPUT_READY\nNEXT     HOST_REQUEST' },
      { connections: [1], reverse: false, body: 'The example prepares a PIN-change request using a placeholder instead of a PIN value.', request: 'ACTION   PIN_CHANGE\nPIN      DEMO_PLACEHOLDER', response: 'STATE    WAITING_FOR_HOST\nTARGET   DEMO_HOST' },
      { connections: [1], reverse: true, title: 'Receive confirmation', body: 'A fixed host result confirms the conceptual PIN change.', tag: 'Confirm', request: 'SOURCE   DEMO_ATM\nACTION   PIN_CHANGE', response: 'RESULT   SAMPLE_SUCCESS\nNEXT     SHOW_CONFIRMATION' },
      { connections: [0], reverse: true, title: 'Show the confirmation', body: 'The ATM concept shows the sample confirmation and returns to its ready state.', request: 'ACTION   SHOW_CONFIRMATION\nRESULT   PIN_UPDATED', response: 'RESULT   SAMPLE_COMPLETE\nSTATE    READY' },
    ] },
  ],
  related: { label: 'Host Simulator', link: '/simulator/host', body: 'Available now: configure sample host responses for your transaction testing with the Host Simulator.' },
};

export const ECR_PREVIEW: SimulatorPreviewData = {
  id: 'ecr',
  name: 'ECR Simulator',
  icon: 'printer',
  link: '/simulator/ecr',
  media: 'ecr',
  slug: 'ecr-simulator',
  typeName: 'ECR Simulator',
  protocols: ['Sale', 'Void', 'Refund'],
  description: 'An electronic cash register simulator in development. Explore how a checkout initiates a sale, void or refund on a payment terminal and receives the result.',
  overviewTitle: 'Give the checkout a voice.',
  overviewText: 'An electronic cash register starts the payment action at checkout and hands it to the terminal. This simulator is being developed to explore that exchange for sale, void and refund flows, from the initial request to the result returned to the register.',
  overviewMore: 'The ECR Simulator will model the cash-register side of an ECR ↔ POS integration: it issues transaction requests (sale, void, refund, settlement) to a connected terminal and consumes the results, over serial (RS232) or TCP links.',
  nodes: [
    { icon: 'shopping-cart', label: 'Checkout', detail: 'Prepares the action' },
    { icon: 'printer', label: 'ECR', detail: 'Drives the exchange' },
    { icon: 'credit-card', label: 'Terminal', detail: 'Returns the result' },
  ],
  steps: [
    { title: 'Prepare the checkout', body: 'Begin with a fictional basket total and a sale action at the register.', tag: 'Prepare', request: 'ORDER    DEMO_0042\nACTION   SALE\nAMOUNT   42.00', response: 'STATE    READY_TO_SEND\nTARGET   DEMO_TERMINAL', activeNode: 0 },
    { title: 'Send to the terminal', body: 'The planned ECR flow hands the chosen payment action and amount to the terminal.', tag: 'Send', request: 'ACTION   SALE\nAMOUNT   42.00\nTARGET   DEMO_TERMINAL', response: 'STATE    REQUEST_RECEIVED\nNEXT     TERMINAL_RESULT', activeNode: 1 },
    { title: 'Wait for the result', body: 'Follow the conceptual terminal exchange. This preview uses a fixed success result to explain the return path.', tag: 'Process', request: 'ORDER    DEMO_0042\nSTATE    PAYMENT_IN_PROGRESS', response: 'ACTION   SALE\nRESULT   SAMPLE_SUCCESS', activeNode: 2 },
    { title: 'Update the checkout', body: 'Return the illustrative result to the register and complete the sample checkout action.', tag: 'Complete', request: 'SOURCE   DEMO_TERMINAL\nRESULT   SAMPLE_SUCCESS', response: 'ORDER    DEMO_0042\nSTATE    SALE_COMPLETE', activeNode: 1 },
  ],
  features: [
    { icon: 'shopping-cart', title: 'Sale requests', body: 'Planned sale flows will show how a register sends a checkout payment to the terminal.' },
    { icon: 'arrow-counter-clockwise', title: 'Void requests', body: 'Explore the planned exchange for a register-initiated void and its terminal result.' },
    { icon: 'receipt', title: 'Refund requests', body: 'Refunds form a third planned flow, following the action from the register through to its result.' },
  ],
  planned: [
    'Sale, void, refund, pre-auth and settlement request messages',
    'ECR ↔ POS integration over RS232 serial and TCP/IP',
    'Configurable register protocol framing and field mapping',
    'Basket / line-item and tender modelling',
    'Response handling and receipt data capture',
  ],
  fit: 'The ECR drives the [POS Simulator](/simulator/pos) from the merchant application side, completing the register → terminal → host chain.',
  scenarios: [
    { id: 'sale', label: 'Sale', result: 'Sale completed', code: 'SALE_COMPLETE', description: 'The register sends a fictional checkout total and receives a successful sample sale result.', steps: [
      { connections: [0], reverse: false, request: 'ORDER    DEMO_0042\nACTION   SALE\nAMOUNT   42.00', response: 'STATE    READY_TO_SEND\nTARGET   DEMO_TERMINAL' },
      { connections: [1], reverse: false, request: 'ACTION   SALE\nAMOUNT   42.00\nTARGET   DEMO_TERMINAL', response: 'STATE    REQUEST_RECEIVED\nNEXT     TERMINAL_RESULT' },
      { connections: [1], reverse: true, request: 'ORDER    DEMO_0042\nSTATE    PAYMENT_IN_PROGRESS', response: 'ACTION   SALE\nRESULT   SAMPLE_SUCCESS' },
      { connections: [0], reverse: true, request: 'SOURCE   DEMO_TERMINAL\nRESULT   SAMPLE_SUCCESS', response: 'ORDER    DEMO_0042\nSTATE    SALE_COMPLETE' },
    ] },
    { id: 'void', label: 'Void', result: 'Void confirmed', code: 'VOID_COMPLETE', description: 'The register requests a void for a sample transaction and receives terminal confirmation.', steps: [
      { connections: [0], reverse: false, title: 'Prepare a void', body: 'Begin with a void action referring to a fictional transaction.', request: 'REFERENCE DEMO_0042\nACTION    VOID', response: 'STATE     READY_TO_SEND\nTARGET    DEMO_TERMINAL' },
      { connections: [1], reverse: false, body: 'The planned ECR flow sends the void action and sample reference to the terminal.', request: 'ACTION    VOID\nREFERENCE DEMO_0042\nTARGET    DEMO_TERMINAL', response: 'STATE     REQUEST_RECEIVED\nNEXT      TERMINAL_RESULT' },
      { connections: [1], reverse: true, body: 'The conceptual terminal exchange returns a fixed successful void result.', request: 'REFERENCE DEMO_0042\nSTATE     VOID_IN_PROGRESS', response: 'ACTION    VOID\nRESULT    SAMPLE_SUCCESS' },
      { connections: [0], reverse: true, title: 'Confirm the void', body: 'Return the illustrative void result to the register for the sample transaction.', request: 'SOURCE    DEMO_TERMINAL\nRESULT    SAMPLE_SUCCESS', response: 'REFERENCE DEMO_0042\nSTATE     VOID_COMPLETE' },
    ] },
    { id: 'refund', label: 'Refund', result: 'Refund confirmed', code: 'REFUND_COMPLETE', description: 'The register initiates a fictional refund and receives a successful sample terminal result.', steps: [
      { connections: [0], reverse: false, title: 'Prepare a refund', body: 'Begin with a fictional refund amount and a sample transaction reference.', request: 'REFERENCE DEMO_0042\nACTION    REFUND\nAMOUNT    42.00', response: 'STATE     READY_TO_SEND\nTARGET    DEMO_TERMINAL' },
      { connections: [1], reverse: false, body: 'The planned ECR flow hands the refund action and sample amount to the terminal.', request: 'ACTION    REFUND\nAMOUNT    42.00\nTARGET    DEMO_TERMINAL', response: 'STATE     REQUEST_RECEIVED\nNEXT      TERMINAL_RESULT' },
      { connections: [1], reverse: true, body: 'The conceptual terminal exchange returns a fixed successful refund result.', request: 'REFERENCE DEMO_0042\nSTATE     REFUND_IN_PROGRESS', response: 'ACTION    REFUND\nRESULT    SAMPLE_SUCCESS' },
      { connections: [0], reverse: true, title: 'Confirm the refund', body: 'Return the illustrative refund result to the register for the sample transaction.', request: 'SOURCE    DEMO_TERMINAL\nRESULT    SAMPLE_SUCCESS', response: 'REFERENCE DEMO_0042\nSTATE     REFUND_COMPLETE' },
    ] },
  ],
  related: { label: 'POS Simulator', link: '/simulator/pos', body: 'In beta now: explore payment terminal apps in the Android emulator with the POS Simulator.' },
};

/** All four, in rail order. Each guide lists the others and links on to the next. */
export const SIMULATOR_PREVIEWS: readonly SimulatorPreviewData[] = [
  SWITCH_PREVIEW, ISSUER_PREVIEW, ATM_PREVIEW, ECR_PREVIEW,
];
