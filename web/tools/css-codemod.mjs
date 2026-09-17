/**
 * Normalises CSS lifted out of the imported pages so it cannot reintroduce the
 * palettes the design system just replaced.
 *
 *  1. drops :root blocks — tokens have exactly one home now;
 *  2. renames the retired token vocabularies onto the surviving one;
 *  3. maps palette hex literals back to their token;
 *  4. drops rules the design system already owns, which is most of what the
 *     home page duplicated inline from site.css.
 */

/** Retired names -> surviving token. Longest first: --text-primary-light must
 *  be rewritten before --text-primary would match it. */
export const TOKEN_ALIASES = [
  ['--primary-blue-light', '--blue-lo'],
  ['--primary-blue-dark', '--blue'],
  ['--primary-blue', '--blue-hi'],
  ['--accent-teal-light', '--teal-lo'],
  ['--accent-teal-dark', '--teal'],
  ['--accent-teal', '--teal-hi'],
  ['--accent-purple', '--purple'],
  ['--accent-orange', '--amber'],
  ['--background-light', '--surface'],
  ['--background-dark', '--bg-deep'],
  ['--card-light', '--card'],
  ['--card-dark', '--card-deep'],
  ['--error-red', '--red-hi'],
  ['--success-green', '--green-hi'],
  ['--warning-yellow', '--yellow'],
  ['--border-light', '--line'],
  ['--border-dark', '--line2'],
  ['--text-primary-light', '--text'],
  ['--text-secondary-light', '--muted'],
  ['--text-secondary', '--muted'],
  ['--text-primary', '--text'],
  ['--text-muted', '--faint'],
];

const HEX_TO_TOKEN = new Map(Object.entries({
  '#15161f': '--bg-deep', '#1b1d27': '--bg', '#252836': '--surface',
  '#2f3142': '--card', '#383b50': '--card-hi', '#1e1f2e': '--card-deep',
  '#1a1d2e': '--bg-deep',
  '#e9ecf5': '--text', '#eceff1': '--text', '#9aa3ba': '--muted',
  '#b0bec5': '--muted', '#6a7288': '--faint',
  '#1e88e5': '--blue', '#6ab7ff': '--blue-hi', '#9fd3ff': '--blue-lo',
  '#005cb2': '--blue-deep', '#26a69a': '--teal', '#64d8cb': '--teal-hi',
  '#a0f4ea': '--teal-lo', '#43a047': '--green', '#5ec26a': '--green-hi',
  '#51cf66': '--green-hi', '#ff9800': '--amber', '#ffb74d': '--amber-hi',
  '#ffd43b': '--yellow', '#e5484d': '--red', '#ff8a8d': '--red-hi',
  '#ff6b6b': '--red-hi', '#9c27b0': '--purple',
}));

/** Splits a stylesheet into top-level rules, keeping at-rules intact. */
function topLevelRules(css) {
  const rules = [];
  let depth = 0, start = 0;
  for (let i = 0; i < css.length; i++) {
    const ch = css[i];
    if (ch === '{') depth++;
    else if (ch === '}') {
      depth--;
      if (depth === 0) { rules.push(css.slice(start, i + 1)); start = i + 1; }
    }
  }
  if (start < css.length && css.slice(start).trim()) rules.push(css.slice(start));
  return rules;
}

const classesIn = (selector) => selector.split(',')
  .flatMap((part) => [...part.matchAll(/\.([a-zA-Z][\w-]*)/g)].map((m) => m[1]));

/** A rule is redundant when every class it targets is already defined by the
 *  design system and it introduces no page-specific class of its own. */
function ownedBySystem(selector, owned) {
  // Document-level rules always belong to _base.css. This has to be checked
  // before the class test below, because these selectors carry no class at all.
  if (/(^|[\s,>+~])(html|body|\*)(\b|$)/.test(selector)) return true;
  const classes = classesIn(selector);
  if (!classes.length) return false;
  return classes.every((c) => owned.has(c));
}

/** Prefixes every selector in a list so the rule cannot escape its page. */
function scopeSelector(selectorList, scope) {
  return selectorList.split(',').map((sel) => {
    const s = sel.trim();
    if (!s) return s;
    // A rule already targeting the wrapper must not be nested inside itself.
    return s.startsWith(scope) ? s : `${scope} ${s}`;
  }).join(', ');
}

