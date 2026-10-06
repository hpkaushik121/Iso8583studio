/**
 * Host Simulator guide (/simulator/host) — page content. Ported from the
 * prototype's host-simulator-data.js; imported only by
 * pages/site/docs-host-simulator.ts. The glass screens the blocks refer to are
 * in pages/shared/simglass/host-screens.ts.
 */
import { GuideCard } from '../shared/guide';
import { HostScreenId } from '../shared/simglass/host-screens';
import { SimGuideData, SimSection, screen, simFeatures, stage } from '../shared/simglass/sim-guide';

const STAGES = [['01', 'Configure', 'Gateway · Transmission · Logs'], ['02', 'Run', 'Transactions · Rules · Template'], ['03', 'Reference', 'Security · Tabs']];

const CARDS: GuideCard[] = [
  { id: 'host-handler', icon: 'arrows-left-right', name: 'ISO8583 Transaction', tag: 'Server · Proxy', desc: 'Start / stop, the live request / response split view with formatted and raw hex panes, and the hold message control.' },
  { id: 'log-settings', icon: 'list-dashes', name: 'Logs', tag: 'Server · Client · Proxy', desc: 'Transaction and connection logs, connection count, bytes in / out, written to a rotating logfile.' },
  { id: 'transaction-rules', icon: 'gear', name: 'Settings', tag: 'Server · Proxy', desc: 'The transaction list with Fields / Config / API tabs for each rule — what the simulator matches and what it answers.' },
  { id: 'iso8583-template', icon: 'code', name: 'ISO8583 Template', tag: 'Server · Client · Proxy', desc: 'Message specification, bit definitions, YAML import / export and the template-level toggles.' },
  { id: 'unsolicited', icon: 'envelope-open', name: 'Unsolicited Message', tag: 'Server · Client · Proxy', desc: 'Messages outside the request-response flow — send them in Client / Proxy mode, view incoming ones in Server / Proxy mode.' },
];

