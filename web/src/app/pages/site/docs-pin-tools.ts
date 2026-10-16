import { ChangeDetectionStrategy, Component } from '@angular/core';
import { SitePage } from './site-page';
import { ToolGuide } from '../shared/glass/tool-guide';
import { PIN_TOOLS_GUIDE } from '../data/pin-tools.data';

/**
 * /tools/pin-tools — Payment Utilities.
 *
 * A tool guide: the page is drawn by ToolGuide (shared/glass/tool-guide.ts)
 * from the data module, which also holds the section ids the page has always
 * had.
 */
@Component({
  selector: 'page-docs-pin-tools',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ToolGuide],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-pin-tools' },
  template: `<app-tool-guide [guide]="guide" />`,
})
export class DocsPinToolsPage {
  protected readonly guide = PIN_TOOLS_GUIDE;
}
