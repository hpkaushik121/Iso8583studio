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
}
