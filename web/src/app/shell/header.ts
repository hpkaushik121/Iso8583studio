import {
  ChangeDetectionStrategy, Component, DOCUMENT, ElementRef, HostListener, OnDestroy,
  PLATFORM_ID, afterNextRender, inject, signal,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { EXTERNAL, NAV_GROUPS } from '../core/site-nav';
import { MobileMenu } from './mobile-menu';
import { UiLogoMark } from '../ui/logo-mark';
import { UiIcon } from '../ui/icon';

/** Scroll distance after which the bar takes its blurred backing. */
const LIFT_AT = 64;

/**
 * Site header. Transparent over the top of the page, then a blurred bar once
 * the reader scrolls. The stylesheet opens the menus on hover and this class
 * opens them on click and keyboard.
 *
 * Several class names here are what analytics reports clicks by and must
 * survive any restyle: header.nav-bar, .nav-item, button.nav-a, a.pro-pill,
 * button.ham. "Download Studio" is an anchor to the release page on purpose —
 * the Pro interstitial and the download events both key on that href.
 */
@Component({
  selector: 'app-header',
  imports: [RouterLink, RouterLinkActive, MobileMenu, UiLogoMark, UiIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="nav-bar ds-drop" [class.is-lifted]="lifted()" [class.has-menu]="openMenu() !== null">
      <div class="nav-in">
        <a class="brand" routerLink="/">
          <ui-logo-mark [size]="24" />ISO8583Studio
        </a>

        <nav class="nav-links lit-capsule" [class.is-unlit]="lifted()" aria-label="Main">
          @for (group of groups; track group.label) {
            <div class="nav-item" (mouseleave)="hoverOff.set(false)">
              <button class="nav-a" type="button"
                      [attr.aria-expanded]="openMenu() === group.label"
                      (click)="toggle(group.label)">
                {{ group.label }}<ui-icon name="caret-down" [size]="12" />
              </button>
              <div class="menu" [class.mega]="group.mega" [class.open]="openMenu() === group.label"
                   [class.hover-off]="hoverOff()">
                <div class="menu-panel">
                  @for (item of group.items; track item.link) {
                    <a [routerLink]="item.link" [class]="'menu-item menu-item--' + tone(item.chip)"
                       (click)="closeAll()">
                      <span class="menu-tile" aria-hidden="true"><ui-icon [name]="item.icon!" [size]="16" /></span>
                      <span class="menu-text">
                        <b>{{ item.label }}@if (item.chip) {<span class="visually-hidden"> {{ item.chip }}</span>}</b>
                        <span class="mi-sub">{{ item.desc }}</span>
                      </span>
                      <ui-icon class="menu-arrow" name="arrow-right" [size]="14" />
                    </a>
                  }
                </div>
              </div>
            </div>
          }
          <div class="nav-item"><a class="nav-a" routerLink="/docs" routerLinkActive="active">Docs</a></div>
          <div class="nav-item"><a class="nav-a" routerLink="/blogs" routerLinkActive="active">Blog</a></div>
        </nav>

        <div class="nav-right">
          <a class="pro-pill" routerLink="/pro" routerLinkActive="active" title="ISO8583Studio Pro">Pro</a>
          <a class="btn btn--primary nav-download" [href]="external.releases">
            Download Studio<ui-icon name="arrow-up-right" [size]="16" />
          </a>
          <button class="ham icon-btn" type="button" aria-label="Open menu" aria-controls="m-menu"
                  [attr.aria-expanded]="mobileOpen()" (click)="toggleMobile()">
            <ui-icon name="list" [size]="18" />
          </button>
        </div>
      </div>
      <span class="nav-shine" aria-hidden="true"></span>
    </header>
    <app-mobile-menu [open]="mobileOpen()" (navigate)="mobileOpen.set(false)" />
  `,
})
export class Header implements OnDestroy {
  protected readonly groups = NAV_GROUPS;
  protected readonly external = EXTERNAL;
  protected readonly openMenu = signal<string | null>(null);
  protected readonly mobileOpen = signal(false);
  protected readonly lifted = signal(false);
  /** Suppresses the CSS hover-open after a menu item is picked — without it
   *  the menu stays visible while the pointer is still parked on it. Cleared
   *  when the pointer leaves the nav item, so hovering works again. */
  protected readonly hoverOff = signal(false);

  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly doc = inject(DOCUMENT);
  private stopScroll: (() => void) | null = null;

  constructor() {
    if (!isPlatformBrowser(inject(PLATFORM_ID))) return;
    afterNextRender(() => {
      const view = this.doc.defaultView;
      if (!view) return;
      const onScroll = () => this.lifted.set(view.scrollY > LIFT_AT);
      onScroll();
      view.addEventListener('scroll', onScroll, { passive: true });
      this.stopScroll = () => view.removeEventListener('scroll', onScroll);
    });
  }

  ngOnDestroy(): void { this.stopScroll?.(); }

  /** The status chip decides the tile colour: live, in beta, or still in development. */
  protected tone(chip: string | undefined): 'live' | 'beta' | 'dev' {
    return chip === 'Available' ? 'live' : chip === 'Dev' ? 'dev' : 'beta';
  }

  protected toggle(label: string): void {
    this.openMenu.update((cur) => (cur === label ? null : label));
  }

  protected closeAll(): void {
    this.openMenu.set(null);
    this.mobileOpen.set(false);
    this.hoverOff.set(true);
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