const SECTIONS: SimSection<HostScreenId>[] = [
  {
    id: 'quick-start', title: 'Quick Start Guide', rail: 'Quick start', icon: 'lightning', eyebrow: stage(STAGES, 0),
    intro: 'Follow these steps to get a basic host simulator up and running:',
    blocks: [
      { t: 'steps', items: [
        '**Create a new simulator** — From the Home screen, navigate to `Simulator → Host Simulator` and create a new configuration.',
        '**Choose a Gateway Type** — Select **Server** to accept incoming connections, **Client** to connect outward, or **Proxy** to bridge two sides.',
        '**Configure Transmission Settings** — Set the IP address, port, connection type (TCP/IP or REST), and message length type.',
        '**Configure the ISO8583 Template** — Open the **ISO8583 Template** tab and import a YAML template via **Upload YAML**, or build the bit definitions manually. Save with the top-bar **Save** button.',
        '**Add Transactions and Responses** — In the **Settings** tab, click **+** in the transaction list to add a transaction (Description, MTI, Processing Code). Select it, then in the right panel use the **Fields** tab to set the response field values, the **Config** tab for transaction-level options, and the **API** tab for REST matching. Click **Save All** in the header.',
        '**Start the Simulator** — Open the **ISO8583 Transaction** tab and click **Start**. Incoming traffic appears live in the request / response split view.',
      ] },
      { t: 'note', tone: 'teal', icon: 'lightbulb', title: 'Tip', x: 'Use placeholders like `[SV]` in response field values to echo back request values. Useful for fields like System Trace or RRN that must match the original request.' },
    ],
  },
  {
    id: 'gateway-types', title: 'Gateway Types', rail: 'Gateway types', icon: 'laptop', eyebrow: stage(STAGES, 0),
    intro: 'The host simulator supports three gateway modes, each suited to different testing scenarios:',
    blocks: [
      screen('gateway', 'the Server, Client and Proxy tiles with the Synchronous / Asynchronous transmission type below'),
      { t: 'table', head: ['Gateway Type', 'Mode', 'Description', 'Use Case'], cols: '110px 130px minmax(0,1.3fr) minmax(0,1fr)', rows: [
        ['**Server**', 'Listens for connections', 'Binds to a port and accepts incoming connections from clients (POS, ATM, payment apps). Parses incoming ISO8583 requests and returns configured responses.', 'Simulating an acquirer or issuer host during terminal testing.'],
        ['**Client**', 'Initiates connections', 'Connects outward to an external host. Allows sending crafted messages and inspecting responses.', 'Testing outbound integrations, sending test transactions to a real or simulated host.'],
        ['**Proxy**', 'Bridges two sides', 'Receives from one side (source), optionally modifies, and forwards to the other side (destination). Returns destination responses to source.', 'Man-in-the-middle testing, message inspection, protocol translation between endpoints.'],
      ] },
      { t: 'note', tone: 'blue', icon: 'info', title: 'Note', x: 'For **Server** and **Proxy** modes, you can choose between **Synchronous** (one request at a time per connection) and **Asynchronous** (multiple concurrent requests) transmission modes.' },
    ],
  },
  {
    id: 'gateway-config', title: 'Gateway Configuration', rail: 'Gateway configuration', icon: 'gauge', eyebrow: stage(STAGES, 0),
    intro: 'The Gateway Type tab provides the core configuration for your simulator instance.',
    blocks: [
      screen('response', 'Response Settings with the Realistic response delay selected, Max Concurrent set to 50, and Advanced Options with Enable Detailed Logging checked'),
      { t: 'h3', x: 'Basic Settings' },
      { t: 'table', head: ['Setting', 'Description'], rows: [
        ['**Name**', 'A descriptive name for your simulator instance.'],
        ['**Description**', 'Optional description for documentation purposes.'],
        ['**Gateway Type**', 'Server, Client, or Proxy.'],
        ['**Auto-Start**', 'Automatically start the simulator when the configuration is loaded.'],
      ] },
      { t: 'h3', x: 'Response Delay' },
      { t: 'p', x: 'Control how quickly the simulator responds to incoming requests. Useful for simulating real-world network conditions.' },
      { t: 'table', head: ['Option', 'Delay', 'Description'], cols: '120px 120px minmax(0,1fr)', rows: [
        ['Instant', '0 ms', 'Responds immediately. Ideal for high-throughput testing.'],
        ['Realistic', '~200-500 ms', 'Simulates typical production host response times.'],
        ['Slow', '~1-3 s', 'Simulates slow networks or stressed hosts.'],
        ['Custom', 'User-defined', 'Set an exact delay in milliseconds.'],
      ] },
      { t: 'h3', x: 'Performance Settings' },
      { t: 'bullets', items: [
        ['Max Concurrent Transactions', 'Limit the number of simultaneous transactions the simulator will process.'],
        ['Detailed Logging', 'Enable verbose logging for debugging purposes.'],
      ] },
    ],
  },
  {
    id: 'transmission', title: 'Transmission Settings', rail: 'Transmission settings', icon: 'plugs-connected', eyebrow: stage(STAGES, 0),
    intro: 'Configure how the simulator communicates over the network. Settings differ based on your gateway type.',
    blocks: [
      screen('connections', 'incoming and outgoing connection settings side by side, each with a connection type and a message length format'),
      { t: 'h3', x: 'Incoming Connection (Server / Proxy)' },
      { t: 'p', x: 'Configure the listener that accepts incoming connections:' },
      { t: 'bullets', items: [
        ['IP Address', 'Bind address (e.g. `0.0.0.0` for all interfaces, or a specific IP).'],
        ['Port', 'TCP port to listen on.'],
        ['Message Length Type', 'How message boundaries are determined.'],
        ['Max Concurrent Connections', 'Limit on simultaneous client connections.'],
        ['Timeout', 'Connection idle timeout in seconds.'],
      ] },
      { t: 'h3', x: 'Outgoing Connection (Client / Proxy)' },
      { t: 'p', x: 'Configure the connection to the destination host:' },
      { t: 'bullets', items: [
        ['Target Address', 'Hostname or IP of the target.'],
        ['Target Port', 'Port to connect to.'],
        ['Terminate on Error', 'Whether to close the connection on errors.'],
      ] },
      { t: 'h3', x: 'Message Length Types' },
      { t: 'table', head: ['Type', 'Description'], cols: '140px minmax(0,1fr)', rows: [
        ['`BCD`', 'Binary-coded decimal length header.'],
        ['`NONE`', 'No length header; messages are delimited by the stream itself.'],
        ['`STRING_4`', '4-character ASCII string length prefix.'],
        ['`HEX_HL`', '2-byte hex length header, high byte first.'],
        ['`HEX_LH`', '2-byte hex length header, low byte first.'],
      ] },
    ],
  },
  {
    id: 'connection-types', title: 'Connection Types', rail: 'Connection types', icon: 'wifi-high', eyebrow: stage(STAGES, 0),
    intro: 'The simulator supports multiple connection protocols for maximum flexibility:',
    blocks: [
      { t: 'table', head: ['Type', 'Protocol', 'Status', 'Description'], cols: '90px 110px 100px minmax(0,1fr)', rows: [
        ['**TCP/IP**', 'TCP Socket', 'Full support', 'Standard TCP/IP socket connections. Most common for ISO8583 host communication.'],
        ['**REST**', 'HTTP/HTTPS', 'Full support', 'REST API endpoints supporting JSON, XML, HEX, BASE64, BINARY, and FORM_DATA formats.'],
        ['**COM**', 'RS232 Serial', 'Available', 'Serial port communication with configurable baud rate, data bits, stop bits, and parity.'],
        ['**Dial-Up**', 'Modem', 'Available', 'Legacy modem dial-up connections via phone number.'],
      ] },
    ],
  },
  {
    id: 'message-formats', title: 'Message Formats', rail: 'Message formats', icon: 'swap', eyebrow: stage(STAGES, 0),
    intro: 'The simulator can handle multiple message encoding formats for both source and destination.',
    blocks: [
      { t: 'h3', x: 'ISO8583 Formats' },
      { t: 'table', head: ['Format', 'Description'], cols: '150px minmax(0,1fr)', rows: [
        ['`BYTE_ARRAY`', 'Standard binary ISO8583 encoding. Default for most payment hosts.'],
        ['`JSON`', 'JSON representation with configurable field mapping.'],
        ['`XML`', 'XML representation with configurable field mapping.'],
        ['`HEX`', 'Hexadecimal string representation of the binary message.'],
        ['`PLAIN_TEXT`', 'Key-value pairs with configurable delimiters.'],
      ] },
      { t: 'h3', x: 'REST Message Formats' },
      { t: 'p', x: 'When using REST connections, the following content types are supported:' },
      { t: 'bullets', cols: 2, items: [
        ['JSON', 'Application/json content type'],
        ['XML', 'Application/xml content type'],
        ['HEX', 'Hex-encoded message body'],
        ['BASE64', 'Base64-encoded message body'],
        ['BINARY', 'Raw binary body'],
        ['FORM_DATA', 'Multipart form data'],
      ] },
      { t: 'h3', x: 'Format Mapping' },
      { t: 'p', x: 'For non-binary formats (JSON, XML, PlainText), use the **Format Mapping Config** in the ISO8583 Template tab to map ISO8583 fields to JSON keys, XML elements, or delimited fields.' },
    ],
  },
  {
    id: 'log-settings', title: 'Log Settings', rail: 'Log settings', icon: 'list-dashes', eyebrow: stage(STAGES, 0),
    intro: 'Configure how the simulator logs transactions and connections.',
    blocks: [
      screen('logging', 'Logging Options with logfile name and max size, and Logging Content set to Parsed data using the ISO8583 protocol'),
      { t: 'table', head: ['Setting', 'Description'], rows: [
        ['**Log Filename**', 'Output file path for transaction logs.'],
        ['**Max Log Size**', 'Maximum log file size in MB before rotation.'],
        ['**Simple**', 'Basic transaction summaries (MTI, response code, timestamps).'],
        ['**Raw Data**', 'Full hex dump of request/response messages.'],
        ['**Text Data**', 'Text-decoded message content with configurable encoding.'],
        ['**Parsed Data**', 'Fully parsed ISO8583 fields using a template file for structured output.'],
      ] },
    ],
  },
  {
    id: 'host-handler', title: 'ISO8583 Transaction Tab', rail: 'ISO8583 Transaction', icon: 'arrows-left-right', eyebrow: stage(STAGES, 1),
    intro: 'The **ISO8583 Transaction** tab (visible for Server and Proxy gateway types) is the main runtime interface for controlling and monitoring the simulator.',
    blocks: [
      screen('parser', 'Message Parser — a raw ISO8583 hex message parsed into 12 fields, field 25 selected and its point of service condition code decoded'),
      screen('bitmap', 'Bitmap Analysis — the twelve set fields of bitmap 3038048020C00014'),
      { t: 'h3', x: 'Controls' },
      { t: 'bullets', items: [
        ['Start / Stop', 'Launch or shut down the simulator server.'],
        ['Hold Message', 'When enabled, the simulator will not auto-respond. Responses are sent only when you manually click "Send" or after a configurable delay. Useful for inspecting requests before responding.'],
      ] },
      { t: 'h3', x: 'Live View' },
      { t: 'p', x: 'The transaction view provides real-time visibility into all traffic:' },
      { t: 'bullets', items: [
        ['Formatted View', 'Parsed ISO8583 fields shown in a readable table.'],
        ['Raw Hex View', 'Full hexadecimal dump of the message bytes.'],
        ['Request / Response Split', 'Side-by-side view of incoming request and outgoing response.'],
      ] },
    ],
  },
  {
    id: 'transaction-rules', title: 'Transaction Rules (Settings Tab)', rail: 'Transaction rules', icon: 'gear', eyebrow: stage(STAGES, 1),
    intro: 'Transaction rules define how the simulator matches incoming requests and what responses to return. Configure them in the **Settings** tab.',
    blocks: [
      screen('settings', 'SALE, REVERSAL and VOID transactions on the left and the Fields tab on the right with 13 configured ISO8583 fields'),
      { t: 'h3', x: 'Settings Tab Layout' },
      { t: 'bullets', items: [
        ['Header bar', 'Field info, Export, Import, **Save All**, plus a **Source / Destination** toggle (Server simulators default to Source; Proxy mode lets you stage rules for either side).'],
        ['Left panel: Transaction list', 'Add, edit, duplicate, delete transactions. Click a row to select it.'],
        ['Right panel: Per-transaction tabs', '`Fields`, `Config`, and (REST connections only) `API`.'],
      ] },
      { t: 'h3', x: 'Adding a Transaction' },
      { t: 'steps', items: [
        'Click **+** at the top of the transaction list. The *Add Transaction* dialog opens.',
        'Fill in **Description *** (e.g. `Purchase Transaction`).',
        'Fill in **MTI *** (e.g. `0200`).',
        'Fill in **Processing Code *** (e.g. `000000`; use `*` as a wildcard for any code).',
        'Click **Save**. The new transaction appears in the list.',
      ] },
      { t: 'h3', x: 'Setting up the Response (Fields tab)' },
      { t: 'steps', items: [
        'Select the transaction in the left list.',
        'Open the **Fields** tab on the right.',
        'Click **Add Fields** to pick which ISO 8583 bits the response should contain.',
        'For each added field, type its response value. Use placeholders — `[SV]` echoes the request value, `[TIME]` generates a timestamp, `[RAND]` generates random digits.',
        'Click **Save All** in the header bar to persist the rules.',
      ] },
      { t: 'h3', x: 'Per-Transaction Config & API tabs' },
      { t: 'bullets', items: [
        ['Config tab', 'Transaction-level options such as response delay overrides and conditional logic.'],
        ['API tab (REST connections only)', '*API Path*, request matchers (key path / operator / value), response mappers, and headers.'],
      ] },
      { t: 'h3', x: 'Matching Logic' },
      { t: 'p', x: 'When a request arrives, the simulator evaluates rules in order:' },
      { t: 'steps', items: [
        'Parse the incoming message to extract MTI and processing code (Field 3).',
        'Compare against each configured transaction rule.',
        'The **first matching rule** wins and its response fields are used to build the response.',
      ] },
      { t: 'note', tone: 'teal', icon: 'lightbulb', title: 'Tip', x: 'Place more specific rules (with exact processing codes) before generic ones (with `*` wildcard) to ensure correct matching priority.' },
      { t: 'h3', x: 'Example Rule' },
      { t: 'code', lines: [
        'Description:     Purchase Transaction',
        'MTI:             0200',
        'Processing Code: 000000',
        '',
        'Response Fields (Fields tab):',
        '  Field 39 (Response Code):     00',
        '  Field 38 (Auth Code):         [RAND]',
        '  Field 11 (System Trace):      [SV]',
        '  Field 37 (RRN):               [SV]',
        '  Field 12 (Local Time):        [TIME]',
        '  Field 13 (Local Date):        [TIME]',
      ] },
    ],
  },
  {
    id: 'placeholders', title: 'Dynamic Placeholders', rail: 'Placeholders', icon: 'brackets-square', eyebrow: stage(STAGES, 1),
    intro: 'Use placeholders in response field values to generate dynamic content at runtime instead of static values.',
    blocks: [
      { t: 'custom', data: { kind: 'phcards', items: [
        { tok: '[SV]', tone: 'b', name: 'Source Value', desc: 'Copies data from the corresponding field in the request message.', ex: [['Field 3: [SV]', 'Processing Code'], ['Field 11: [SV]', 'STAN'], ['Field 37: [SV]', 'RRN']], note: 'Useful for response messages where you need to echo back values from the original request.' },
        { tok: '[TIME]', tone: 't', name: 'Current Time', desc: 'Generates a current timestamp based on the field’s expected length.', ex: [['Field 12 (6): [TIME]', '143052 (HHmmss)'], ['Field 13 (4): [TIME]', '1204 (MMdd)'], ['Field 7 (10): [TIME]', '1204143052']], note: 'Automatically formats the current time according to ISO8583 field specifications.' },
        { tok: '[RAND]', tone: 'o', name: 'Random Number', desc: 'Generates a random numeric value based on the field’s maximum length.', ex: [['Field 11 (6): [RAND]', '123456'], ['Field 37 (12): [RAND]', '987654321098'], ['Field 38 (6): [RAND]', '456789']], note: 'Generates unique values for fields like STAN, RRN or authorization codes during testing.' },
      ] } },
      { t: 'note', tone: 'blue', icon: 'info', title: 'Note', x: 'Placeholders can be combined with static text. Any field value that does not contain a placeholder is sent as a literal static value.' },
    ],
  },
  {
    id: 'rest-matching', title: 'REST API Matching', rail: 'REST API matching', icon: 'globe', eyebrow: stage(STAGES, 1),
    intro: 'When using REST connections, the simulator provides advanced request matching beyond MTI/processing code.',
    blocks: [
      { t: 'h3', x: 'Match Criteria' },
      { t: 'bullets', items: [
        ['Path', 'Match the URL path exactly or use `*` as a wildcard.'],
        ['HTTP Method', 'Match by GET, POST, PUT, DELETE, etc.'],
        ['Query Parameters', 'Match by URL query parameters.'],
        ['Headers', 'Match by HTTP request headers.'],
        ['Body Fields', 'Match by fields in the request body (JSON/XML).'],
      ] },
      { t: 'h3', x: 'Match Operators' },
      { t: 'p', x: 'Each matcher supports these comparison operators:' },
      { t: 'table', head: ['Operator', 'Description'], cols: '170px minmax(0,1fr)', rows: [
        ['`EQUALS`', 'Exact string match.'],
        ['`NOT_EQUALS`', 'Value does not equal the specified string.'],
        ['`STARTS_WITH`', 'Value starts with the specified prefix.'],
        ['`ENDS_WITH`', 'Value ends with the specified suffix.'],
        ['`CONTAINS`', 'Value contains the specified substring.'],
        ['`REGEX`', 'Value matches a regular expression pattern.'],
        ['`GREATER_THAN`', 'Numeric comparison (value > threshold).'],
        ['`LESS_THAN`', 'Numeric comparison (value < threshold).'],
      ] },
      { t: 'h3', x: 'Response Mapping' },
      { t: 'p', x: 'For REST responses, use **Response Mapping** to define which fields appear in the response body and headers:' },
      { t: 'bullets', items: [
        ['targetKey', 'JSON/XML key in the response body.'],
        ['value', 'Static value or placeholder (`[SV]`, `[TIME]`, `[RAND]`).'],
        ['targetHeader', 'Set a response HTTP header instead of a body field.'],
      ] },
    ],
  },
  {
    id: 'iso8583-template', title: 'ISO8583 Template Tab', rail: 'ISO8583 Template', icon: 'code', eyebrow: stage(STAGES, 1),
    intro: 'The **ISO8583 Template** tab defines the message specification used for parsing and building messages.',
    blocks: [
      screen('template', 'Source Bit Templates listing each bit with its format type, length type, max length and description, beside the source advanced options and message format panels'),
      { t: 'h3', x: 'Top-bar actions' },
      { t: 'bullets', items: [
        ['Save', 'Persist template changes. Send Message and other tabs re-sync immediately.'],
        ['Upload YAML', 'Import a YAML template (the fastest way to bootstrap a known message spec).'],
        ['Download Template', 'Export the current template to YAML.'],
      ] },
      { t: 'h3', x: 'Per-Bit Editor' },
      { t: 'p', x: 'Click any bit number to edit it in a side dialog with three required attributes:' },
      { t: 'bullets', items: [
        ['Bit Length', 'Maximum length (in bytes or characters depending on type).'],
        ['Bit Type', 'Field type (numeric, alphanumeric, binary, etc.).'],
        ['Max Length', 'Used together with the length type (fixed, LLVAR, LLLVAR, LLLLVAR).'],
      ] },
      { t: 'p', x: 'Add or remove bits with the **Add** / **Delete** buttons.' },
      { t: 'h3', x: 'Template-level Options (toggles)' },
      { t: 'bullets', items: [
        ['Iso8583 use Ascii', 'Treat numeric fields as ASCII rather than packed BCD.'],
        ['Don’t use TPDU Header', 'Skip the 5-byte TPDU prefix.'],
        ['Respond same message if unrecognized', 'Echo unmatched requests instead of returning an error.'],
        ['Metfone message', 'Enable Metfone-specific framing.'],
        ['Not update screen', 'Suppress UI updates for high-throughput tests.'],
      ] },
      { t: 'note', tone: 'warn', icon: 'warning', title: 'Important', x: 'The template must match the message specification used by the connecting client. Mismatched templates will cause parsing errors. Ensure field lengths, formats, and encoding match your ISO8583 specification.' },
    ],
  },
  {
    id: 'unsolicited', title: 'Unsolicited Messages', rail: 'Unsolicited messages', icon: 'envelope-open', eyebrow: stage(STAGES, 1),
    intro: 'Unsolicited messages are messages sent outside the normal request-response flow, typically for network management, notifications, or terminal updates.',
    blocks: [
      { t: 'h3', x: 'Send Message (Client / Proxy)' },
      { t: 'p', x: 'Craft and send outbound unsolicited messages to connected hosts. Available in **Client** and **Proxy** modes. Use the message builder to construct ISO8583 messages with specific fields and send them on demand.' },
      { t: 'h3', x: 'Receive Unsolicited (Server / Proxy)' },
      { t: 'p', x: 'In **Server** and **Proxy** modes, incoming unsolicited messages from clients are displayed in the Unsolicited Message tab. Messages are parsed using the active ISO8583 template.' },
    ],
  },
  {
    id: 'security', title: 'Security Options', rail: 'Security options', icon: 'shield-check', eyebrow: stage(STAGES, 2),
    intro: 'The simulator supports several security features for testing encrypted and authenticated connections.',
    blocks: [
      { t: 'h3', x: 'Cipher Types' },
      { t: 'table', head: ['Algorithm', 'Key Sizes'], cols: '200px minmax(0,1fr)', rows: [
        ['DES', '56-bit'],
        ['Triple DES (3DES)', '112/168-bit'],
        ['AES', '128, 192, 256-bit'],
        ['RSA', 'Variable'],
      ] },
      { t: 'h3', x: 'Cipher Modes' },
      { t: 'p', x: 'ECB, CBC, CFB, OFB, and CTS modes are available for block cipher operations.' },
      { t: 'h3', x: 'Authentication' },
      { t: 'bullets', items: [
        ['None', 'No authentication required.'],
        ['Single Password', 'One shared password for all clients.'],
        ['Client Password', 'Per-client authentication credentials.'],
      ] },
      { t: 'h3', x: 'SSL/TLS' },
      { t: 'p', x: 'Enable SSL for both the server listener and REST API client connections for encrypted transport.' },
    ],
  },
  {
    id: 'tabs-reference', title: 'Tabs Reference', rail: 'Tabs reference', icon: 'layout', eyebrow: stage(STAGES, 2),
    intro: 'The runtime simulator screen exposes a different subset of tabs depending on the gateway type.',
    blocks: [
      { t: 'table', head: ['Tab', 'Server', 'Client', 'Proxy', 'Description'], cols: '150px 56px 56px 56px minmax(0,1fr)', rows: [
        ['**ISO8583 Transaction**', '✓', '—', '✓', 'Start / stop, live request / response view, hold message control.'],
        ['**Logs**', '✓', '✓', '✓', 'Transaction and connection logs, connection count, bytes in / out.'],
        ['**Settings**', '✓', '—', '✓', 'Transaction list with Fields / Config / API tabs for each transaction.'],
        ['**ISO8583 Template**', '✓', '✓', '✓', 'Message specification, bit definitions, YAML import / export, template-level toggles.'],
        ['**Send Message**', '✓', '✓', '—', 'Craft and send outbound ISO 8583 messages; inspect responses.'],
        ['**Load Test**', '—', '✓', '—', 'Concurrency / throughput testing for client-mode integrations.'],
        ['**Unsolicited Message**', '✓', '✓', '✓', 'View incoming unsolicited messages from connected peers.'],
      ] },
    ],
  },
];

