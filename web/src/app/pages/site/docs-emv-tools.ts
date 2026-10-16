import { ChangeDetectionStrategy, Component } from '@angular/core';
import { SitePage } from './site-page';
import { ToolGuide } from '../shared/glass/tool-guide';
import { EMV_TOOLS_GUIDE } from '../data/emv-tools.data';

/**
 * /tools/emv-tools — EMV & Card Tools.
 *
 * A tool guide: the page is drawn by ToolGuide (shared/glass/tool-guide.ts)
 * from the data module, which also holds the section ids the page has always
 * had.
 */
@Component({
  selector: 'page-docs-emv-tools',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ToolGuide],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-emv-tools' },
  template: `<app-tool-guide [guide]="guide" />`,
})
export class DocsEmvToolsPage {
  protected readonly guide = EMV_TOOLS_GUIDE;
}
