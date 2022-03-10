import { Breadcrumb, BreadcrumbItem, BreadcrumbLink, Container, Divider, Heading, Link, Text } from "@chakra-ui/react"
import Layout from "../../components/layout/Layout"
import { getPlugins, readPluginById } from "../../helpers/mdx"
import MDX from "../../components/MDX"
import { MDXRemote } from "next-mdx-remote"
import { ChevronRightIcon } from "@chakra-ui/icons"

const GitHubLink = ({ url, children }) => {
  return (
    <>
      <Link color={'purple.300'} href={`https://github.com/${url}`}>{children}</Link>
    </>
  )
}
export default function Plugin({ plugin }) {
  return (
    <Layout>
      <Container marginTop={6}>
        <Breadcrumb spacing='8px' separator={<ChevronRightIcon color='gray.500' />}>
          <BreadcrumbItem>
            <BreadcrumbLink href='/plugin'>Plugins</BreadcrumbLink>
          </BreadcrumbItem>
          <BreadcrumbItem isCurrentPage>
              <BreadcrumbLink href={`/plugin/${plugin.id}`}>{plugin.title}</BreadcrumbLink>
          </BreadcrumbItem>
        </Breadcrumb>
        <Heading marginY={2}>{plugin.title}</Heading>
        <Text>
          Repository: <GitHubLink url={plugin.repository}>{plugin.repository}</GitHubLink>
        </Text>
        <Text>
          Authors: {plugin.authors.map(author => (
            <Text as='abbr'>
              <GitHubLink url={author}>{author}</GitHubLink>
              {[...plugin.authors].pop() !== author ? <>,&nbsp;</> : <></>}
            </Text>
          ))}
        </Text>
        <Divider paddingY={2} marginBottom={1} />
        <MDXRemote components={MDX} {...plugin.content} />
      </Container>
    </Layout>
  )
}

export async function getStaticProps({ params: { id } }) {
  const plugin = await readPluginById(id)

  return {
    props: {
      plugin
    }
  }
}

export async function getStaticPaths() {
  const plugins = await getPlugins()

  return ({
    paths: plugins.map(plugin => ({
      params: {
        id: plugin.id
      }
    })),
    fallback: false
  })
}