import { ChakraProvider } from '@chakra-ui/react'
import Head from 'next/head'
import { useEffect, useState } from 'react';

function App({ Component, pageProps }) {
  const [mounted, setMounted] = useState(false)

  useEffect(() => {
    setMounted(true)
  }, [])

  return (
    <ChakraProvider>
      <Head>
        <title>Reposilite - Lightweight repository manager for Maven artifacts</title>
      </Head>
      {mounted && <Component {...pageProps} />}
    </ChakraProvider>
  )
}

export default App