export function normalise(css, owned, scope, systemTokens = new Set()) {
  // 0. strip comments. The rule splitter slices a selector as "everything since
  //    the last closing brace", so a comment sitting above a rule would be
  //    absorbed into its selector — and, worse, would stop an @media block from
  //    being recognised as an at-rule.
  let out = css.replace(/\/\*[\s\S]*?\*\//g, '');

  /* 1. Take the :root blocks apart rather than deleting them wholesale.
        Most of what they declare is the retired palette, which the aliases
        below rename. But a few are page-local values with no equivalent in the
        design system — solutions.css declares --line-soft, and the legal pages
        declare --text-muted — and the markup uses them from inline style
        attributes. Dropping those silently invalidated the declarations that
        referenced them, collapsing section borders and resetting text colour. */
  const preserved = [];
  out = out.replace(/:root\s*\{([^}]*)\}/g, (_, body) => {
    for (const decl of body.split(';')) {
      const m = decl.match(/^\s*(--[\w-]+)\s*:\s*([\s\S]+)$/);
      if (!m) continue;
      const [, name, value] = m;
      if (systemTokens.has(name)) continue;                            // system owns it
      if (TOKEN_ALIASES.some(([from]) => from === name)) continue;      // retired, renamed below
      preserved.push(`${name}: ${value.trim()}`);
    }
    return '';
  });

  // 2. rename retired tokens
  for (const [from, to] of TOKEN_ALIASES) {
    out = out.replaceAll(new RegExp(`${from}(?![\\w-])`, 'g'), to);
  }

  // 3. hex -> token
  out = out.replace(/#[0-9a-fA-F]{6}\b/g, (hex) => {
    const token = HEX_TO_TOKEN.get(hex.toLowerCase());
    return token ? `var(${token})` : hex;
  });

  /* 4. Scope what is left, and — only for the site-wide stylesheets — drop the
        rules the design system already provides.

        A page's own stylesheet is never stripped this way. docs/emv-tools
        styles .shot-grid, h2 and h3 differently from site.css on purpose, and
        dropping those as "already owned" replaced the page's design with the
        system's. Scoping alone is enough: the rules cannot leak, and their
        extra specificity makes them win on their own page. */
  const kept = [];
  let dropped = 0;
  const isPageScoped = !!scope;

  const takeRule = (rule) => {
    const brace = rule.indexOf('{');
    if (brace === -1) return rule.trim() ? rule : '';
    const selector = rule.slice(0, brace).trim();
    if (!isPageScoped && ownedBySystem(selector, owned)) { dropped++; return ''; }
    return `${scopeSelector(selector, scope)}{${rule.slice(brace + 1)}`;
  };

  for (const rule of topLevelRules(out)) {
    const brace = rule.indexOf('{');
    if (brace === -1) { if (rule.trim()) kept.push(rule); continue; }
    const selector = rule.slice(0, brace).trim();

    if (selector.startsWith('@media') || selector.startsWith('@supports')) {
      const inner = rule.slice(brace + 1, rule.lastIndexOf('}'));
      const innerKept = topLevelRules(inner).map(takeRule).filter(Boolean);
      if (innerKept.length) kept.push(`${selector}{${innerKept.join('')}}`);
      continue;
    }
    // Keyframe steps ('from', 'to', '50%') are not selectors and must not be
    // scoped; @font-face and friends pass through untouched too.
    if (selector.startsWith('@')) { kept.push(rule); continue; }

    const out2 = takeRule(rule);
    if (out2) kept.push(out2);
  }

  /* Re-emit the survivors. Scoped pages hang them off their wrapper class so
     descendants inherit them; the site-wide sheet keeps them on :root. */
  if (preserved.length) {
    const holder = scope || ':root';
    kept.unshift(`${holder}{${preserved.join(';')}}`);
  }

  return { css: kept.join('\n').trim(), dropped };
}

/** Every class the design system defines, read from its own stylesheets. */
export function ownedClasses(sources) {
  const owned = new Set();
  for (const css of sources) {
    const stripped = css.replace(/\/\*[\s\S]*?\*\//g, '');
    for (const m of stripped.matchAll(/([^{}]+)\{/g)) {
      const sel = m[1];
      if (sel.trim().startsWith('@')) continue;
      for (const c of classesIn(sel)) owned.add(c);
    }
  }
  return owned;
}
