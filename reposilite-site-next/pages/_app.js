import Head from 'next/head'
import { Chakra } from '../components/Chakra'

export default function App({ Component, pageProps }) {
  return (
    <Chakra cookies={pageProps.cookies}>
      <Head>
        <title>Reposilite - Lightweight repository manager for Maven artifacts</title>
      </Head>
      <Component {...pageProps} />
    </Chakra>
  )
}

export { getServerSideProps } from "../components/Chakra"