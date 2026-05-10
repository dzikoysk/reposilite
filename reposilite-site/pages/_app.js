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