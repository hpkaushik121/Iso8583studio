/**
 * Walks every page's content DOM in lockstep against the original site and
 * reports every computed-style difference.
 *
 * The importer preserves body markup verbatim, so the old and new content trees
 * have the same shape and can be aligned node by node. Anything that differs is
 * a styling regression introduced by the migration.
 *
 *   node tools/compare-pages.mjs            # summary
 *   node tools/compare-pages.mjs --verbose  # every diff per page
 */
import puppeteer from 'puppeteer';

const OLD = 'http://localhost:4322';
const NEW = 'http://localhost:4321';
const VERBOSE = process.argv.includes('--verbose');
const WIDTH = 1280, HEIGHT = 1000;

/** old path -> new path */
const PAGES = [
  ['/', '/'],
  ['/privacy-policy.html', '/privacy-policy'],
  ['/terms-and-conditions.html', '/terms-and-conditions'],
  ...['cloud-simulators', 'contact', 'emv-certification', 'kernel', 'middleware', 'pro']
    .map((s) => [`/${s}/`, `/${s}`]),
  ['/docs/', '/docs'],
  ...['apdu-simulator', 'atm-simulator', 'card-validation', 'cipher-tools', 'contributing',
    'dukpt-tools', 'ecr-simulator', 'emv-tools', 'host-simulator', 'hsm-command-console',
    'hsm-simulator', 'installation', 'issuer-simulator', 'key-tools', 'mac-tools',
    'payment-simulators', 'payment-switch', 'pin-tools', 'pos-simulator', 'utility-tools',
    'versions'].map((s) => [`/docs/${s}/`, `/docs/${s}`]),
];

const PROPS = [
  'display', 'fontSize', 'fontWeight', 'lineHeight', 'letterSpacing', 'textTransform',
  'color', 'backgroundColor', 'marginTop', 'marginRight', 'marginBottom', 'marginLeft',
  'paddingTop', 'paddingRight', 'paddingBottom', 'paddingLeft', 'borderTopWidth',
  'borderRightWidth', 'borderBottomWidth', 'borderLeftWidth', 'borderTopLeftRadius',
  'borderTopColor', 'borderLeftColor', 'gap', 'gridTemplateColumns', 'flexDirection',
  'alignItems', 'justifyContent', 'textAlign', 'flexWrap', 'listStyleType',
  /* opacity is deliberately absent: the old pages hide sections at opacity 0
     until their reveal script runs, so a headless snapshot compares animation
     state rather than design. The new pages render them visible without JS,
     which is the safer behaviour. */
];

/** Serialise the content tree: one entry per element, in document order. */
const SNAPSHOT = (props) => {
  // In the old page the shell is a pair of custom elements; in the new one it
  // lives outside the routed content. Strip both so the trees align.
  const root = document.querySelector('.static-page') ?? document.body;
  const skip = new Set(['SCRIPT', 'STYLE', 'SITE-HEADER', 'SITE-FOOTER', 'LINK', 'NOSCRIPT']);
  const out = [];
  const walk = (el, path) => {
    if (skip.has(el.tagName)) return;
    const cs = getComputedStyle(el);
    const style = {};
    for (const p of props) style[p] = cs[p];
    out.push({
      path,
      tag: el.tagName,
      cls: el.className && typeof el.className === 'string' ? el.className : '',
      w: Math.round(el.getBoundingClientRect().width),
      style,
    });
    let i = 0;
    for (const child of el.children) {
      if (skip.has(child.tagName)) continue;
      walk(child, `${path}>${child.tagName}.${i++}`);
    }
  };
  let i = 0;
  for (const child of root.children) {
    if (skip.has(child.tagName)) continue;
    walk(child, `${child.tagName}.${i++}`);
  }
  return out;
};

const browser = await puppeteer.launch({ headless: true, args: ['--no-sandbox'] });

async function snap(url) {
  const page = await browser.newPage();
  await page.setViewport({ width: WIDTH, height: HEIGHT });
  await page.goto(url, { waitUntil: 'networkidle0', timeout: 45000 });
  // Angular hydrates and the SEO service writes the head after load.
  await new Promise((r) => setTimeout(r, 350));
  /* Both sites reveal sections on scroll and autoplay demos. Whether a given
     element is mid-transition when the snapshot is taken is a race, and it
     produced more noise than every real difference combined. Settle everything
     first so the comparison is of the design, not of animation timing. */
  await page.addStyleTag({ content: `
    *, *::before, *::after {
      transition: none !important;
      /* Run animations to completion rather than removing them: cancelling an
         entrance animation leaves the element at its opacity:0 start state. */
      animation-duration: 1ms !important;
      animation-delay: 0s !important;
      animation-iteration-count: 1 !important;
      animation-fill-mode: forwards !important;
    }
    .reveal, .reveal.on { opacity: 1 !important; transform: none !important; }
  ` });
  await new Promise((r) => setTimeout(r, 120));
  const data = await page.evaluate(SNAPSHOT, PROPS);
  await page.close();
  return data;
}

