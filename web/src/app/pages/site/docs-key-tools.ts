import { ChangeDetectionStrategy, Component } from '@angular/core';
import { SitePage } from './site-page';
import { ToolGuide } from '../shared/glass/tool-guide';
import { KEY_TOOLS_GUIDE } from '../data/key-tools.data';

/**
 * /tools/key-tools — Key Management Tools.
 *
 * A tool guide: the page is drawn by ToolGuide (shared/glass/tool-guide.ts)
 * from the data module, which also holds the section ids the page has always
 * had.
 */
@Component({
  selector: 'page-docs-key-tools',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ToolGuide],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-key-tools' },
  template: `<app-tool-guide [guide]="guide" />`,
})
export class DocsKeyToolsPage {
  protected readonly guide = KEY_TOOLS_GUIDE;
}
