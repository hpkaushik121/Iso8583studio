import {
  ChangeDetectionStrategy, Component, DOCUMENT, ElementRef, HostListener,
  inject, signal,
} from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { NAV_GROUPS } from '../core/site-nav';
import { MobileMenu } from './mobile-menu';

/**
 * Site header. The stylesheet opens the mega-menus on hover, and this class
 * opens them on click/keyboard — the previous CSS-only implementation was
 * hover-exclusive, so none of the menus could be reached from a keyboard.
 */
@Component({
  selector: 'app-header',
  imports: [RouterLink, RouterLinkActive, MobileMenu],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="nav-bar">
      <div class="nav-in">
        <a class="brand" routerLink="/">
          <img src="/images/app.png" alt="" width="27" height="27">ISO8583Studio
        </a>

        <nav class="nav-links" aria-label="Main">
          @for (group of groups; track group.label) {
            <div class="nav-item">
              <button class="nav-a" type="button"
                      [attr.aria-expanded]="openMenu() === group.label"
                      (click)="toggle(group.label)">
                {{ group.label }}
                <svg width="10" height="6" viewBox="0 0 10 6" fill="none" aria-hidden="true">
                  <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                </svg>
              </button>
              <div class="menu" [class.mega]="group.mega" [class.open]="openMenu() === group.label">
                @for (item of group.items; track item.link) {
                  <a [routerLink]="item.link" (click)="closeAll()">
                    <span class="icon-tile" aria-hidden="true">{{ item.glyph }}</span>
                    <span>
                      <b>{{ item.label }} @if (item.chip) { <span class="cnt">{{ item.chip }}</span> }</b>
                      <span class="mi-sub">{{ item.desc }}</span>
                    </span>
                  </a>
                }
              </div>
            </div>
          }
          <div class="nav-item"><a class="nav-a" routerLink="/docs" routerLinkActive="active">Docs</a></div>
          <div class="nav-item"><a class="nav-a" routerLink="/blogs" routerLinkActive="active">Blog</a></div>
        </nav>

        <div class="nav-right">
          <a class="pro-pill" routerLink="/pro" title="ISO8583Studio Pro">✦ Pro</a>
          <button class="ham" type="button" aria-label="Menu" aria-controls="m-menu"
                  [attr.aria-expanded]="mobileOpen()" (click)="toggleMobile()">☰</button>
        </div>
      </div>
    </header>
    <app-mobile-menu [open]="mobileOpen()" (navigate)="mobileOpen.set(false)" />
  `,
})
export class Header {
  protected readonly groups = NAV_GROUPS;
  protected readonly openMenu = signal<string | null>(null);
  protected readonly mobileOpen = signal(false);

  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly doc = inject(DOCUMENT);

  protected toggle(label: string): void {
    this.openMenu.update((cur) => (cur === label ? null : label));
  }

  protected closeAll(): void {
    this.openMenu.set(null);
    this.mobileOpen.set(false);
  }

  protected toggleMobile(): void {
    this.mobileOpen.update((v) => !v);
    this.openMenu.set(null);
  }

  @HostListener('document:keydown.escape')
  protected onEscape(): void {
    if (this.openMenu() || this.mobileOpen()) {
      this.closeAll();
      this.doc.querySelector<HTMLElement>('.nav-a[aria-expanded="true"]')?.focus();
    }
  }

  @HostListener('document:click', ['$event'])
  protected onDocumentClick(event: MouseEvent): void {
    if (!this.host.nativeElement.contains(event.target as Node)) this.closeAll();
  }
}
