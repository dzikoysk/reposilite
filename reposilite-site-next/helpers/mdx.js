import { readdir, readFile } from 'fs'
import matter from 'gray-matter'
import { serialize } from 'next-mdx-remote/serialize'
import path from 'path'
import { promisify } from 'util'
import remarkGfm from 'remark-gfm'
import rehypePrism from '@mapbox/rehype-prism'

const GUIDE_PATH = path.join(process.cwd(), "data", "guides")
const PLUGINS_PATH = path.join(process.cwd(), "data", "plugins")

const readDirectory = promisify(readdir)
const readSpecificFile = promisify(readFile)

function serializeMdx(mdx) {
  return serialize(mdx, {
    mdxOptions: {
      remarkPlugins: [
        remarkGfm,
      ],
      rehypePlugins: [
        rehypePrism
      ]
    }
  })
}

export async function getAllGuides() {
  const guides = await readDirectory(GUIDE_PATH)

  return await Promise.all(
    guides
      .filter(path => /\.mdx?$/.test(path))
      .map(async (guide) => {
        const content = await readSpecificFile(path.join(GUIDE_PATH, guide))
        const { content: raw, data: metadata } = matter(content)
        const serializedContent = await serializeMdx(raw)

        return {
          content: serializedContent,
          ...metadata
        }
      })
      .sort((a, b) => a.id - b.id)
  )
}