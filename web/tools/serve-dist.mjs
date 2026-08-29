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
  '.txt': 'text/plain; charset=utf-8', '.webp': 'image/webp',
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
  res.writeHead(200, { 'content-type': TYPES[extname(candidate)] ?? 'application/octet-stream' });
  res.end(readFileSync(candidate));
}).listen(PORT, () => console.log(`serving ${DIST} on http://localhost:${PORT}`));
