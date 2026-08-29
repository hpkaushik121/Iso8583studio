/**
 * Generates the blog routes, per-post content modules and post index from
 * content/blog/*.md.
 *
 * Replaces docs/blogs/build_blogs.py. Differences that matter:
 *  - a real Markdown parser (nested lists, inline HTML, correct escaping);
 *  - internal links keep the SPA router instead of being forced to
 *    target="_blank", which the old process_inline() did to every link;
 *  - URLs are extensionless, and the sitemap is no longer written from a
 *    hardcoded list of 11 static pages that overwrote every other entry.
 */
import { readdirSync, readFileSync, writeFileSync, mkdirSync, rmSync, existsSync } from 'node:fs';
import { join, dirname, basename } from 'node:path';
import { fileURLToPath } from 'node:url';
import matter from 'gray-matter';
import MarkdownIt from 'markdown-it';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const CONTENT = join(ROOT, 'content/blog');
const OUT_POSTS = join(ROOT, 'src/app/content/posts');
const OUT_DIR = join(ROOT, 'src/app/content');
const OUT_IMAGES = join(ROOT, 'public/images/blog');
const SITE = 'https://iso8583.studio';

/* ---- Post images ---------------------------------------------------------
 *
 * Generated once with Gemini and committed, never regenerated on a normal
 * build: the API costs money, returns a different picture every call, and CI
 * has no key. So `npm run build` only ever reads what is already on disk, and
 * generation is an explicit opt-in:
 *
 *   npm run blog:images        # fill in whatever is missing
 *   npm run blog:images -- --force <slug>   # redo one you dislike
 *
 * A post whose file is absent simply has no image; nothing breaks.
 */
const WANT_IMAGES = process.argv.includes('--images');
const FORCE = process.argv.includes('--force');
const ONLY = process.argv.filter((a) => !a.startsWith('-') && !a.endsWith('.mjs') && !a.includes('node'));

// The key lives in the repo-root .env, which is gitignored. Absent is normal.
if (WANT_IMAGES && typeof process.loadEnvFile === 'function') {
  try { process.loadEnvFile(join(ROOT, '..', '.env')); } catch { /* no .env */ }
}

const IMAGE_ENDPOINT = 'https://generativelanguage.googleapis.com/v1beta/interactions';
// 1K is the smallest tier that clears the 1200x630 minimum for a social card.
const IMAGE_SIZE = process.env.GEMINI_IMAGE_SIZE || '1K';
const IMAGE_MODEL = process.env.GEMINI_IMAGE_MODEL || process.env.GEMINI_MODEL;

/* One house style, so fifty-two posts do not look like fifty-two blogs.
 *
 * These are infographics, not decoration: the picture is supposed to teach the
 * post's subject at a glance, so it is built from the post's own structure —
 * its description and its section headings — rather than from the title alone.
 * A title on its own gave pretty artwork about nothing in particular.
 *
 * Labels are wanted here and the model spells them correctly, but they have to
 * be kept short. Asked for explanatory text it produces paragraph-shaped
 * blocks of plausible nonsense, which is the one kind of text that actually
 * looks broken. */
const IMAGE_STYLE =
  'A clean technical infographic illustrating a payments-engineering concept. ' +
  'Very dark navy background (#15161f), flat vector diagram in blue (#1e88e5) and ' +
  'teal (#26a69a), labels in white and light grey, clear visual hierarchy, ' +
  'generous spacing, balanced 16:9 editorial layout.';