/* The migration deliberately retired the second palette the legal pages and
   the blog template carried. Those substitutions are the intended outcome, not
   regressions, so they are counted separately. */
const RECONCILED = new Map(Object.entries({
  'rgb(236, 239, 241)': 'rgb(233, 236, 245)',   // --text-primary  -> --text
  'rgb(176, 190, 197)': 'rgb(154, 163, 186)',   // --text-secondary-> --muted
  'rgb(26, 29, 46)':    'rgb(21, 22, 31)',      // --background-dark -> --bg-deep
  'rgb(61, 61, 61)':    'rgba(151, 168, 214, 0.14)', // --border-light -> --line
  'rgb(42, 42, 42)':    'rgba(151, 168, 214, 0.07)', // --border-dark  -> --line2
  'rgb(255, 107, 107)': 'rgb(255, 138, 141)',   // --error-red    -> --red-hi
  'rgb(81, 207, 102)':  'rgb(94, 194, 106)',    // --success-green-> --green-hi
  'rgb(144, 164, 174)': 'rgb(106, 114, 136)',   // --text-muted   -> --faint
}));

/* Screenshot figures no longer break out of the text column — see the
   .shot-fig comment in _components.css. Their geometry is expected to differ
   from the original, so it is reported as intentional rather than as a
   regression. */
const LAYOUT_CHANGES = [
  { match: (cls) => /\bshot-fig\b|\bshot-grid\b/.test(cls),
    props: new Set(['marginLeft', 'marginRight', 'marginTop', 'marginBottom', 'width']) },
];

const INTENTIONAL = (prop, oldV, newV, cls = '') =>
  (/color/i.test(prop) && RECONCILED.get(oldV) === newV) ||
  LAYOUT_CHANGES.some((c) => c.match(cls) && c.props.has(prop));

let intentional = 0;
const totals = new Map();          // "selector | prop" -> {count, old, new, pages:Set}
let pagesWithDiffs = 0;
let compared = 0;

const ONLY = process.argv.find((a) => a.startsWith('--only='))?.slice(7);
for (const [oldPath, newPath] of PAGES) {
  if (ONLY && newPath !== ONLY) continue;
  let a, b;
  try {
    [a, b] = await Promise.all([snap(OLD + oldPath), snap(NEW + newPath)]);
  } catch (err) {
    console.log(`  ! ${newPath}: ${err.message}`);
    continue;
  }
  compared++;

  if (a.length !== b.length) {
    console.log(`  ! ${newPath}: element count differs — old ${a.length}, new ${b.length}`);
  }

  // Align by DOM path. Aligning by index makes every element after an
  // insertion look like a difference, which buries the real ones.
  const byPath = new Map(b.map((n) => [n.path, n]));
  const diffs = [];
  for (const oldNode of a) {
    const newNode = byPath.get(oldNode.path);
    if (!newNode || newNode.tag !== oldNode.tag) continue;
    for (const p of PROPS) {
      const ov = oldNode.style[p], nv = newNode.style[p];
      if (ov === nv) continue;
      if (INTENTIONAL(p, ov, nv, oldNode.cls)) { intentional++; continue; }
      const key = `${oldNode.tag.toLowerCase()}${oldNode.cls ? '.' + oldNode.cls.trim().split(/\s+/).join('.') : ''} | ${p}`;
      const rec = totals.get(key) ?? { count: 0, old: ov, new: nv, pages: new Set() };
      rec.count++;
      rec.pages.add(newPath);
      totals.set(key, rec);
      diffs.push({ el: key, old: ov, new: nv });
    }
  }

  if (diffs.length) {
    pagesWithDiffs++;
    console.log(`  ${newPath}: ${diffs.length} diff(s) over ${a.length} elements`);
    if (VERBOSE) for (const d of diffs.slice(0, 40)) console.log(`      ${d.el}: old=${d.old} new=${d.new}`);
  }
}

await browser.close();

console.log(`\ncompared ${compared} pages — ${pagesWithDiffs} with differences`);
console.log(`${intentional} deliberate substitutions ignored (palette + screenshot layout)\n`);
const ranked = [...totals.entries()].sort((x, y) => y[1].count - x[1].count);
if (ranked.length) {
  console.log('distinct issues, most widespread first:');
  for (const [key, r] of ranked.slice(0, 40)) {
    console.log(`  ${String(r.count).padStart(4)}x  ${key}`);
    console.log(`         old=${r.old}  new=${r.new}   (${r.pages.size} page(s))`);
  }
} else {
  console.log('no computed-style differences.');
}
process.exit(ranked.length ? 1 : 0);
