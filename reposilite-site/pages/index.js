import Head from "next/head"
import Landing from "../components/landing/Landing"
import Layout from "../components/layout/Layout"

export default function Home() {
  return (
    <Layout>
      <Head>
        <title>Reposilite · Lightweight repository manager for Maven artifacts</title>  
      </Head>
      <Landing />
    </Layout>
  )
}
