import { ChangeDetectionStrategy, Component } from '@angular/core';
import { SitePage } from './site-page';
import { ToolGuide } from '../shared/glass/tool-guide';
import { CIPHER_TOOLS_GUIDE } from '../data/cipher-tools.data';

/**
 * /tools/cipher-tools — Cryptographic Tools.
 *
 * A tool guide: the page is drawn by ToolGuide (shared/glass/tool-guide.ts)
 * from the data module, which also holds the section ids the page has always
 * had.
 */
@Component({
  selector: 'page-docs-cipher-tools',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ToolGuide],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-cipher-tools' },
  template: `<app-tool-guide [guide]="guide" />`,
})
export class DocsCipherToolsPage {
  protected readonly guide = CIPHER_TOOLS_GUIDE;
}
