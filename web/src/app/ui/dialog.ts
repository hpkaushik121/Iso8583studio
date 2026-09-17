import {
  AfterViewInit, ChangeDetectionStrategy, Component, DOCUMENT, ElementRef,
  OnDestroy, inject, input, output, viewChild,
} from '@angular/core';

const FOCUSABLE =
  'a[href],button:not([disabled]),textarea,input,select,[tabindex]:not([tabindex="-1"])';

/**
 * Modal dialog. The markup this replaces was a div with role="dialog" and no
 * focus management at all — Tab escaped to the page behind it and the trigger
 * never got focus back.
 */
@Component({
  selector: 'ui-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="dialog-ov" (click)="onBackdrop($event)">
      <div #panel class="dialog" role="dialog" aria-modal="true" [attr.aria-label]="label()"
           (keydown)="onKeydown($event)">
        <button class="dialog-x" type="button" aria-label="Close" (click)="close.emit('button')">×</button>
        <ng-content />
      </div>
    </div>
  `,
})
export class UiDialog implements AfterViewInit, OnDestroy {
  readonly label = input.required<string>();
  readonly close = output<'button' | 'backdrop' | 'escape'>();

  private readonly panel = viewChild.required<ElementRef<HTMLElement>>('panel');
  private readonly doc = inject(DOCUMENT);
  private previouslyFocused: HTMLElement | null = null;

  ngAfterViewInit(): void {
    this.previouslyFocused = this.doc.activeElement as HTMLElement | null;
    this.doc.body.style.overflow = 'hidden';
    // Focus the panel itself rather than the close button, so a screen reader
    // announces the dialog label before its controls.
    const panel = this.panel().nativeElement;
    panel.setAttribute('tabindex', '-1');
    panel.focus();
  }

  ngOnDestroy(): void {
    this.doc.body.style.overflow = '';
    this.previouslyFocused?.focus?.();
  }

  protected onBackdrop(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('dialog-ov')) {
      this.close.emit('backdrop');
    }
  }

  protected onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      event.preventDefault();
      this.close.emit('escape');
      return;
    }
    if (event.key !== 'Tab') return;

    const items = Array.from(
      this.panel().nativeElement.querySelectorAll<HTMLElement>(FOCUSABLE),
    ).filter((el) => el.offsetParent !== null || el === this.doc.activeElement);
    if (!items.length) return;

    const first = items[0];
    const last = items[items.length - 1];
    const active = this.doc.activeElement;

    if (event.shiftKey && (active === first || active === this.panel().nativeElement)) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && active === last) {
      event.preventDefault();
      first.focus();
    }
  }
}
