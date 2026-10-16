import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/**
 * GuideRich — inline copy written with a little markdown, as the guide data
 * files carry it.
 *
 *   <p class="gd-p" [gdRich]="'Tag `9F46`, **required**, see [PIN block](/tools/pin-tools#pin-block)'"></p>
 *
 * Understands `code`, **strong** (which may contain `code`), *emphasis* and
 * [label](href). It is an attribute component, so it fills whatever element
 * you put it on and adds no wrapper of its own; the host gets class `gd-rich`,
 * which is what styles the <code>, <strong> and <a> it renders.
 *
 * Links: write internal hrefs in full ('/tools/pin-tools#translate', never a
 * bare '#translate') — the site has <base href="/"> and the page directive
 * hands any '/…' href to the router.
 *
 * Input: gdRich (string, required).
 */

interface RichNode {
  /** t text · c code · s strong · e emphasis · a link */
  k: 't' | 'c' | 's' | 'e' | 'a';
  x: string;
  href?: string;
  kids?: RichNode[];
}

const TOKENS = /(`[^`]+`|\*\*.+?\*\*|\*[^*]+\*|\[[^\]]+\]\([^)]+\))/g;
const LINK = /^\[([^\]]+)\]\(([^)]+)\)$/;

export function parseRich(text: string, nested = false): RichNode[] {
  const nodes: RichNode[] = [];
  for (const part of text.split(TOKENS)) {
    if (!part) continue;
    if (part.startsWith('**') && part.endsWith('**') && part.length > 4 && !nested) {
      nodes.push({ k: 's', x: '', kids: parseRich(part.slice(2, -2), true) });
    } else if (part.startsWith('`') && part.endsWith('`') && part.length > 2) {
      nodes.push({ k: 'c', x: part.slice(1, -1) });
    } else if (part.startsWith('[') && LINK.test(part)) {
      const m = LINK.exec(part)!;
      nodes.push({ k: 'a', x: m[1], href: m[2] });
    } else if (part.startsWith('*') && part.endsWith('*') && part.length > 2 && !part.startsWith('**')) {
      nodes.push({ k: 'e', x: part.slice(1, -1) });
    } else {
      nodes.push({ k: 't', x: part });
    }
  }
  return nodes;
}

@Component({
  selector: '[gdRich]',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'gd-rich' },
  // One line on purpose: whitespace between the pieces would show up as stray
  // spaces inside the sentence.
  template: `@for (n of nodes(); track $index) {@switch (n.k) {@case ('c') {<code>{{ n.x }}</code>}@case ('e') {<em>{{ n.x }}</em>}@case ('a') {<a [href]="n.href">{{ n.x }}</a>}@case ('s') {<strong>@for (m of n.kids; track $index) {@switch (m.k) {@case ('c') {<code>{{ m.x }}</code>}@case ('e') {<em>{{ m.x }}</em>}@case ('a') {<a [href]="m.href">{{ m.x }}</a>}@default {<ng-container>{{ m.x }}</ng-container>}}}</strong>}@default {<ng-container>{{ n.x }}</ng-container>}}}`,
})
export class GuideRich {
  readonly text = input.required<string>({ alias: 'gdRich' });
  protected readonly nodes = computed(() => parseRich(this.text() ?? ''));
}
