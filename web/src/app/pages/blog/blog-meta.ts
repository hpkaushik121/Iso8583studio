/** Frontmatter for one post, without its body. */
export interface BlogMeta {
  slug: string;
  path: string;
  title: string;
  description: string;
  date: string;
  category: string;
  author: string;
  readTime: string;
  tags: string[];
  /** `/images/blog/<slug>.jpg`, or null when no image has been generated. */
  image: string | null;
}
