import { Box, Container, Link } from "@chakra-ui/react"
import { MDXRemote } from "next-mdx-remote"
import { getAllGuides } from "../../helpers/mdx"
import Layout from '../../components/layout/Layout'

export default function Guide({ guides }) {
  return (
    <Layout>
      <Container>
      {guides.map((guide, index) => (
        <Box key={guide.id}>
          <Link >{guide.title}</Link>
          <MDXRemote {...guide.content} />
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