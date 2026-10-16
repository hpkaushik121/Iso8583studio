/**
 * Renders public/images/logo-mark.svg (the brand mark) to every raster icon
 * the site serves, so they can never drift from the SVG:
 *
 *   public/favicon.ico                  16/32/48 ICO, for browsers and tools
 *                                       that request /favicon.ico directly
 *   public/images/favicon-192.png       the PNG Google Search reads (Google
 *                                       does not rasterise SVG favicons)
 *   public/images/apple-touch-icon.png  180px iOS home-screen icon
 *   public/images/app.png               512px mark: default og:image and the
 *                                       Organization logo in JSON-LD
 *
 * One-off: run `node tools/build-favicons.mjs` after changing the SVG. It is
 * not part of build:content because the outputs are committed.
 */
import sharp from 'sharp';
import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const svg = readFileSync(join(ROOT, 'public/images/logo-mark.svg'));
const SVG_SIZE = 48;

const render = (size) =>
  sharp(svg, { density: (72 * size) / SVG_SIZE }).resize(size, size).png();

for (const [file, size] of [
  ['public/images/app.png', 512],
  ['public/images/favicon-192.png', 192],
  ['public/images/apple-touch-icon.png', 180],
]) {
  await render(size).toFile(join(ROOT, file));
  console.log(`favicons: ${file} ${size}px`);
}

// ICO container: a 6-byte header, one 16-byte directory entry per image, then
// the PNG payloads (Windows Vista+ and every browser accept PNG-in-ICO).
const sizes = [16, 32, 48];
const pngs = await Promise.all(sizes.map((s) => render(s).toBuffer()));
const header = Buffer.alloc(6);
header.writeUInt16LE(0, 0); // reserved
header.writeUInt16LE(1, 2); // type: icon
header.writeUInt16LE(sizes.length, 4);
const dir = Buffer.alloc(16 * sizes.length);
let offset = header.length + dir.length;
sizes.forEach((s, i) => {
  const e = i * 16;
  dir.writeUInt8(s, e); // width (0 would mean 256)
  dir.writeUInt8(s, e + 1); // height
  dir.writeUInt8(0, e + 2); // palette
  dir.writeUInt8(0, e + 3); // reserved
  dir.writeUInt16LE(1, e + 4); // colour planes
  dir.writeUInt16LE(32, e + 6); // bits per pixel
  dir.writeUInt32LE(pngs[i].length, e + 8);
  dir.writeUInt32LE(offset, e + 12);
  offset += pngs[i].length;
});
writeFileSync(join(ROOT, 'public/favicon.ico'), Buffer.concat([header, dir, ...pngs]));
console.log(`favicons: public/favicon.ico ${sizes.join('/')}px`);