export const HOST_SIMULATOR_GUIDE: SimGuideData<HostScreenId> = {
  slug: 'host-simulator',
  meta: 'Server · Client · Proxy · TCP/IP · REST · RS232',
  title: 'Host Simulator',
  lede: 'Simulate acquirer and issuer host responses for payment terminals, ATMs, and client applications. Test your ISO8583 integrations without a production host.',
  clips: ['host'],
  poster: 'host',
  browse: 'Browse the tabs',
  intro: {
    eyebrow: 'host-simulator · Hitachi - 1 · Server gateway',
    paras: [
      'The Host Simulator in ISO8583Studio acts as a payment host simulator for development and testing. It accepts incoming connections from POS terminals, ATMs, or other financial clients and returns configurable responses based on matching rules. This eliminates the need for a real host environment during development and QA testing.',
    ],
    screen: { kind: 'screen', id: 'txn', caption: 'running as Hitachi - 1 on 0.0.0.0:8080 — a formatted 0200 request and 0210 response beside their raw hex' },
    features: simFeatures([
      ['desktop', 'Multi-Mode Gateway', 'Run as Server, Client, or Proxy with support for synchronous and asynchronous transmission.'],
      ['plugs-connected', 'Multiple Protocols', 'TCP/IP, REST/HTTP, RS232 serial, and dial-up connections all supported out of the box.'],
      ['list-checks', 'Configurable Rules', 'Match requests by MTI, processing code, REST paths, headers, and return dynamic responses.'],
      ['arrows-clockwise', 'Format Support', 'Handle ISO8583 binary, JSON, XML, HEX, and plain text message formats with conversion.'],
      ['envelope-open', 'Unsolicited Messages', 'Send and receive unsolicited messages for network management and terminal notifications.'],
      ['pulse', 'Live Monitoring', 'Real-time transaction monitoring with request/response inspection and hex dump views.'],
    ]),
  },
  cards: { heading: 'Runtime tabs', rail: 'Runtime tabs', lede: 'The simulator screen exposes a different subset of tabs depending on the gateway type — each card links to the section that documents it.', more: 'Read the section', items: CARDS },
  sections: SECTIONS,
  cta: { heading: 'Run it against your own host', text: 'Free and open source. Download the studio, start a Server gateway and point your terminal at 0.0.0.0:8080 in minutes.' },
};
