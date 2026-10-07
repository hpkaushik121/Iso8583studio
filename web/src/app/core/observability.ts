import { DOCUMENT, Injectable, inject } from '@angular/core';

/** What the New Relic browser agent exposes once it has loaded. */
interface NewRelicApi {
  addPageAction(name: string, attributes?: Record<string, unknown>): void;
  noticeError(error: Error | string, attributes?: Record<string, unknown>): void;
  log(message: string, options?: { level?: string; customAttributes?: Record<string, unknown> }): void;
  setCustomAttribute(key: string, value: string | number | boolean | null): void;
}

/**
 * The thin wrapper over the New Relic browser agent.
 *
 * Separate from AnalyticsService on purpose. That one answers "what are people
 * doing" for GA4 and Google Ads, and its events are a product taxonomy people
 * report on. This one answers "is it working" — whether a submission left,
 * whether anything came back, what failed — and is read while debugging. The
 * two have different audiences and different retention, so they stay apart.
 *
 * Everything is a no-op when the agent is absent: the script is async and
 * cross-origin, so it may not have loaded yet, may be blocked by an extension,
 * or may never be configured at all. Nothing here may throw into a caller —
 * an observability layer that breaks the thing it observes is worse than none.
 *
 * Nothing personal goes through here. Names, email addresses, company names
 * and message bodies are not diagnostics; what is useful is which surface,
 * which outcome and which error code. The same rule as the GA4 layer, for the
 * same reason, and New Relic's own guidance says so too.
 */
@Injectable({ providedIn: 'root' })
export class Observability {
  private readonly doc = inject(DOCUMENT);

  /** The agent, or undefined until it has loaded. Read fresh each time. */
  private get nr(): NewRelicApi | undefined {
    const win = this.doc.defaultView as (Window & { newrelic?: NewRelicApi }) | null;
    return win?.newrelic;
  }

  /**
   * A counted event with dimensions — the thing you chart and alert on.
   *
   * Mirrors how the funnel is already reported to GA4, so the two can be read
   * side by side when a number looks wrong.
   */
  action(name: string, attributes: Record<string, unknown> = {}): void {
    try { this.nr?.addPageAction(name, attributes); } catch { /* never break the caller */ }
  }

  /** A log line, for the detail that would make a chart useless as a dimension. */
  log(message: string, attributes: Record<string, unknown> = {}, level = 'INFO'): void {
    try { this.nr?.log(message, { level, customAttributes: attributes }); } catch { /* as above */ }
  }

  /**
   * Something that failed.
   *
   * Takes a message rather than a caught value by default, because most of
   * what goes wrong here is not an exception — it is a request that answered
   * and said nothing useful.
   */
  error(message: string, attributes: Record<string, unknown> = {}): void {
    try { this.nr?.noticeError(new Error(message), attributes); } catch { /* as above */ }
  }
}
