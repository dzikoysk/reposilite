import Head from "next/head"
import Landing from "../components/landing/Landing"
import Layout from "../components/layout/Layout"

export default function Home() {
  const defaultTitle = 'Reposilite · Lightweight repository manager for Maven artifacts'

  return (
    <Layout>
      <Head>
        <title>{defaultTitle}</title>  
      </Head>
      <Landing />
    </Layout>
  )
}
