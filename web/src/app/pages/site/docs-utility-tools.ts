import { ChangeDetectionStrategy, Component } from '@angular/core';
import { SitePage } from './site-page';
import { ToolGuide } from '../shared/glass/tool-guide';
import { DATA_CONVERTERS_GUIDE } from '../data/data-converters.data';

/**
 * /tools/utility-tools — Data Converters.
 *
 * A tool guide: the page is drawn by ToolGuide (shared/glass/tool-guide.ts)
 * from the data module, which also holds the section ids the page has always
 * had.
 */
@Component({
  selector: 'page-docs-utility-tools',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ToolGuide],
  hostDirectives: [SitePage],
  host: { class: 'static-page page-docs-utility-tools' },
  template: `<app-tool-guide [guide]="guide" />`,
})
export class DocsUtilityToolsPage {
  protected readonly guide = DATA_CONVERTERS_GUIDE;
}
