import { ChangeDetectionStrategy, Component, HostListener, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { UiDialog } from '../ui/dialog';
import { AnalyticsService } from '../core/analytics';

const DOWNLOAD_HREF = /releases\/latest|releases\/download|\.(dmg|exe|msi|deb|rpm|jar|zip)(\?|$)/i;

/**
 * Offers Pro once before any download. Same matching rules as the original
 * site.js interceptor; the dialog itself is now focus-trapped and Esc-closable.
 */
@Component({
  selector: 'app-pro-download-modal',
  imports: [UiDialog, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (pendingHref(); as href) {
      <ui-dialog label="Try ISO8583Studio Pro" (close)="dismiss($event)">
        <span class="pn-tag">✦ Pro</span>
        <h3>Before you download — try Pro</h3>
        <p>The studio is free forever. Pro raises the ceiling — more cryptographic throughput,
           the full algorithm set, deeper simulator tuning, and hosted endpoints your CI can reach.</p>
        <ul class="pm-list">
          <li>Higher CPS — multi-threaded crypto for load &amp; soak tests</li>
          <li>Full algorithm set: RSA, ECC, SHA-3, FPE, AES DUKPT</li>
          <li>Deep tweaks: field overrides, latency &amp; error injection</li>
          <li>Hosted endpoints, scheme test packs, priority support</li>
        </ul>
        <div class="pm-actions">
          <a class="btn btn--primary" routerLink="/pro" (click)="close()">Register for Pro — from ₹2</a>
          <a class="pm-skip" [href]="href" (click)="close()">Just download the free studio →</a>
        </div>
      </ui-dialog>
    }
  `,
})
export class ProDownloadModal {
  protected readonly pendingHref = signal<string | null>(null);
  private readonly analytics = inject(AnalyticsService);

  /** Capture phase, so the interception happens before any other handler. */
  @HostListener('document:click', ['$event'])
  protected onClick(event: MouseEvent): void {
    if (this.pendingHref()) return;
    // Paid clicks were promised a download, not an upsell — the interstitial
    // is what the landing-page-experience signal punishes hardest.
    if (this.analytics.paidSession()) return;
    const anchor = (event.target as Element | null)?.closest?.('a');
    if (!anchor) return;

    const href = anchor.getAttribute('href') || '';
    const isDownload =
      DOWNLOAD_HREF.test(href) ||
      (/^\s*(⬇\s*)?download\b/i.test(anchor.textContent || '') &&
        /github\.com\/hpkaushik121/i.test(href));

    // Links inside the dialog are the escape hatch, never a new trigger.
    if (!isDownload || anchor.closest('.dialog')) return;

    event.preventDefault();
    this.pendingHref.set(href);
    this.analytics.reportProModalShown(href);
  }

  protected close(): void { this.pendingHref.set(null); }

  protected dismiss(reason: 'button' | 'backdrop' | 'escape'): void {
    if (reason === 'backdrop') this.analytics.reportProModalDismissed();
    this.close();
  }
}
