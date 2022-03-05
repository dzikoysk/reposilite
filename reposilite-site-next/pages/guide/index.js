import { Box, Container, Link } from "@chakra-ui/react"
import { MDXRemote } from "next-mdx-remote"
import { getAllGuides } from "../../helpers/mdx"
import Layout from '../../components/layout/Layout'
import MDX from "../../components/MDX"

export default function Guide({ guides }) {
  return (
    <Layout>
      <Container maxW='container.lg' marginInlineStart={{ base: '0', sm: 'auto' }}>
        {guides.map((guide, index) => (
          <Box key={guide.id} paddingY='20'>
            <Link >{guide.title}</Link>
            <MDXRemote components={MDX} {...guide.content} />
          </Box>
        ))}
        </Container>
    </Layout>
  )
}

export async function getStaticProps() {
  const guides = await getAllGuides()
  console.log(guides)
  
  return {
    props: {
      guides: guides
        .sort((a, b) => a.metadata.id - b.metadata.id)
        .map((guideEntry, index) => {
          const { metadata, guide } = guideEntry
          const { id, title } = metadata
          
          return {
            id,
            title,
            metadata,
            content: guide
          }
        })
    }
  }
}