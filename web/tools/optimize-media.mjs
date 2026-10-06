/**
 * Converts the design's source media into what the site actually serves, under
 * public/media/. Run by hand when the source art changes; the outputs are
 * committed, so neither `npm run build` nor CI needs the sources, sharp or
 * ffmpeg.
 *
 *   npm run media -- "/path/to/ISO8583Studio Design System/assets"
 *
 * What it does, and why:
 *   - Solution-page art: the four source folders repeat the same drawings, so
 *     files are de-duplicated by content and written once, as WebP at two
 *     widths for srcset.
 *   - Hero videos: re-encoded to H.264 at 720p and 480p without an audio
 *     track, with the index at the front so playback can start before the
 *     download ends.
 *   - Posters, the hero dot map, blog cover thumbnails and the documentation
 *     screenshots: WebP at the sizes they are displayed at.
 *
 * Existing outputs are skipped, so re-running after adding one file is cheap.
 * Pass --force to rebuild everything.
 */
import { execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import {
  existsSync, mkdirSync, readdirSync, readFileSync, statSync, writeFileSync,
} from 'node:fs';
import { basename, dirname, extname, join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';
import sharp from 'sharp';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const OUT = join(ROOT, 'public/media');
const args = process.argv.slice(2);
const FORCE = args.includes('--force');
const SRC = args.find((a) => !a.startsWith('--'));

if (!SRC || !existsSync(SRC)) {
  console.error('usage: npm run media -- "<design assets directory>" [--force]');
  process.exit(1);
}

const fresh = (file) => !FORCE && existsSync(file);
const ensureDir = (file) => mkdirSync(dirname(file), { recursive: true });
const kb = (file) => `${Math.round(statSync(file).size / 1024)} kB`;

async function webp(src, out, width, quality = 80) {
  if (fresh(out)) return;
  ensureDir(out);
  await sharp(src)
    .resize({ width, withoutEnlargement: true })
    .webp({ quality, alphaQuality: 90, effort: 6 })
    .toFile(out);
  console.log(`  ${relative(ROOT, out)}  ${kb(out)}`);
}

function video(src, out, height, crf) {
  if (fresh(out)) return;
  ensureDir(out);
  execFileSync('ffmpeg', [
    '-y', '-loglevel', 'error', '-i', src,
    '-vf', `scale=-2:${height}`,
    '-c:v', 'libx264', '-preset', 'slow', '-crf', String(crf),
    '-pix_fmt', 'yuv420p', '-an', '-movflags', '+faststart',
    out,
  ]);
  console.log(`  ${relative(ROOT, out)}  ${kb(out)}`);
}

// ---- Solution art ---------------------------------------------------------

/** The folders repeat drawings under different names; the shortest plain name
 *  in each group of identical files becomes the published one. */
async function art() {
  console.log('art');
  const groups = new Map();
  for (const dir of ['cloud', 'kernel', 'middleware', 'motion']) {
    const folder = join(SRC, dir);
    if (!existsSync(folder)) continue;
    for (const file of readdirSync(folder).filter((f) => f.endsWith('.png')).sort()) {
      const full = join(folder, file);
      const hash = createHash('md5').update(readFileSync(full)).digest('hex');
      const name = basename(file, '.png').replace(/^\d+-/, '');
      if (!groups.has(hash)) groups.set(hash, { full, names: [], sources: [] });
      groups.get(hash).names.push(name);
      groups.get(hash).sources.push(`${dir}/${file}`);
    }
  }

  const map = {};
  const taken = new Set();
  for (const group of groups.values()) {
    let name = [...group.names].sort((a, b) => a.length - b.length || a.localeCompare(b))[0];
    while (taken.has(name)) name += '-2';
    taken.add(name);
    await webp(group.full, join(OUT, 'art', `${name}-640.webp`), 640, 78);
    await webp(group.full, join(OUT, 'art', `${name}-1024.webp`), 1024, 78);
    for (const source of group.sources) map[source] = `/media/art/${name}`;
  }
  return map;
}

// ---- Hero dot map, posters ------------------------------------------------

async function stills() {
  console.log('hero and posters');
  for (const name of ['world-dots', 'world-dots-hot']) {
    const src = join(SRC, `${name}.png`);
    if (!existsSync(src)) continue;
    await webp(src, join(OUT, 'hero', `${name}-1440.webp`), 1440, 82);
    await webp(src, join(OUT, 'hero', `${name}-2880.webp`), 2880, 82);
  }
  for (const folder of [SRC, join(SRC, 'simulators')]) {
    if (!existsSync(folder)) continue;
    for (const file of readdirSync(folder).filter((f) => f.endsWith('-poster.jpg'))) {
      const name = basename(file, '-poster.jpg').replace(/-hero$/, '');
      await webp(join(folder, file), join(OUT, 'poster', `${name}-640.webp`), 640, 72);
      await webp(join(folder, file), join(OUT, 'poster', `${name}-1280.webp`), 1280, 72);
    }
  }
}

// ---- Video ----------------------------------------------------------------

function videos() {
  console.log('video');
  for (const folder of [SRC, join(SRC, 'simulators')]) {
    if (!existsSync(folder)) continue;
    for (const file of readdirSync(folder).filter((f) => f.endsWith('.mp4'))) {
      // hsm-hero-1.mp4 -> hsm-1, host-hero.mp4 -> host
      const name = basename(file, '.mp4').replace(/-hero/, '');
      video(join(folder, file), join(OUT, 'video', `${name}-720.mp4`), 720, 29);
      video(join(folder, file), join(OUT, 'video', `${name}-480.mp4`), 480, 30);
    }
  }
}

// ---- Images already in the repo -------------------------------------------

async function blogThumbs() {
  console.log('blog thumbnails');
  const folder = join(ROOT, 'public/images/blog');
  for (const file of readdirSync(folder).filter((f) => f.endsWith('.jpg'))) {
    await webp(join(folder, file), join(OUT, 'blog', `${basename(file, '.jpg')}.webp`), 640, 74);
    // The post page shows the cover full width; the JPG stays for og:image.
    await webp(join(folder, file), join(OUT, 'blog', `${basename(file, '.jpg')}-1376.webp`), 1376, 78);
  }
}

const DOC_SHOT_FOLDERS = ['payment-utilities'];

async function docShots() {
  console.log('documentation screenshots');
  const root = join(ROOT, 'public/images/docs');
  // Only the folders a page still shows: the other guides draw their screens
  // in DOM now and no longer use screenshots.
  for (const dir of DOC_SHOT_FOLDERS) {
    const folder = join(root, dir);
    if (!existsSync(folder)) continue;
    for (const file of readdirSync(folder).filter((f) => extname(f) === '.png')) {
      await webp(join(folder, file), join(OUT, 'docs', dir, `${basename(file, '.png')}.webp`), 1600, 80);
    }
  }
}

const map = await art();
await stills();
videos();
await blogThumbs();
await docShots();

writeFileSync(join(ROOT, 'tools/media-sources.json'), JSON.stringify(map, null, 2) + '\n');
console.log('done');
