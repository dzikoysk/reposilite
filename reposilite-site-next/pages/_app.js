import { ChakraProvider } from '@chakra-ui/react'
import Head from 'next/head'
import { ColorModeScript } from 'nextjs-color-mode'

const criticalThemeCss = `
html {
  min-width: 460px;
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