const promptFor = (post) => {
  const points = post.headings.slice(0, 5);
  return `${IMAGE_STYLE} ` +
    `Explain this topic visually so a reader understands it at a glance: ${post.title}. ` +
    `${post.description} ` +
    (points.length ? `Build the graphic around these ideas: ${points.join('; ')}. ` : '') +
    'Use labelled boxes, arrows, icons and simple structured layouts to show how the ' +
    'parts relate. Every label must be one to three real, correctly spelled words: ' +
    'check the spelling of each one, and use fewer labels rather than risk a ' +
    'misspelling. Any hex or binary shown must be well formed. ' +
    'No sentences or paragraphs of text, no fake body copy, no legend blocks. ' +
    'No logos, no watermarks, no people, no hands, no application screenshots.';
};

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function generateImage(post, attempt = 1) {
  const res = await fetch(IMAGE_ENDPOINT, {
    method: 'POST',
    headers: { 'x-goog-api-key': process.env.GEMINI_API_KEY, 'Content-Type': 'application/json' },
    body: JSON.stringify({
      model: IMAGE_MODEL,
      input: [{ type: 'text', text: promptFor(post) }],
      response_format: {
        type: 'image', mime_type: 'image/jpeg', aspect_ratio: '16:9', image_size: IMAGE_SIZE,
      },
    }),
  });

  if (!res.ok) {
    const detail = await res.text();
    // Rate limit and transient server errors are worth retrying; a 400 is a
    // bad request and will fail identically every time.
    if ((res.status === 429 || res.status >= 500) && attempt < 4) {
      await sleep(2000 * attempt);
      return generateImage(post, attempt + 1);
    }
    throw new Error(`${res.status} ${detail.slice(0, 200)}`);
  }

  const json = await res.json();
  // The picture arrives as a model_output step, alongside a thought step.
  const image = (json.steps ?? []).flatMap((s) => s.content ?? [])
    .find((c) => c.type === 'image' && c.data);
  if (!image) throw new Error('response carried no image');

  writeFileSync(join(OUT_IMAGES, `${post.slug}.jpg`), Buffer.from(image.data, 'base64'));
}

/** Fills in missing images, a few at a time so the API is not hit flat out. */
async function generateMissing(posts) {
  if (!process.env.GEMINI_API_KEY) {
    console.warn('  no GEMINI_API_KEY (set it in the repo-root .env) — skipping images');
    return;
  }
  mkdirSync(OUT_IMAGES, { recursive: true });

  const todo = posts.filter((p) => {
    if (ONLY.length && !ONLY.includes(p.slug)) return false;
    return FORCE || !existsSync(join(OUT_IMAGES, `${p.slug}.jpg`));
  });
  if (!todo.length) { console.log('  images: nothing to generate'); return; }

  console.log(`  images: generating ${todo.length} at ${IMAGE_SIZE} with ${IMAGE_MODEL}`);
  let done = 0, failed = 0;
  const queue = [...todo];
  const worker = async () => {
    for (let post; (post = queue.shift());) {
      try {
        await generateImage(post);
        console.log(`    ✔ ${++done}/${todo.length} ${post.slug}`);
      } catch (err) {
        failed++;
        console.warn(`    ✘ ${post.slug}: ${err.message}`);
      }
    }
  };
  await Promise.all(Array.from({ length: 3 }, worker));
  if (failed) console.warn(`  images: ${failed} failed — re-run to retry just those`);
}

const md = new MarkdownIt({ html: true, linkify: true, typographer: false });

// Wrap tables so wide ones scroll inside their own box rather than forcing the
// page to scroll sideways.
md.renderer.rules.table_open = () => '<div class="table-wrapper"><table>';
md.renderer.rules.table_close = () => '</table></div>';

// Headings get slug ids so in-page anchors and the scroll-margin rule work.
const slugify = (s) => s.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
md.renderer.rules.heading_open = (tokens, i, opts, env, self) => {
  const token = tokens[i];
  if (token.tag === 'h2' || token.tag === 'h3') {
    const text = tokens[i + 1]?.content ?? '';
    if (text) token.attrSet('id', slugify(text));
  }
  return self.renderToken(tokens, i, opts);
};

// Only genuinely external links open in a new tab. Internal ones stay relative
// so the router handles them.
const defaultLink = md.renderer.rules.link_open
  ?? ((tokens, i, opts, env, self) => self.renderToken(tokens, i, opts));
md.renderer.rules.link_open = (tokens, i, opts, env, self) => {
  const href = tokens[i].attrGet('href') || '';
  const isExternal = /^https?:\/\//i.test(href) && !href.startsWith(SITE);
  if (isExternal) {
    tokens[i].attrSet('target', '_blank');
    tokens[i].attrSet('rel', 'noopener');
  }
  return defaultLink(tokens, i, opts, env, self);
};

const files = readdirSync(CONTENT).filter((f) => f.endsWith('.md')).sort();
if (!files.length) throw new Error(`no markdown found in ${CONTENT}`);

