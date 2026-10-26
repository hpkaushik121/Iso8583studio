# Bundled fonts

The browser tools draw on a canvas, so CSS fonts never reach them; IBM Plex Sans and IBM Plex
Mono (SIL Open Font License 1.1, see OFL.txt) are bundled as Compose resources in
`src/wasmJsMain/composeResources/font/`.

Source: https://github.com/IBM/plex (v6.4.1), `*/fonts/complete/ttf/*.ttf`. The files are
subset to Latin-1, common punctuation and arrows to keep the bundle small:

```bash
pyftsubset IBMPlexSans-Regular.ttf --unicodes="U+0020-007E,U+00A0-00FF,U+2013-2014,U+2018-201D,U+2022,U+2026,U+2039-203A,U+00B7,U+2190-2193,U+2212,U+2264-2265,U+2713,U+2016" \
  --layout-features='kern,liga,tnum,calt' --no-hinting --desubroutinize \
  --output-file=src/wasmJsMain/composeResources/font/ibm_plex_sans_regular.ttf
```
