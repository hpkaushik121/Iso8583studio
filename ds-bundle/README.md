# ISO8583Studio — Design Tokens

A **tokens-only** design system. ISO8583Studio is a Kotlin **Compose Desktop** application, so
there are no React components to ship: `_ds_bundle.js` is intentionally empty-bodied and the
value here is the token set exported as CSS custom properties.

## Source of truth

Everything is generated from a single file:

```
composeApp/src/desktopMain/kotlin/in/aicortex/iso8583studio/ui/AppTheme.kt
```

If you change a colour, type style or corner radius there, regenerate rather than hand-editing
anything in this bundle.

## Contents

| Path | What |
|---|---|
| `styles.css` | Entry stylesheet — `@import`s the three token files |
| `tokens/colors.css` | 19 opaque + 4 translucent colours, plus Material light/dark role mapping |
| `tokens/typography.css` | 10-step Material type scale + `.iso-<style>` utility classes |
| `tokens/shapes.css` | 3 corner radii |
| `tokens/tokens.json` | Machine-readable form of all of the above |
| `components/foundations/*` | Preview cards: Colors, Typography, Shapes |

## Using the tokens

```css
.panel {
  background: var(--iso-surface);
  color: var(--iso-on-surface);
  border: 1px solid var(--iso-border);
  border-radius: var(--iso-radius-medium);
}
```

Theme roles resolve per mode. `:root` defaults to light; set `data-theme="dark"` on an
ancestor to switch:

```html
<div data-theme="dark"> … </div>
```

## Notes on fidelity

- **Compose `sp` → CSS `px`.** At 1× density they are equivalent. Compose scales `sp` with the
  OS font-size setting; CSS `px` does not. Use `rem` if you need that behaviour on the web.
- **`secondaryVariant` is absent in dark mode.** `AppTheme.kt`'s `darkColors(...)` never sets it,
  so it falls through to the Material default. That gap is reproduced here rather than papered over.
- **Glass tokens are Windows 11 only.** `BackgroundLightGlass` / `CardDarkGlass` etc. apply solely
  when the Mica/Acrylic backdrop is active. Everywhere else the opaque palette is used.
- **Excluded:** `Purple80/40`, `PurpleGrey80/40`, `Pink80/40` — Material 3 template leftovers in
  `AppTheme.kt` that no palette references and no source file outside that file uses.
