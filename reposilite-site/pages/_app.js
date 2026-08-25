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

import { ChakraProvider } from '@chakra-ui/react'
import { generateDefaultSeo } from 'next-seo/pages'
import Head from 'next/head'
import { ColorModeScript } from 'nextjs-color-mode'
import '../components/MDX.css'
import defaultSeoConfig from "../next-seo.config"

const criticalThemeCss = `
html, body {
  min-width: 460px;
  width: 100%;
}`

function App({ Component, pageProps }) {
  return (
    <>
      <Head>
        <style dangerouslySetInnerHTML={{ __html: criticalThemeCss }} />
        {generateDefaultSeo(defaultSeoConfig)}
      </Head>
      <ChakraProvider>
        <ColorModeScript />
        <Component {...pageProps} />
      </ChakraProvider>
    </>
  )
}

export default App