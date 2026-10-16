/** Frontmatter for one post, without its body. */
export interface BlogMeta {
  slug: string;
  path: string;
  title: string;
  description: string;
  /** ISO date, `2025-08-30`. */
  date: string;
  /** The frontmatter category; equals the `name` of one BlogTopic. */
  category: string;
  /** Slug of the category — the topic's id and its `/blogs#<id>` fragment. */
  topicId: string;
  author: string;
  /** ISO date of the last substantive revision, from the `updated:` front matter; '' when never revised. */
  updated: string;
  /** As written in the frontmatter, `9 min read`. */
  readTime: string;
  /** The number parsed out of `readTime`. */
  minutes: number;
  tags: string[];
  /** `/images/blog/<slug>.jpg` — the post cover and the social card — or null
   *  when no image has been generated. */
  image: string | null;
  /** `/media/blog/<slug>.webp`, the 640px card thumbnail, or null. */
  thumb: string | null;
}

/** One of the topics the blog is browsed by (content/blog/topics.json). */
export interface BlogTopic {
  id: string;
  name: string;
  /** Phosphor icon name. */
  icon: string;
  summary: string;
  /** Number of posts in the topic. */
  count: number;
}

/** One h2 of a post body. */
export interface TocEntry {
  id: string;
  text: string;
}

/** What a post's content module holds, and what the route resolves to. */
export interface PostContent {
  html: string;
  toc: TocEntry[];
}

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

/** `2025-08-30` → `Aug 30, 2025`. Done by hand rather than through Date so the
 *  prerender and the browser cannot disagree across time zones. */
export function longDate(iso: string): string {
  const [y, m, d] = iso.split('-').map(Number);
  return m && d ? `${MONTHS[m - 1]} ${d}, ${y}` : iso;
}

/** `2025-08-30` → `Aug 30`. */
export function shortDate(iso: string): string {
  const [, m, d] = iso.split('-').map(Number);
  return m && d ? `${MONTHS[m - 1]} ${d}` : iso;
}
