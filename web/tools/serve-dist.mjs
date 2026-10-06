/**
 * Serves the build the way GitHub Pages does: extensionless URLs resolve to
 * <path>.html or <path>/index.html, and unknown paths get 404.html with a real
 * 404 status. Local preview only.
 */
import { createServer } from 'node:http';
import { readFileSync, existsSync, statSync } from 'node:fs';
import { join, extname, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const DIST = join(dirname(fileURLToPath(import.meta.url)), '../dist/iso8583-studio/browser');
const PORT = Number(process.env.PORT ?? 4321);

const TYPES = {
  '.html': 'text/html; charset=utf-8', '.js': 'text/javascript', '.css': 'text/css',
  '.json': 'application/json', '.xml': 'application/xml', '.png': 'image/png',
  '.jpg': 'image/jpeg', '.svg': 'image/svg+xml', '.ico': 'image/x-icon',
  '.txt': 'text/plain; charset=utf-8', '.webp': 'image/webp', '.avif': 'image/avif',
  '.mp4': 'video/mp4', '.webm': 'video/webm', '.woff2': 'font/woff2',
};

const isFile = (p) => existsSync(p) && statSync(p).isFile();

createServer((req, res) => {
  const url = decodeURIComponent(req.url.split('?')[0]);
  const base = join(DIST, url);
  const candidate =
    isFile(base) ? base
    : isFile(`${base}.html`) ? `${base}.html`
    : isFile(join(base, 'index.html')) ? join(base, 'index.html')
    : null;

  if (!candidate) {
    const notFound = join(DIST, '404.html');
    res.writeHead(404, { 'content-type': 'text/html; charset=utf-8' });
    res.end(isFile(notFound) ? readFileSync(notFound) : 'Not found');
    return;
  }
  const type = TYPES[extname(candidate)] ?? 'application/octet-stream';
  const body = readFileSync(candidate);

  // Safari will not play a video from a server that ignores Range, and
  // GitHub Pages honours it, so the preview has to as well.
  const range = /^bytes=(\d*)-(\d*)$/.exec(req.headers.range ?? '');
  if (range) {
    const start = range[1] ? Number(range[1]) : 0;
    const end = range[2] ? Math.min(Number(range[2]), body.length - 1) : body.length - 1;
    res.writeHead(206, {
      'content-type': type,
      'accept-ranges': 'bytes',
      'content-range': `bytes ${start}-${end}/${body.length}`,
      'content-length': end - start + 1,
    });
    res.end(body.subarray(start, end + 1));
    return;
  }
  res.writeHead(200, { 'content-type': type, 'accept-ranges': 'bytes', 'content-length': body.length });
  res.end(body);
}).listen(PORT, () => console.log(`serving ${DIST} on http://localhost:${PORT}`));
