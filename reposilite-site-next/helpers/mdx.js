import { readdir, readFile } from 'fs'
import matter from 'gray-matter'
import { serialize } from 'next-mdx-remote/serialize'
import path from 'path'
import { promisify } from 'util'
import remarkGfm from 'remark-gfm'
import rehypePrism from '@mapbox/rehype-prism'
import { categories } from '../data/guides/guides'

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

export async function getGuideCategories() {
  return Promise.all(categories.map(async category => await readCategory(category)))
}

async function readCategory(category) {
  return ({
    name: category.name,
    directory: category.directory,
    content: await Promise.all(category.content
      .map(async guideId => {
        const { title } = await readGuideById(category.directory, guideId)

        return {
          id: guideId,
          title
        }
      }))
  })
}

export async function getPlugins() {
  const plugins = await readDirectory(PLUGINS_PATH)
  return Promise.all(plugins.map(async file => readMdx(path.join(PLUGINS_PATH, file))))
}

export async function readGuideById(category, id) {
  return readMdx(path.join(GUIDE_PATH, category, id.endsWith('md') ? id : `${id}.md`))
}

export async function readPluginById(id) {
  return readMdx(path.join(PLUGINS_PATH, id.endsWith('md') ? id : `${id}.md`))
}

export async function readMdx(file) {
  const content = await readSpecificFile(file)
  const { content: raw, data: metadata } = matter(content)
  const serializedContent = await serializeMdx(raw)

  return {
    content: serializedContent,
    ...metadata
  }
}