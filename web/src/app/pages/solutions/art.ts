import { Directive, booleanAttribute, input } from '@angular/core';

/** The rendered 3D pieces under /media/art, each published at 640 and 1024 px (square). */
export type ArtName =
  | 'acquirer' | 'assessment-magnifier' | 'base' | 'bridge' | 'chip' | 'cloud' | 'core'
  | 'driver' | 'glass-plate' | 'hsm' | 'hub' | 'issuer' | 'kernel-board' | 'nfc' | 'server'
  | 'shield' | 'stack' | 'submission-folder' | 'terminal' | 'test-document';

/**
 * One piece of solution-page art: `<img solArt="chip" sizes="160px">`.
 *
 * Fills in everything but `sizes`, which only the caller knows. Images are
 * lazy by default. `eager` is for pieces that sit above the fold on a desktop
 * screen; `priority` is for the single largest piece of a hero and is the only
 * thing that asks the browser to fetch ahead of the queue.
 */
@Directive({
  selector: 'img[solArt]',
  host: {
    alt: '',
    decoding: 'async',
    width: '1024',
    height: '1024',
    '[attr.loading]': "eager() || priority() ? null : 'lazy'",
    '[attr.fetchpriority]': "priority() ? 'high' : null",
    '[attr.srcset]': "'/media/art/' + name() + '-640.webp 640w, /media/art/' + name() + '-1024.webp 1024w'",
    '[attr.src]': "'/media/art/' + name() + '-640.webp'",
  },
})
export class SolArt {
  readonly name = input.required<ArtName>({ alias: 'solArt' });
  readonly eager = input(false, { transform: booleanAttribute });
  readonly priority = input(false, { transform: booleanAttribute });
}
