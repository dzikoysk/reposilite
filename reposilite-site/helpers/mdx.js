/*
 * Copyright (c) 2020-2026 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { readdir, readFile } from 'fs'
import matter from 'gray-matter'
import { serialize } from 'next-mdx-remote/serialize'
import path from 'path'
import { promisify } from 'util'
import remarkGfm from 'remark-gfm'
import rehypePrism from 'rehype-prism-plus'
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
  return readMdx(path.join(GUIDE_PATH, category, id.endsWith('.md') ? id : `${id}.md`))
}

export async function readPluginById(id) {
  return readMdx(path.join(PLUGINS_PATH, id.endsWith('.md') ? id : `${id}.md`))
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
