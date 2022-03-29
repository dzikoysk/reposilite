import { ChakraProvider } from '@chakra-ui/react'
import Head from 'next/head'
import { ColorModeScript } from 'nextjs-color-mode'
import '../components/MDX.css'

const criticalThemeCss = `
html, body {
  min-width: 460px;
  width: 100%;
}`

function MyApp({ Component, pageProps }) {
  return (
    <>
      <Head>
        <style dangerouslySetInnerHTML={{ __html: criticalThemeCss }} />
      </Head>
      <ChakraProvider>
        <ColorModeScript />
        <Component {...pageProps} />
      </ChakraProvider>
    </>
  )
}

export default MyApp