const posts = files.map((file) => {
  const slug = basename(file, '.md');
  const raw = readFileSync(join(CONTENT, file), 'utf8');
  const { data, content } = matter(raw);
  const tags = Array.isArray(data.tags)
    ? data.tags.map(String)
    : String(data.tags ?? '').split(',').map((t) => t.trim()).filter(Boolean);

  return {
    slug,
    path: `/blogs/${slug}`,
    title: String(data.title ?? slug),
    description: String(data.description ?? ''),
    date: String(data.date ?? ''),
    category: String(data.category ?? 'General'),
    author: String(data.author ?? 'AiCortex Team'),
    readTime: String(data.read_time ?? '5 min read'),
    tags,
    // Section headings drive the infographic prompt; they are the closest
    // thing the post has to an outline of what the picture should show.
    headings: [...content.matchAll(/^#{2,3}\s+(.+?)\s*$/gm)]
      .map((m) => m[1].replace(/[`*_]/g, '').trim()),
    html: md.render(content),
  };
});

// Newest first, matching the previous index ordering.
posts.sort((a, b) => (a.date < b.date ? 1 : a.date > b.date ? -1 : 0));

if (WANT_IMAGES) await generateMissing(posts);

// Read from disk rather than from whether generation ran, so a build with no
// key still wires up every image that is already committed.
for (const post of posts) {
  post.image = existsSync(join(OUT_IMAGES, `${post.slug}.jpg`))
    ? `/images/blog/${post.slug}.jpg`
    : null;
}

rmSync(OUT_POSTS, { recursive: true, force: true });
mkdirSync(OUT_POSTS, { recursive: true });

const banner = '// GENERATED by tools/build-blog-routes.mjs — do not edit.\n';

for (const post of posts) {
  writeFileSync(
    join(OUT_POSTS, `${post.slug}.ts`),
    `${banner}export const html = ${JSON.stringify(post.html)};\n`,
  );
}

const meta = posts.map(({ html, headings, ...rest }) => rest);

writeFileSync(
  join(OUT_DIR, 'blog-index.ts'),
  `${banner}import type { BlogMeta } from '../pages/blog/blog-meta';\n\n` +
  `export const BLOG_POSTS: BlogMeta[] = ${JSON.stringify(meta, null, 2)};\n\n` +
  `export const BLOG_CATEGORIES: string[] = ${JSON.stringify(
    [...new Set(meta.map((p) => p.category))].sort(), null, 2)};\n`,
);

// One route per post, each dynamically importing only its own content module.
const routeEntries = posts.map((p) => {
  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'BlogPosting',
    headline: p.title,
    description: p.description,
    author: { '@type': 'Organization', name: 'AiCortex Solutions' },
    publisher: { '@type': 'Organization', name: 'AiCortex Solutions', url: `${SITE}/` },
    datePublished: p.date,
    ...(p.image ? { image: `${SITE}${p.image}` } : {}),
    url: `${SITE}${p.path}`,
    mainEntityOfPage: `${SITE}${p.path}`,
    keywords: p.tags.join(', ') || 'ISO8583, payment testing',
  };
  const seo = {
    title: `${p.title} - ISO8583Studio Blog`,
    description: p.description,
    keywords: p.tags.join(', ') || 'ISO8583, payment testing',
    path: p.path,
    ogType: 'article',
    author: p.author,
    ...(p.image ? { image: `${SITE}${p.image}` } : {}),
    jsonLd,
  };
  return `  {
    path: 'blogs/${p.slug}',
    component: BlogPost,
    resolve: { html: postResolver },
    data: {
      seo: ${JSON.stringify(seo)},
      slug: ${JSON.stringify(p.slug)},
      load: () => import('../content/posts/${p.slug}'),
    },
  },`;
});

writeFileSync(
  join(OUT_DIR, 'blog-routes.ts'),
  `${banner}import { Routes } from '@angular/router';\n` +
  `import { BlogPost } from '../pages/blog/blog-post';\n` +
  `import { postResolver } from '../pages/blog/post-resolver';\n\n` +
  `export const blogRoutes: Routes = [\n${routeEntries.join('\n')}\n];\n`,
);

console.log(`blog: ${posts.length} posts, ${new Set(posts.map((p) => p.category)).size} categories, `
  + `${posts.filter((p) => p.image).length} with an image`);
